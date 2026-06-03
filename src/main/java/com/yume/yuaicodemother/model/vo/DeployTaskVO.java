package com.yume.yuaicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部署任务包装类。
 */
@Data
public class DeployTaskVO implements Serializable {

    private Long id;

    private Long appId;

    private Long userId;

    private String status;

    private String statusText;

    private String codeGenType;

    private String deployKey;

    private String deployUrl;

    private String errorMessage;

    private Integer retryCount;

    private String messageKey;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private static final long serialVersionUID = 1L;
}
