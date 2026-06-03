package com.yume.yuaicodemother.mq.producer;

import cn.hutool.json.JSONUtil;
import com.yume.yuaicodemother.constant.MqConstant;
import com.yume.yuaicodemother.mq.model.DeployTaskMessage;
import com.yume.yuaicodemother.mq.model.ScreenshotTaskMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 应用任务消息生产者。
 */
@Component
@Slf4j
public class AppTaskProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public void sendDeployTaskMessage(DeployTaskMessage message) {
        SendResult sendResult = rocketMQTemplate.syncSend(
                MqConstant.DEPLOY_TASK_DESTINATION,
                MessageBuilder.withPayload(JSONUtil.toJsonStr(message)).build()
        );
        log.info("部署任务消息发送成功, taskId={}, msgId={}", message.getTaskId(), sendResult.getMsgId());
    }

    public void sendScreenshotTaskMessage(ScreenshotTaskMessage message) {
        SendResult sendResult = rocketMQTemplate.syncSend(
                MqConstant.SCREENSHOT_TASK_DESTINATION,
                MessageBuilder.withPayload(JSONUtil.toJsonStr(message)).build()
        );
        log.info("截图任务消息发送成功, taskId={}, msgId={}", message.getTaskId(), sendResult.getMsgId());
    }
}
