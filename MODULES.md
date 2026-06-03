# MODULES.md

## 1. 文档目标
本文档描述 **Yume AI Code Mother** 的模块划分、职责边界、关键接口与协作方式，帮助研发、测试与运维快速理解系统。

---

## 2. 模块总览

### 2.1 后端模块（Spring Boot）
后端代码根路径：`src/main/java/com/yume/yuaicodemother`

| 模块目录 | 职责 | 关键类/文件 |
|---|---|---|
| `controller` | 对外 REST / SSE 接口层 | `AppController.java`、`UserController.java`、`ChatHistoryController.java`、`WorkflowSseController.java` |
| `service` + `service/impl` | 业务编排、权限内聚、数据访问协调 | `AppServiceImpl.java`、`ChatHistoryServiceImpl.java`、`UserServiceImpl.java` |
| `ai` | AI 服务工厂、模型路由、Guardrail、Tool 管理 | `AiCodeGeneratorServiceFactory.java`、`AiCodeGenTypeRoutingServiceFactory.java`、`ai/tools/*` |
| `langgraph4j` | 工作流编排（节点、状态、条件边、循环边） | `CodeGenWorkflow.java`、`langgraph4j/node/*` |
| `core` | 代码生成外观、流处理、代码解析与保存、构建 | `AiCodeGeneratorFacade.java`、`handler/StreamHandlerExecutor.java`、`builder/VueProjectBuilder.java` |
| `mapper` | MyBatis-Flex 数据映射层 | `AppMapper.java`、`UserMapper.java`、`ChatHistoryMapper.java` |
| `model` | DTO / Entity / VO / Enum 数据模型 | `model/dto/*`、`model/entity/*`、`model/vo/*` |
| `mq` | RocketMQ 消息生产/消费，异步任务编排 | `producer/AppTaskProducer.java`、`consumer/DeployTaskConsumer.java`、`consumer/ScreenshotTaskConsumer.java` |
| `ratelimter` | 分布式限流注解 + AOP | `RateLimitAspect.java`、`RateLimit.java` |
| `aop` + `annotation` | 统一鉴权切面与注解 | `AuthInterceptor.java`、`AuthCheck.java` |
| `config` | 缓存、Redis Memory、跨域、模型配置等 | `RedisCacheManagerConfig.java`、`RedisChatMemoryStoreConfig.java` |
| `common` + `exception` | 通用响应、异常体系、错误码 | `BaseResponse.java`、`GlobalExceptionHandler.java` |
| `constant` | 全局常量定义 | `AppConstant.java`、`MqConstant.java` |
| `utils` / `manager` / `generator` | 工具类与辅助组件 | 例如截图、键生成、代码生成辅助 |

### 2.2 前端模块（Vue 3）
前端代码根路径：`yume-ai-code-mother-frontend/src`

| 模块目录 | 职责 | 关键文件 |
|---|---|---|
| `pages` | 业务页面（用户端 + 管理端） | `pages/app/AppChatPage.vue`、`pages/app/AppEditPage.vue`、`pages/admin/*` |
| `components` | 可复用组件 | `AppDetailModal.vue`、`DeploySuccessModal.vue`、`MarkdownRenderer.vue` |
| `api` | OpenAPI 生成/封装接口调用 | `appController.ts`、`chatHistoryController.ts` |
| `router` | 路由注册与页面映射 | `router/index.ts` |
| `stores` | 登录态等全局状态 | `stores/loginUser.ts` |
| `utils` | 业务工具函数 | `visualEditor.ts`、`codeGenTypes.ts` |
| `config` | 环境与地址配置 | `config/env.ts` |

---

## 3. 核心后端模块详解

## 3.1 Controller 接口模块

### 3.1.1 应用模块接口（AppController）
位置：`src/main/java/com/yume/yuaicodemother/controller/AppController.java`

**职责**
- 应用 CRUD（用户与管理员视角）
- AI 对话生成 SSE 输出
- 部署与代码下载
- 精选应用分页查询（缓存）

**关键接口**
- `GET /api/app/code/gen/code`：SSE 生成接口（带 `@RateLimit`）
- `POST /api/app/add`：创建应用
- `POST /api/app/deploy`：提交部署任务（返回 `DeployTaskVO`，后台异步执行）
- `GET /api/app/deploy/task/get`：查询部署任务状态（前端轮询）
- `GET /api/app/download/{appId}`：下载代码
- `POST /api/app/admin/*`：管理员管理接口（`@AuthCheck`）

