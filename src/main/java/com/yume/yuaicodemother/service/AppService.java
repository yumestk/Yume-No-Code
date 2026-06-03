package com.yume.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.yume.yuaicodemother.model.dto.app.AppAddRequest;
import com.yume.yuaicodemother.model.dto.app.AppQueryRequest;
import com.yume.yuaicodemother.model.entity.App;
import com.yume.yuaicodemother.model.entity.User;
import com.yume.yuaicodemother.model.vo.AppVO;
import com.yume.yuaicodemother.model.vo.DeployTaskVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author <a href="https://github.com/yumestk">yumestk</a>
 */
public interface AppService extends IService<App> {

    /**
     * 通过对话生成应用代码
     * @param appId 应用ID
     * @param message 提示词
     * @param loginUser 登录用户
     * @return
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 创建应用
     * @param appAddRequest
     * @param loginUser
     * @return
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 提交部署任务（异步执行）。
     *
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署任务视图
     */
    DeployTaskVO deployApp(Long appId, User loginUser);

    /**
     * 获取应用封装类
     * @param app
     * @return
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用封装类列表
     * @param appList
     * @return
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造应用查询条件
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);


}
