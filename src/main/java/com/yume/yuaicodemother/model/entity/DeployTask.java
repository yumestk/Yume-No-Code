package com.yume.yuaicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部署任务实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("deploy_task")
public class DeployTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("appId")
    private Long appId;

    @Column("userId")
    private Long userId;

    private String status;

    @Column("codeGenType")
    private String codeGenType;

    @Column("deployKey")
    private String deployKey;

    @Column("deployUrl")
    private String deployUrl;

    @Column("errorMessage")
    private String errorMessage;

    @Column("retryCount")
    private Integer retryCount;

    @Column("messageKey")
    private String messageKey;

    @Column("createTime")
    private LocalDateTime createTime;

    @Column("updateTime")
    private LocalDateTime updateTime;

    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