**输入输出约定**
- 普通接口返回 `BaseResponse<T>`
- SSE 接口输出 `ServerSentEvent<String>`，结束事件 `done`
- 业务错误通过 SSE 事件 `business-error` 返回

### 3.1.2 用户与对话管理接口
- 用户管理：`UserController.java`
- 对话管理：`ChatHistoryController.java`

**职责**
- 登录注册、用户信息维护
- 对话历史分页查询（含管理员权限控制）

---

## 3.2 Service 业务编排模块

### 3.2.1 AppServiceImpl
位置：`src/main/java/com/yume/yuaicodemother/service/impl/AppServiceImpl.java`

**职责**
- 应用创建时调用 AI 路由决定生成类型（HTML / MULTI_FILE / VUE_PROJECT）
- 对话生成总流程编排：参数校验 -> 权限校验 -> 写入用户消息 -> 调用 AI 流 -> 回写 AI 结果
- 部署任务提交：校验 -> 创建 `deploy_task` -> 发布 RocketMQ 消息 -> 返回任务视图

**关键方法**
- `chatToGenCode(Long appId, String message, User loginUser)`
- `createApp(AppAddRequest appAddRequest, User loginUser)`
- `deployApp(Long appId, User loginUser)`（返回 `DeployTaskVO`，实际的构建部署由 MQ Consumer 异步执行）

### 3.2.2 ChatHistoryServiceImpl
位置：`src/main/java/com/yume/yuaicodemother/service/impl/ChatHistoryServiceImpl.java`

**职责**
- 记录用户/AI 对话
- 基于游标分页查询历史消息
- 将数据库历史消息加载到 LangChain4j ChatMemory

**关键能力**
- 支持 AI reasoning 内容入库与恢复
- 加载前清理 ChatMemory，避免重复注入

---

## 3.3 AI 服务模块（LangChain4j）

### 3.3.1 AI 服务工厂
位置：`src/main/java/com/yume/yuaicodemother/ai/AiCodeGeneratorServiceFactory.java`

**职责**
- 按 `appId + codeGenType` 创建并缓存 AI 服务实例
- 绑定工具、记忆、护轨（Guardrail）
- 区分不同生成类型使用不同模型配置

**缓存策略**
- 使用 Caffeine：
  - 最大缓存 1000
  - 写后 30 分钟过期
  - 访问后 10 分钟过期

**记忆策略**
- `MessageWindowChatMemory` + `RedisChatMemoryStore`
- 启动时从 DB 回放最近 N 条历史（当前 20）

### 3.3.2 Tool 管理模块
位置：`src/main/java/com/yume/yuaicodemother/ai/tools/ToolManager.java`

**职责**
- 启动时自动注册所有 Tool
- 为 AI Service 提供工具实例数组

---

## 3.4 Workflow 编排模块（LangGraph4j）

位置：`src/main/java/com/yume/yuaicodemother/langgraph4j/CodeGenWorkflow.java`

**职责**
- 将代码生成流程拆解为有状态节点
- 通过条件边、循环边实现质量校验回路

**节点链路**
1. `image_collector`
2. `prompt_enhancer`
3. `router`
4. `code_generator`
5. `code_quality_check`
6. 条件路由：
   - `build` -> `project_builder`
   - `skip_build` -> `END`
   - `fail` -> `code_generator`（回环）

**输出模式**
- 支持同步执行
- 支持 Flux SSE 流式事件输出（`workflow_start`、`step_completed`、`workflow_completed`）

---

## 3.5 Core 生成执行模块

### 3.5.1 AiCodeGeneratorFacade
位置：`src/main/java/com/yume/yuaicodemother/core/AiCodeGeneratorFacade.java`

**职责**
- 对上提供统一入口：按类型生成并保存代码
- 区分普通文本流与 ToolCalling 流
- 在流结束后触发代码解析、持久化与 Vue 构建

### 3.5.2 StreamHandlerExecutor
位置：`src/main/java/com/yume/yuaicodemother/core/handler/StreamHandlerExecutor.java`

