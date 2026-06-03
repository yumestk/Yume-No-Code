package com.yume.yuaicodemother.service;

import com.mybatisflex.core.service.IService;
import com.yume.yuaicodemother.model.entity.DeployTask;
import com.yume.yuaicodemother.model.vo.DeployTaskVO;

/**
 * 部署任务服务层。
 */
public interface DeployTaskService extends IService<DeployTask> {

    /**
     * 创建部署任务。
     *
     * @param appId 应用 id
     * @param userId 用户 id
     * @param codeGenType 代码生成类型
     * @param deployKey 部署标识
     * @param messageKey 消息 key
     * @return 部署任务
     */
    DeployTask createDeployTask(Long appId, Long userId, String codeGenType, String deployKey, String messageKey);

    /**
     * 是否存在执行中的部署任务。
     *
     * @param appId 应用 id
     * @return 是否存在
     */
    boolean hasRunningDeployTask(Long appId);

    /**
     * 获取部署任务封装类。
     *
     * @param deployTask 部署任务
     * @return 封装类
     */
    DeployTaskVO getDeployTaskVO(DeployTask deployTask);

    /**
     * 根据 id 获取部署任务封装类。
     *
     * @param id 任务 id
     * @return 封装类
     */
    DeployTaskVO getDeployTaskVOById(Long id);
}
