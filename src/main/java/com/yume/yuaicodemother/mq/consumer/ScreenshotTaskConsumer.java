package com.yume.yuaicodemother.mq.consumer;

import cn.hutool.json.JSONUtil;
import com.yume.yuaicodemother.constant.MqConstant;
import com.yume.yuaicodemother.mapper.AppMapper;
import com.yume.yuaicodemother.model.entity.App;
import com.yume.yuaicodemother.mq.model.ScreenshotTaskMessage;
import com.yume.yuaicodemother.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 截图任务消费者 —— 异步生成封面。
 */
@Component
@Slf4j
@RocketMQMessageListener(
        topic = MqConstant.APP_TASK_TOPIC,
        selectorExpression = MqConstant.SCREENSHOT_TASK_TAG,
        consumerGroup = MqConstant.SCREENSHOT_TASK_CONSUMER_GROUP
)
public class ScreenshotTaskConsumer implements RocketMQListener<String> {

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AppMapper appMapper;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public void onMessage(String message) {
        ScreenshotTaskMessage msg = JSONUtil.toBean(message, ScreenshotTaskMessage.class);
        Long appId = msg.getAppId();
        log.info("收到截图任务消息, taskId={}, appId={}", msg.getTaskId(), appId);
        String lockKey = MqConstant.SCREENSHOT_TASK_LOCK_KEY_PREFIX + appId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(5, 30, java.util.concurrent.TimeUnit.SECONDS)) {
                log.info("截图任务已被其他实例处理，跳过, appId={}", appId);
                return;
            }
            executeScreenshot(msg);
        } catch (Exception e) {
            log.error("截图任务执行异常, taskId={}：{}", msg.getTaskId(), e.getMessage());
            throw new RuntimeException("截图任务失败, taskId=" + msg.getTaskId(), e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void executeScreenshot(ScreenshotTaskMessage msg) {
        String screenshotUrl = screenshotService.generateAndUploadScreenshot(msg.getDeployUrl());
        App updateApp = new App();
        updateApp.setId(msg.getAppId());
        updateApp.setCover(screenshotUrl);
        appMapper.update(updateApp);
        log.info("截图任务完成, taskId={}, screenShotUrl={}", msg.getTaskId(), screenshotUrl);
    }
}