**职责**
- 根据生成类型选择流处理器：
  - `VUE_PROJECT` -> `JsonMessageStreamHandler`
  - `HTML/MULTI_FILE` -> `SimpleTextStreamHandler`

### 3.5.3 VueProjectBuilder
位置：`src/main/java/com/yume/yuaicodemother/core/builder/VueProjectBuilder.java`

**职责**
- 在代码目录执行 `npm install` + `npm run build`
- 提供命令超时控制与失败检测

**超时策略**
- `npm install`：300s
- `npm run build`：180s

---

## 3.6 基础能力模块

### 3.6.1 鉴权模块
- 注解：`src/main/java/com/yume/yuaicodemother/annotation/AuthCheck.java`
- 切面：`src/main/java/com/yume/yuaicodemother/aop/AuthInterceptor.java`

**职责**
- 拦截标注接口
- 校验登录态与角色（管理员/普通用户）

### 3.6.2 限流模块
- 注解：`ratelimter/annotation/RateLimit.java`
- 切面：`ratelimter/aspect/RateLimitAspect.java`

**职责**
- 支持 USER / API / IP 三种粒度
- 基于 Redisson `RRateLimiter` 分布式限流

### 3.6.3 缓存模块
- 配置：`src/main/java/com/yume/yuaicodemother/config/RedisCacheManagerConfig.java`

**职责**
- Spring Cache 统一接入 Redis
- 默认 TTL 30 分钟
- `good_app_page` 配置 5 分钟 TTL

### 3.6.4 异常处理模块
- `src/main/java/com/yume/yuaicodemother/exception/GlobalExceptionHandler.java`

**职责**
- 统一拦截 `BusinessException` / `RuntimeException`
- 普通请求返回标准 JSON
- SSE 请求返回结构化错误事件

---

## 3.7 RocketMQ 异步任务模块

### 3.7.1 Producer
位置：`src/main/java/com/yume/yuaicodemother/mq/producer/AppTaskProducer.java`

**职责**
- 发布部署任务消息（`app-task-topic:deploy-request`）
- 发布截图任务消息（`app-task-topic:screenshot-generate`）

### 3.7.2 Deploy Task Consumer
位置：`src/main/java/com/yume/yuaicodemother/mq/consumer/DeployTaskConsumer.java`

**职责**
- 消费部署消息并执行构建、拷贝、App 更新
- 幂等保护：Redisson 锁 + 任务 SUCCESS 状态守卫
- 失败时标记 FAILED 并抛出异常触发 RocketMQ 重试
- 成功后发布截图消息

### 3.7.3 Screenshot Task Consumer
位置：`src/main/java/com/yume/yuaicodemother/mq/consumer/ScreenshotTaskConsumer.java`

**职责**
- 消费截图消息，调用 `ScreenshotService` 生成封面
- 写回 `App.cover`

### 3.7.4 任务管理服务
位置：`src/main/java/com/yume/yuaicodemother/service/impl/DeployTaskServiceImpl.java`

**职责**
- 创建任务（`PENDING`）
- 状态流转：`PENDING -> RUNNING -> SUCCESS/FAILED`
- 重试计数
- 任务视图转换

### 3.7.5 MQ 常量
位置：`src/main/java/com/yume/yuaicodemother/constant/MqConstant.java`

**职责**
- 统一管理 Topic/Tag/ConsumerGroup/LockKey 前缀

---

## 4. 前端模块详解

## 4.1 应用对话页（AppChatPage）
位置：`yume-ai-code-mother-frontend/src/pages/app/AppChatPage.vue`

**职责**
- 加载应用信息与历史消息
- 通过 `EventSource` 调用 SSE 生成接口
- 处理 `done` / `business-error` / `onerror`
- 实时更新消息区与右侧预览
- 接入可视化编辑器选中元素并拼接提示词
- 部署改为任务提交 + 2 秒轮询状态

**关键状态**
- `isGenerating`：是否在生成中
- `messages`：对话列表
- `previewUrl`：预览地址
- `selectedElementInfo`：选中元素上下文
- `deployTaskId` / `deployPollingTimer`：部署任务轮询

## 4.2 应用编辑页（AppEditPage）
位置：`yume-ai-code-mother-frontend/src/pages/app/AppEditPage.vue`

