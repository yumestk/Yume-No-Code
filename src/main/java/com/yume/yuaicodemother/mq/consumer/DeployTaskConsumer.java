package com.yume.yuaicodemother.mq.consumer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yume.yuaicodemother.constant.AppConstant;
import com.yume.yuaicodemother.constant.MqConstant;
import com.yume.yuaicodemother.core.builder.VueProjectBuilder;
import com.yume.yuaicodemother.mapper.AppMapper;
import com.yume.yuaicodemother.model.entity.App;
import com.yume.yuaicodemother.model.entity.DeployTask;
import com.yume.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yume.yuaicodemother.model.enums.DeployTaskStatusEnum;
import com.yume.yuaicodemother.mq.model.DeployTaskMessage;
import com.yume.yuaicodemother.mq.model.ScreenshotTaskMessage;
import com.yume.yuaicodemother.mq.producer.AppTaskProducer;
import com.yume.yuaicodemother.service.AppService;
import com.yume.yuaicodemother.service.impl.DeployTaskServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 部署任务消费者 —— 异步执行构建与部署。
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstant.APP_TASK_TOPIC,
        selectorExpression = MqConstant.DEPLOY_TASK_TAG,
        consumerGroup = MqConstant.DEPLOY_TASK_CONSUMER_GROUP
)
public class DeployTaskConsumer implements RocketMQListener<String> {

    @Resource
    private DeployTaskServiceImpl deployTaskService;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private AppMapper appMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private AppTaskProducer appTaskProducer;

    @Override
    public void onMessage(String message) {
        DeployTaskMessage msg = JSONUtil.toBean(message, DeployTaskMessage.class);
        Long taskId = msg.getTaskId();
        log.info("收到部署任务消息, taskId={}, appId={}", taskId, msg.getAppId());
        String lockKey = MqConstant.DEPLOY_TASK_LOCK_KEY_PREFIX + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 60, java.util.concurrent.TimeUnit.SECONDS)) {
                log.info("部署任务已被其他实例处理，跳过, taskId={}", taskId);
                return;
            }
            DeployTask task = deployTaskService.getById(taskId);
            if (task == null) {
                log.error("部署任务不存在, taskId={}", taskId);
                return;
            }
            if (DeployTaskStatusEnum.SUCCESS.getValue().equals(task.getStatus())) {
                log.info("部署任务已完成，跳过, taskId={}", taskId);
                return;
            }
            deployTaskService.incrementRetryCount(taskId);
            deployTaskService.markRunning(taskId);
            executeDeploy(msg);
        } catch (Exception e) {
            log.error("部署任务执行异常, taskId={}：{}", taskId, e.getMessage());
            deployTaskService.markFailed(taskId, e.getMessage());
            throw new RuntimeException("部署任务失败, taskId=" + taskId, e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void executeDeploy(DeployTaskMessage msg) {
        Long appId = msg.getAppId();
        Long taskId = msg.getTaskId();
        String codeGenType = msg.getCodeGenType();
        DeployTask task = deployTaskService.getById(taskId);
        String deployKey = task.getDeployKey();
        // 构建源目录路径
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new RuntimeException("应用代码不存在，请先生成代码");
        }
        // Vue 项目构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            if (!buildSuccess) {
                throw new RuntimeException("Vue 项目构建失败，请检查代码和依赖");
            }
            File distDir = new File(sourceDirPath, "dist");
            if (!distDir.exists() || !distDir.isDirectory()) {
                throw new RuntimeException("Vue 项目构建完成但未生成dist目录");
            }
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署dist目录：{}", distDir.getAbsolutePath());
        }
        // 拷贝到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        // 更新应用
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updated = appMapper.update(updateApp) > 0;
        if (!updated) {
            throw new RuntimeException("更新应用部署信息失败");
        }
        // 标记成功
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        deployTaskService.markSuccess(taskId, appDeployUrl);
        log.info("部署任务完成, taskId={}, deployUrl={}", taskId, appDeployUrl);
        // 发送截图消息
        ScreenshotTaskMessage screenshotMsg = new ScreenshotTaskMessage(taskId, appId, appDeployUrl);
        appTaskProducer.sendScreenshotTaskMessage(screenshotMsg);
    }
}
