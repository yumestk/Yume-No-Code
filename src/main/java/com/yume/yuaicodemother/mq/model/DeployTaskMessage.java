package com.yume.yuaicodemother.mq.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 部署任务消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeployTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;

    private Long appId;

    private Long userId;

    private String codeGenType;

    private String deployKey;
}