**职责**
- 普通用户编辑应用名称
- 管理员可编辑封面与优先级
- 展示应用详细信息与跳转能力

## 4.3 路由与权限显示
位置：`yume-ai-code-mother-frontend/src/router/index.ts`

**职责**
- 注册用户端与管理端页面路由
- 结合登录态在页面侧做操作显隐控制

---

## 5. 数据模型模块

### 5.1 核心实体
- `App`：应用元数据（名称、类型、部署键、封面、优先级等）
- `User`：用户信息与角色
- `ChatHistory`：对话历史（消息类型、正文、reasoning 内容）
- `DeployTask`：部署任务（状态、重试次数、消息键等）

### 5.2 数据映射
- XML 映射：`src/main/resources/mapper/*.xml`
- 脚本：`sql/create_table.sql`

---

## 6. 配置与资源模块

### 6.1 后端配置
- 主配置：`src/main/resources/application.yml`
- 本地配置：`src/main/resources/application-local.yml`

### 6.2 Prompt 模板
目录：`src/main/resources/prompt`

包括：
- 代码生成（HTML / Multi-file / Vue）
- 路由决策
- 质检
- 图像采集

### 6.3 前端环境配置
- `yume-ai-code-mother-frontend/src/config/env.ts`
- Vite 代理：`yume-ai-code-mother-frontend/vite.config.ts`

---

## 7. 模块协作关系（高频链路）

## 7.1 创建应用链路
1. 前端提交 `initPrompt`
2. 后端 `AppServiceImpl.createApp` 调用路由 AI 判定生成类型
3. 写入 App 数据并返回 `appId`

## 7.2 对话生成链路
1. 前端 SSE 请求 `/app/code/gen/code`
2. `AppController` -> `AppServiceImpl.chatToGenCode`
3. 写入用户消息
4. `AiCodeGeneratorFacade` 触发生成流
5. `StreamHandlerExecutor` 按类型处理并回写 AI 消息
6. 前端接收 chunk 并渲染

## 7.3 异步部署链路（RocketMQ）
1. 前端调用 `/app/deploy`，后端校验权限与代码目录
2. 后端创建 `deploy_task`（PENDING）并发布 RocketMQ 消息
3. 前端开始轮询 `/app/deploy/task/get`
4. `DeployTaskConsumer` 异步执行：Vue build -> 拷贝部署目录 -> 更新 App -> 标记 SUCCESS
5. 部署成功后发布截图消息，`ScreenshotTaskConsumer` 异步生成封面
6. 前端轮询到 SUCCESS 后展示部署 URL

---

## 8. 模块边界与演进建议

### 8.1 当前边界
- Controller 仅处理参数与协议转换
- 业务规则集中在 Service
- AI 编排与执行解耦在 `ai` / `langgraph4j` / `core`
- 通用治理（鉴权、限流、缓存、异常）横切

### 8.2 后续演进建议
- ~~将"构建部署"从 AppService 拆为独立部署域服务~~（已完成：RocketMQ 异步解耦）
- 将"对话存储"拆为消息域，支持异步事件总线
- 将"工作流节点"按插件化目录组织，支持可配置装配
- 增加观测模块（metrics/tracing）聚合 AI 调用关键指标

---

## 9. 快速定位索引

- 生成入口：`controller/AppController.java:60`
- 部署任务提交：`controller/AppController.java:98`
- 任务状态查询：`controller/AppController.java:117`
- 生成编排：`service/impl/AppServiceImpl.java:78`
- AI 服务工厂：`ai/AiCodeGeneratorServiceFactory.java:31`
- 工作流定义：`langgraph4j/CodeGenWorkflow.java:33`
- 流处理选择：`core/handler/StreamHandlerExecutor.java:33`
- MQ Producer：`mq/producer/AppTaskProducer.java`
- Deploy Consumer：`mq/consumer/DeployTaskConsumer.java`
- Screenshot Consumer：`mq/consumer/ScreenshotTaskConsumer.java`
- 任务状态管理：`service/impl/DeployTaskServiceImpl.java`
- 鉴权切面：`aop/AuthInterceptor.java:33`
- 限流切面：`ratelimter/aspect/RateLimitAspect.java:37`
- 全局异常：`exception/GlobalExceptionHandler.java:22`
