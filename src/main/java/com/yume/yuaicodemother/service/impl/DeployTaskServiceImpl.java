package com.yume.yuaicodemother.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yume.yuaicodemother.exception.BusinessException;
import com.yume.yuaicodemother.exception.ErrorCode;
import com.yume.yuaicodemother.exception.ThrowUtils;
import com.yume.yuaicodemother.mapper.DeployTaskMapper;
import com.yume.yuaicodemother.model.entity.DeployTask;
import com.yume.yuaicodemother.model.enums.DeployTaskStatusEnum;
import com.yume.yuaicodemother.model.vo.DeployTaskVO;
import com.yume.yuaicodemother.service.DeployTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 部署任务服务层实现。
 */
@Service
public class DeployTaskServiceImpl extends ServiceImpl<DeployTaskMapper, DeployTask> implements DeployTaskService {

    @Override
    public DeployTask createDeployTask(Long appId, Long userId, String codeGenType, String deployKey, String messageKey) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        DeployTask task = DeployTask.builder()
                .appId(appId)
                .userId(userId)
                .status(DeployTaskStatusEnum.PENDING.getValue())
                .codeGenType(codeGenType)
                .deployKey(deployKey)
                .retryCount(0)
                .messageKey(messageKey)
                .build();
        boolean saved = this.save(task);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建部署任务失败");
        return task;
    }

    @Override
    public boolean hasRunningDeployTask(Long appId) {
        QueryWrapper wrapper = QueryWrapper.create()
                .eq("appId", appId)
                .in("status", DeployTaskStatusEnum.PENDING.getValue(), DeployTaskStatusEnum.RUNNING.getValue())
                .orderBy("createTime", false);
        return this.count(wrapper) > 0;
    }

    @Override
    public DeployTaskVO getDeployTaskVO(DeployTask deployTask) {
        if (deployTask == null) {
            return null;
        }
        DeployTaskVO vo = new DeployTaskVO();
        vo.setId(deployTask.getId());
        vo.setAppId(deployTask.getAppId());
        vo.setUserId(deployTask.getUserId());
        vo.setStatus(deployTask.getStatus());
        DeployTaskStatusEnum statusEnum = DeployTaskStatusEnum.getEnumByValue(deployTask.getStatus());
        vo.setStatusText(statusEnum != null ? statusEnum.getText() : deployTask.getStatus());
        vo.setCodeGenType(deployTask.getCodeGenType());
        vo.setDeployKey(deployTask.getDeployKey());
        vo.setDeployUrl(deployTask.getDeployUrl());
        vo.setErrorMessage(deployTask.getErrorMessage());
        vo.setRetryCount(deployTask.getRetryCount());
        vo.setMessageKey(deployTask.getMessageKey());
        vo.setCreateTime(deployTask.getCreateTime());
        vo.setUpdateTime(deployTask.getUpdateTime());
        return vo;
    }

    @Override
    public DeployTaskVO getDeployTaskVOById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        }
        DeployTask task = this.getById(id);
        ThrowUtils.throwIf(task == null, ErrorCode.NOT_FOUND_ERROR, "部署任务不存在");
        return getDeployTaskVO(task);
    }

    /**
     * 标记任务为运行中（可幂等，只允许从 PENDING 或 FAILED 状态扭转为 RUNNING）。
     *
     * @return true 表示已成功占用
     */
    public boolean markRunning(Long taskId) {
        DeployTask partial = new DeployTask();
        partial.setId(taskId);
        partial.setStatus(DeployTaskStatusEnum.RUNNING.getValue());
        partial.setUpdateTime(LocalDateTime.now());
        return this.updateById(partial);
    }

    /**
     * 标记任务成功。
     */
    public boolean markSuccess(Long taskId, String deployUrl) {
        DeployTask partial = new DeployTask();
        partial.setId(taskId);
        partial.setStatus(DeployTaskStatusEnum.SUCCESS.getValue());
        partial.setDeployUrl(deployUrl);
        partial.setErrorMessage(null);
        partial.setUpdateTime(LocalDateTime.now());
        return this.updateById(partial);
    }

    /**
     * 标记任务失败。
     */
    public boolean markFailed(Long taskId, String errorMessage) {
        DeployTask partial = new DeployTask();
        partial.setId(taskId);
        partial.setStatus(DeployTaskStatusEnum.FAILED.getValue());
        partial.setErrorMessage(errorMessage);
        partial.setUpdateTime(LocalDateTime.now());
        return this.updateById(partial);
    }

    /**
     * 自增重试计数。
     */
    public boolean incrementRetryCount(Long taskId) {
        DeployTask current = this.getById(taskId);
        if (current == null) {
            return false;
        }
        int newCount = (current.getRetryCount() == null ? 0 : current.getRetryCount()) + 1;
        DeployTask partial = new DeployTask();
        partial.setId(taskId);
        partial.setRetryCount(newCount);
        partial.setUpdateTime(LocalDateTime.now());
        return this.updateById(partial);
    }
}
