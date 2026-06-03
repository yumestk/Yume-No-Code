package com.yume.yuaicodemother.mq.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 截图任务消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenshotTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;

    private Long appId;

    private String deployUrl;
}
