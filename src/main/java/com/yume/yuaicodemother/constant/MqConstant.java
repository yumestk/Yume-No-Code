package com.yume.yuaicodemother.constant;

/**
 * RocketMQ 常量。
 */
public interface MqConstant {

    String APP_TASK_TOPIC = "app-task-topic";

    String DEPLOY_TASK_TAG = "deploy-request";

    String SCREENSHOT_TASK_TAG = "screenshot-generate";

    String DEPLOY_TASK_DESTINATION = APP_TASK_TOPIC + ":" + DEPLOY_TASK_TAG;

    String SCREENSHOT_TASK_DESTINATION = APP_TASK_TOPIC + ":" + SCREENSHOT_TASK_TAG;

    String DEPLOY_TASK_PRODUCER_GROUP = "yume_ai_code_mother_app_task_producer";

    String DEPLOY_TASK_CONSUMER_GROUP = "yume_ai_code_mother_deploy_task_consumer";

    String SCREENSHOT_TASK_CONSUMER_GROUP = "yume_ai_code_mother_screenshot_task_consumer";

    String DEPLOY_TASK_LOCK_KEY_PREFIX = "rocketmq:deploy_task:";

    String SCREENSHOT_TASK_LOCK_KEY_PREFIX = "rocketmq:screenshot_task:";
}
