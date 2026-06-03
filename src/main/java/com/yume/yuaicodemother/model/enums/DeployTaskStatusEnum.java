package com.yume.yuaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 部署任务状态枚举。
 */
@Getter
public enum DeployTaskStatusEnum {

    PENDING("待处理", "pending"),
    RUNNING("处理中", "running"),
    SUCCESS("成功", "success"),
    FAILED("失败", "failed");

    private final String text;

    private final String value;

    DeployTaskStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举。
     *
     * @param value 枚举值
     * @return 枚举
     */
    public static DeployTaskStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (DeployTaskStatusEnum anEnum : DeployTaskStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
