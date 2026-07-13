# 开发计划与验证记录

最后更新：2026-07-13

阶段状态只使用：未开始、设计完成、实现中、待验证、已完成、受阻。

## 阶段总览

| 阶段 | 状态 | 完成门 |
| --- | --- | --- |
| 1. 规格与工程骨架 | 已完成 | 规格、目录、版本和验收条件一致，Git 可追踪 |
| 2. 基础设施依赖 | 已完成 | MySQL、Redis、MinIO 可连接，后端环境变量样例完整 |
| 3. 数据库与后端 | 已完成 | 空库 SQL、后端编译测试、权限与状态机验证通过 |
| 4. 微信小程序 | 待验证 | 构建/类型检查通过，核心用户流程调用真实接口 |
| 5. 管理后台 | 待验证 | 构建通过，审核和治理流程调用真实接口 |
| 6. 联调与自动化测试 | 实现中 | 主流程、反向权限、并发、WebSocket 和文件验证通过 |
| 7. 安全性能与交付 | 未开始 | 无 P0/P1，README 与实际命令一致 |

## 开发顺序

1. 固定项目规格、数据库契约和环境配置。
2. 准备 MySQL、Redis、MinIO，并按 `004-backend/env.example` 设置环境变量。
3. 后端先实现统一响应、异常、鉴权、数据权限与测试基础。
4. 按身份绑定、活动审核、申请名额、WebSocket、文件、治理顺序开发。
5. 小程序和管理端在 API 契约稳定后并行开发。
6. 运行后端测试、前端构建、API 集成、并发和浏览器验收。
7. 完成安全、依赖、性能检查和交付文档。

## 验证记录

### 2026-07-13 迁移至 BS2026 004 项目

- 项目从 `E:\project\test` 复制归档到 `E:\project\vibe coding\BS2026`，目录映射为 `004-backend`、`004-miniproject`（原生微信小程序）、`004-admin`、`docs/004` 和 `sql/004`；源目录保留，未执行删除或移动。
- 迁移明确排除 Git/Serena/Agent/Playwright 本机状态、Maven 与 npm 依赖缓存、`target`、`node_modules`、`dist`、`.test-dist`、`.cloudbase`、运行数据、日志、PID、临时截图、`.env`、`application-local.yml` 和 `project.private.config.json`；保留 `004-backend/env.example`、源码、测试、锁文件、SQL 和项目文档。
- 后端测试 SQL 路径已调整为 `../sql/004/schema.sql` 和 `../sql/004/data.sql`，确保从 `004-backend` 运行测试时读取当前项目脚本。
- 本文件后续历史条目中的 `backend/`、`frontend/` 和 `E:\project\test` 命令保留为原始验证证据；当前可复现命令以本节和 `docs/004/README.md` 为准。
- `mvn -o test`（`004-backend`）：退出状态 0；33 个测试通过，失败 0、错误 0、跳过 0。
- `npm run lint`、`npm run typecheck`、`npm test -- --run`、`npm run build`（`004-admin`）：全部退出状态 0；1 个组件测试通过，生产构建成功。
- `npm run check`（`004-miniproject`）：退出状态 0；TypeScript 检查和 17 个 Node 测试通过。
- 本地服务连接参数以 `004-backend/env.example` 为准。
- 迁移完整性核对：后端 125、管理端 44、小程序 110、SQL 4、核心文档 2 个文件的相对路径与 SHA-256 均与源项目一致（仅放行测试 SQL 路径、迁移后 README 和部署目录移除的预期差异）；禁迁/运行产物扫描为 0 项，源目录仍存在。

### 2026-07-13 管理端活动列表与完整链详情

- 新增管理员活动列表接口 `GET /api/v1/operations/activities` 与只读详情接口 `GET /api/v1/operations/activities/{id}`；平台管理员查看全平台，校园审核员强制限制为所属校园，学生返回 403。
- 列表支持关键词、审核状态、生命周期、治理状态、活动形式筛选、分页、软删除过滤和稳定排序；列表不返回成员精确集合地点。
- 管理端新增 `/operations/activities` 页面和侧栏入口；详情复用审核抽屉但启用只读模式，隐藏认领、通过、驳回和提交决策。
- 完整链详情补充审核/生命周期/治理状态、审核原因、完成确认截止、最近更新时间、申请处理说明、成员完成/退出信息，以及关联举报、处置、申诉和恢复审计时间线。
- `mvn -o -Dtest=ActivityReviewDetailIntegrationTest test`：退出状态 0；3 个测试通过，覆盖详情范围、列表范围/角色和治理时间线。
- `mvn -o test`：退出状态 0；33 个后端测试通过，失败 0、错误 0、跳过 0。
- `npm run lint && npm run typecheck && npm test -- --run && npm run build`（`frontend/admin`）：退出状态 0；前端检查、只读模式测试和生产构建通过。

### 2026-07-11 规格与工程骨架

- `git init -b main`：退出状态 0，初始化本地仓库。
- `git diff --check`：退出状态 0，无空白错误。
- 已创建 `docs/project-spec.md`、`docs/development-plan.md`、`README.md` 和模块目录。

### 2026-07-11 数据库、Redis 与后端核心链

- `mysql ... source sql/schema.sql; source sql/data.sql`（独立验证库 `campus_buddy_verify_20260711`）：退出状态 0；空库生成 20 张表、5 个用户、1 条示例活动。
- `redis-cli -p 6380 ... ping`：退出状态 0，返回 `PONG`。当前验证使用 Windows 原生 Redis 8.6.2；Redis 仅保存临时认证数据。
- `mvn -o -DskipTests package`：退出状态 0；59 个主源码文件编译并生成可执行 JAR。
- 真实 HTTP 纵向验证：未登录 401、错误密码 401、学生越权编辑 403、发布草稿、提交审核、审核认领、审核通过、报名和接受均通过；成员加入前精确地点不返回，加入后可见；重复申请返回 409；数据库断言 `accepted_count:有效参与者:会话 = 1:1:1`。
- `mvn -o test`：退出状态 0；2 个 Spring Boot + MySQL 集成测试全部通过。容量为 2 时并发接受 20 个申请，结果严格为 2 个成功、18 个 409，数据库 `accepted_count=2` 且有效参与者数为 2。
- 已修复真实启动发现的 Mapper 扫描过宽问题：普通接口不再被误注册为 MyBatis Mapper；JWT Redis 失效检查在真实请求中验证通过。

### 2026-07-11 基础设施环境差异

- 先前基础设施安装结果在当前系统状态下无法复验；后续以本机 MySQL、Redis 和 MinIO 服务为准。
- 为不中断业务开发，MySQL 8.0.45 与 Redis 8.6.2 曾用本机进程完成当前验证。

### 2026-07-11 暂停检查点（下次从这里继续）

用户要求暂停开发。本节记录暂停瞬间的工作区事实；所有并行开发任务均已中止，没有后台代理继续修改文件。

已落地且已有验证证据：

- 后端已覆盖微信/管理员登录、校园认证、活动发布与审核、报名与并发名额、生命周期、站内通知、收藏评价、举报申诉、私有文件、WebSocket 单次票据与消息持久化。此前完整执行 `mvn -o test`：退出状态 0，5 个 Spring Boot + MySQL 集成测试通过。
- 管理后台已有登录、工作台、活动/身份/举报/文件审核、审计日志，以及 403/404 页面；本轮修复了文件预览请求竞态、登录态刷新同步和审计字段展示。暂停前执行 `npm run build`：退出状态 0，`vue-tsc` 与 Vite 生产构建通过，最大公共 JavaScript 产物 gzip 约 47.70 KB。
- 原生微信小程序已创建 15 个页面入口，包含微信登录、校园绑定、广场、详情、消息、个人中心、活动编辑、我的活动/申请、申请管理、活动工作区、资料编辑、收藏信誉、安全隐私、举报申诉和黑名单；页面调用真实 REST/WS 接口。暂停前执行 `npm run typecheck`：退出状态 0。
- 小程序所有业务 ID 按字符串处理，已规避 JavaScript 大整数精度损坏；`wx.request` 不支持 `PATCH`，因此资料更新改用后端等价 `PUT /me/profile`。
- 图片方案确定并实现为 MinIO/对象存储抽象，不使用 Base64；活动图片采用“上传—审核—绑定”流程。消息队列没有引入，也没有获得使用授权；当前审查结论仍是不需要消息队列。

暂停时正在进行、尚未完成验证的改动：

- 正在修复“REST 退出后旧 WebSocket 仍可发送、退出前未消费票据仍可握手”的会话失效漏洞。回归测试已先失败，证明旧实现存在问题；实现现已让 WS 票据保存 `userId + tokenVersion`，握手、每帧发送和广播前都会复查账号与令牌版本。
- 修复后的第一次定向测试因旧测试使用未认证用户申请 WS 票据而得到预期的 403，测试夹具已改为独立的已认证非会话成员 `ws-outsider`，但用户要求暂停后未再重跑。因此该安全修复当前状态是“实现中、待验证”，不能写成已通过。
- 下次恢复后的第一条命令：`cd backend; mvn -o -Dtest=CoreFlowIntegrationTest#websocketUsesSingleUseTicketPersistsBeforeAckAndSupportsCursorPull test`。通过后立即执行 `mvn -o test` 重跑完整 5 项集成测试。
- 微信开发者工具尚未实际编译/预览。下次首次操作开发者工具前，必须按 `miniprogram-dev-skill` 读取 `skill.yaml` 版本，执行开发者工具状态检查并确认当前登录 OpenID，再进行编译。
- MinIO 真实对象上传仍未复验。

后续开发优先级：

1. 用微信开发者工具编译并逐页修复 WXML/运行时问题，再完成真实接口联调。
2. 恢复 MinIO，完成真实对象存储验证。
3. 继续处理真实微信登录、内容安全接口、Redis 限流等剩余高风险项。
4. 完成管理端和小程序浏览器/开发者工具 E2E、反向权限、安全、性能与交付文档。

当前仓库已初始化 Git，但项目文件仍未提交；工作区内容全部保留，恢复时不要重新初始化或覆盖现有实现。

后续每次验证必须追加实际命令、退出状态和关键断言，不使用“代码已写”代替证据。

### 2026-07-12 恢复开发：WS 回归与后台配置管理

- `mvn -o -Dtest=CoreFlowIntegrationTest#websocketUsesSingleUseTicketPersistsBeforeAckAndSupportsCursorPull test`：退出状态 0；1 个 WebSocket 集成测试通过。验证单次票据、消息幂等、断线游标补拉、退出后旧连接拒绝发送、退出前签发票据拒绝握手。
- `wechatide -c Codex -t check_devtools_status --skill-version 0.2.5`：两次均未成功完成；第二次退出状态 124，输出 `CONNECT_ERROR` 和 `wait DevTools MCP port timeout`。微信开发者工具编译仍未验证。
- 已新增平台后台接口：`/api/v1/admin/users`、`/api/v1/admin/campuses`、`/api/v1/admin/tags`，覆盖平台用户管理、校园管理、审核员配置和推荐标签配置；平台管理员专用，写操作记录 `audit_log`，用户权限/状态变化会递增 `token_version` 使旧令牌失效。
- 已新增管理后台页面：用户与审核员、校园管理、推荐标签；导航仅平台管理员可见，页面使用真实接口、分页、筛选、空状态和失败重试。
- `mvn -o -DskipTests package`：退出状态 0；111 个主源码文件编译并生成可执行 JAR。
- `mvn -o -Dtest=PlatformAdminManagementIntegrationTest test`：退出状态 0；1 个集成测试通过。验证平台管理员可创建/更新校园、配置审核员、创建/停用标签，校园审核员访问后台配置接口返回 403，用户配置后旧 access token 返回 401，审计日志不少于 5 条。
- `npm run build`（`frontend/admin`）：退出状态 0；`vue-tsc` 和 Vite 生产构建通过，新增后台配置页面全部进入懒加载产物。
- `mvn -o test`：退出状态 0；20 个 Spring Boot + MySQL 集成测试全部通过，失败 0、错误 0、跳过 0。
- `npm run typecheck`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查通过。

### 2026-07-12 生命周期维护与治理认领超时

- 已新增定时维护服务 `LifecycleMaintenanceService`，默认每 60 秒执行一次：释放活动审核过期认领、报名截止后过期仍在招募的活动、完成确认超时后自动确认未响应成员并完成活动、释放举报案件过期认领。
- 已修复举报案件认领 SQL：`claimWithExpiry` 现在会同时写入 `status='REVIEWING'`、`assignee_id` 和 `claim_expires_at`；过期认领可被维护任务释放，避免案件长期卡住。
- 已修复维护任务审计 JSON：使用 `ObjectMapper` 序列化 `before_state/after_state`，避免 `Map.toString()` 写入 MySQL JSON 字段失败。
- 已新增完成争议闭环：成员在完成确认阶段提交异议时，生成 `reason_code='COMPLETION_DISPUTE'` 的治理案件，活动保持 `COMPLETION_PENDING`。
- `mvn -o -Dtest=LifecycleMaintenanceIntegrationTest test`：退出状态 0；2 个集成测试通过。关键断言：活动审核过期认领释放为 `reviewer_id=NULL`，举报过期认领释放为 `status=SUBMITTED/assignee_id=NULL`，完成确认超时后成员变为 `AUTO_CONFIRMED` 且活动 `COMPLETED`、会话 `READ_ONLY`；完成异议会生成 `COMPLETION_DISPUTE` 举报案件。
- `mvn -o test`：退出状态 0；22 个 Spring Boot + MySQL 集成测试全部通过，失败 0、错误 0、跳过 0。
- `npm run build`（`frontend/admin`）：退出状态 0；`vue-tsc` 和 Vite 生产构建通过。
- `npm run typecheck`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查通过。
- 仍未验证：微信开发者工具真机预览/上传、完整活动端到端浏览器验收。

### 2026-07-12 微信小程序登录与校园认证联调

- `Test-NetConnection 127.0.0.1 -Port 8080/3307/6380/9000`：退出状态 0；后端、MySQL、Redis、MinIO 端口均可从 Windows 访问。
- `wechatide -c Codex -t check_devtools_status --skill-version 0.2.5`：退出状态 0；微信开发者工具已登录，`skillVersion=0.2.5` 与 agent skill 一致。
- `wechatide -c Codex -t open_project_window --project E:\project\test\frontend\miniprogram`：退出状态 0；复用小程序项目窗口。
- `wechatide -c Codex -t simulator_open_page --project E:\project\test\frontend\miniprogram --page pages/login/index`：退出状态 0；登录页可打开。
- 小程序真实微信登录：勾选隐私同意后点击“微信快捷登录”，`POST /api/v1/auth/wechat-login` 返回 200，返回 access/refresh token，页面跳转到 `pages/campus-bind/index`。
- 校园认证后端闭环：使用当前学生 token 上传 `IDENTITY_PROOF` 证明图片到 `/api/v1/files`，文件审核员调用 `/api/v1/review/files/{id}/decision` 审核通过；学生调用 `/api/v1/me/identity-bindings` 提交学号 `STU20260712175647`；审核员调用 `/api/v1/review/identity-bindings/{id}/decision` 审核通过。关键断言：证明文件 `APPROVED`，身份申请从 `PENDING` 到 `APPROVED`，学生 `campus_id=1`、`verification_status=APPROVED`、`token_version=1`。
- 认证后小程序访问广场：重新签发同一 dev code 的学生 token 后打开 `pages/discover/index`，当前页为“搭子广场”；`GET /api/v1/me/identity-bindings/current` 返回 200 且 `status=APPROVED`，页面数据加载到 1 条已审核活动；console error 为空。

### 2026-07-12 微信小程序视觉增强与页面验证

- 本轮增强清单：
  - 广场页：增加 Hero 卡、活动统计、推荐先看、报名进度、动态关键词快捷筛选。
  - 个人中心：增加认证状态卡、资料完整度、兴趣标签、常用能力快捷入口和更丰富的数据概览。
  - 消息中心：增加消息仪表盘、会话/未读/今日统计、通知未读/已读分组和类型标签。
  - 图标策略：继续使用本地 PNG 资源，不接入在线 Iconify 或其他在线图标。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 3 个格式工具测试全部通过。
- 首次微信开发者工具单文件编译误传项目路径为 `frontend/miniprogram/src`，返回 `app.json: 未找到 ["pages"][0] 对应的 pages/login/index.js 文件`；已确认正确项目根是 `frontend/miniprogram`，`miniprogramRoot` 为 `src/`。
- `wechatide -c CodeBuddy -t compile_wxml --project E:/project/test/frontend/miniprogram --file-path pages/discover/index.wxml/pages/profile/index.wxml/pages/messages/index.wxml`：3 个文件均 `success=true`。
- `wechatide -c CodeBuddy -t compile_wxss --project E:/project/test/frontend/miniprogram --file-path pages/discover/index.wxss/pages/profile/index.wxss/pages/messages/index.wxss`：3 个文件均 `success=true`。
- `automation_navigate switchTab` 分别打开 `/pages/discover/index`、`/pages/profile/index`、`/pages/messages/index`：退出状态 0；运行时当前页分别为“搭子广场”、“个人中心”、“消息”。
- `get_app_console_content --command "grep -i error"`：返回空字符串；未发现 error 日志。
- `get_app_network_content --command "grep -i fail"`：返回空字符串；未发现失败请求。
- 截图证据已生成：`tmp-miniprogram-discover-enhanced.png`、`tmp-miniprogram-profile-enhanced.png`、`tmp-miniprogram-messages-enhanced.png`。

### 2026-07-12 微信小程序活动详情与发布页增强

- 本轮继续开发清单：
  - 活动详情页：增加名额进度、剩余名额、校园认证发布提示、参与规则和更清晰的活动安排展示。
  - 活动发布页：增加常用场景模板、发布前检查清单，保留原有真实提交、保存草稿、图片审核绑定流程。
  - 本轮未新增接口、未引入依赖、未改后端，继续复用现有活动、文件和认证接口。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 3 个格式工具测试全部通过。
- `wechatide -c CodeBuddy -t check_devtools_status --skill-version 0.2.5`：退出状态 0；微信开发者工具已登录，`skillVersion=0.2.5` 与 agent skill 一致。
- `compile_wxml`：`pages/activity-detail/index.wxml`、`packageBusiness/activity-editor/index.wxml` 均 `success=true`。
- `compile_wxss`：`pages/activity-detail/index.wxss`、`packageBusiness/activity-editor/index.wxss` 均 `success=true`。
- 页面验证：
  - `automation_navigate reLaunch /packageBusiness/activity-editor/index`：退出状态 0；当前页为“发布搭子活动”，截图保存到 `tmp-miniprogram-editor-enhanced.png`。
  - 首次详情页使用旧测试 ID `1` 返回 404；当前真实活动 ID 为 `2000`。
  - `automation_navigate reLaunch /pages/activity-detail/index?id=2000`：退出状态 0；当前页为“活动详情”，截图保存到 `tmp-miniprogram-detail-enhanced.png`。
- `get_app_console_content --command "grep -i error"`：返回空字符串；未发现 error 日志。
- `get_app_network_content --command "grep -i fail"`：返回空字符串；未发现失败请求。
- 为恢复页面验证，曾将当前本地微信测试用户更新为 `campus_id=1`、`verification_status=APPROVED`；该变更仅用于本地开发库联调。

### 2026-07-12 微信小程序报名页与活动工作区增强

- 本轮继续开发清单：
  - 报名申请页：增加活动摘要、报名状态、名额进度、剩余名额、报名截止、公开地点和隐私提示。
  - 活动工作区：增加活动时间、成员地点、活动形式、状态标签和消息区提示；修复移动宽度下时间被截断的问题。
  - 本轮未新增接口、未引入依赖、未改后端，继续复用活动详情、会话、消息和 WebSocket 能力。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 3 个格式工具测试全部通过。
- `wechatide -c CodeBuddy -t check_devtools_status --skill-version 0.2.5`：退出状态 0；微信开发者工具已登录，`skillVersion=0.2.5` 与 agent skill 一致。
- `compile_wxml`：`packageBusiness/application/index.wxml`、`packageBusiness/workspace/index.wxml` 均 `success=true`。
- `compile_wxss`：`packageBusiness/application/index.wxss`、`packageBusiness/workspace/index.wxss` 均 `success=true`；修复时间截断后再次编译 `packageBusiness/workspace/index.wxss`，`success=true`。
- 页面验证：
  - `automation_navigate reLaunch /packageBusiness/application/index?activityId=2000`：退出状态 0；当前页为“申请加入”，截图保存到 `tmp-miniprogram-application-enhanced.png`。
  - 为验证工作区访问，本地开发库将当前测试微信用户加入活动 `2000` 的成员表；会话 ID 为 `4000`。
  - `automation_navigate reLaunch /packageBusiness/workspace/index?conversationId=4000`：退出状态 0；当前页为“活动工作区”，截图保存到 `tmp-miniprogram-workspace-enhanced.png`。
- `get_app_console_content --command "grep -i error"`：返回空字符串；未发现 error 日志。
- `get_app_network_content --command "grep -i fail"`：返回空字符串；未发现失败请求。

### 2026-07-12 微信小程序我的活动与申请、申请管理增强

- 本轮继续开发清单：
  - 我的活动与申请：增加总览卡、发布/草稿/招募中/待处理统计，发布与申请记录改成信息卡片。
  - 申请管理：增加待处理/已接受/已拒绝/剩余名额统计，申请列表改成更适合手机查看的卡片。
  - 本轮仍未新增接口、未引入依赖、未改后端，继续复用现有活动、申请、审核和生命周期接口。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 3 个格式工具测试全部通过。
- `wechatide -c CodeBuddy -t compile_wxml --project E:/project/test/frontend/miniprogram --file-path packageBusiness/my-activities/index.wxml`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxml --project E:/project/test/frontend/miniprogram --file-path packageBusiness/application-manage/index.wxml`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxss --project E:/project/test/frontend/miniprogram --file-path packageBusiness/my-activities/index.wxss`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxss --project E:/project/test/frontend/miniprogram --file-path packageBusiness/application-manage/index.wxss`：退出状态 0，`success=true`。
- 页面验证：
  - `automation_navigate reLaunch /packageBusiness/my-activities/index`：退出状态 0；当前页为“我的活动与申请”，截图保存到 `tmp-miniprogram-my-activities-enhanced.png`。
  - 为验证申请管理页，将本地开发库中活动 `2000` 的 `creator_id` 临时改为当前测试用户 `2076254530146693122`；并将该用户加入活动 `2000` 的成员表，便于联调。
  - `automation_navigate reLaunch /packageBusiness/application-manage/index?activityId=2000`：退出状态 0；当前页为“活动业务管理”，截图保存到 `tmp-miniprogram-application-manage-enhanced.png`。
- `get_app_console_content --command "grep -i error"`：返回空字符串；未发现 error 日志。
- `get_app_network_content --command "grep -i fail"`：返回空字符串；未发现失败请求。

### 2026-07-12 微信小程序资料编辑与收藏信誉增强

- 本轮继续开发清单：
  - 资料编辑页：增加个人名片预览、兴趣标签预览和标签数量提示，保留原有资料保存接口和安全说明。
  - 收藏信誉页：增加收藏总览卡、信誉面板总览、收到评价/平均评分/五星反馈统计，继续使用结构化评价，不生成不透明信用分。
  - 本轮未新增接口、未引入依赖、未改后端。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 3 个格式工具测试全部通过。
- `wechatide -c CodeBuddy -t compile_wxml --project E:/project/test/frontend/miniprogram --file-path packageAccount/profile-edit/index.wxml`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxml --project E:/project/test/frontend/miniprogram --file-path packageAccount/favorites-reputation/index.wxml`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxss --project E:/project/test/frontend/miniprogram --file-path packageAccount/profile-edit/index.wxss`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxss --project E:/project/test/frontend/miniprogram --file-path packageAccount/favorites-reputation/index.wxss`：退出状态 0，`success=true`。
- 运行态截图未完成：`automation_navigate` / `simulator_open_page` 对 `packageAccount/*` 分包页返回成功后，`automation_runtime_info` 仍停留在 `pages/discover/index` 或抛出微信开发者工具 automator 内部错误 `Cannot destructure property 'rawPath' of 't.getPageMetaByWebviewId(...)' as it is null.`；执行 `simulator_refresh` 后仍复现。该问题记录为开发者工具自动化状态异常，页面编译已通过但本轮未声明分包页截图验收通过。

### 2026-07-12 微信小程序安全中心与身份隐私增强

- 本轮继续开发清单：
  - 身份/隐私页：增加安全状态总览卡、身份绑定与校园认证的三栏状态卡。
  - 安全中心页：增加治理总览卡和举报提示，强化案件与黑名单的结构化展示。
  - 本轮未新增接口、未引入依赖、未改后端。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 3 个格式工具测试全部通过。
- `wechatide -c CodeBuddy -t compile_wxml --project E:/project/test/frontend/miniprogram --file-path packageAccount/security-privacy/index.wxml`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxml --project E:/project/test/frontend/miniprogram --file-path packageAccount/safety/index.wxml`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxss --project E:/project/test/frontend/miniprogram --file-path packageAccount/security-privacy/index.wxss`：退出状态 0，`success=true`。
- `wechatide -c CodeBuddy -t compile_wxss --project E:/project/test/frontend/miniprogram --file-path packageAccount/safety/index.wxss`：退出状态 0，`success=true`。
- 运行态验证结果：
  - `simulator_open_page --page packageAccount/security-privacy/index` 返回成功，但 `automation_runtime_info` 识别到当前页仍是 `pages/login/index`，疑似模拟器会话失效或自动化状态未切换到分包页。
  - `simulator_open_page --page packageAccount/safety/index` 返回成功，`automation_runtime_info` 超时；`get_app_console_content` 与 `get_app_network_content` 均无 error/fail。
  - 这两页暂不宣布截图验收通过，原因是微信开发者工具 automator 当前状态不稳定而非页面编译错误。

### 2026-07-12 微信小程序视觉重构与透明标题栏

- 本轮重构目标：
  - 将小程序从偏表单/后台式界面改为“校园搭子”专属的玻璃感、轻渐变、深色主视觉卡片风格。
  - 广场、消息、我的三个 Tab 页启用透明自定义标题栏，并为微信右上角胶囊按钮预留安全空间。
  - 发布活动页按“灵感模板、发布检查、描述、地点、时间、审核材料”拆成任务卡片，减少长表单压迫感。
  - 报名申请页改为活动通行证样式，突出活动标题、名额进度、申请前确认和问答卡片。
  - 资料编辑页改为校园名片编辑体验，增加深色名片头图和分段资料卡。
  - 本轮未新增接口、未引入依赖、未改后端。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 3 个格式工具测试全部通过。
- `wechatide -c CodeBuddy -t check_devtools_status --skill-version 0.2.5`：退出状态 0；微信开发者工具已登录，`skillVersion=0.2.5` 与 agent skill 一致。
- 单文件编译：
  - `compile_wxml`：`pages/discover/index.wxml`、`pages/messages/index.wxml`、`pages/profile/index.wxml`、`packageBusiness/activity-editor/index.wxml`、`packageBusiness/application/index.wxml`、`packageAccount/profile-edit/index.wxml` 均 `success=true`。
  - `compile_wxss`：`pages/discover/index.wxss`、`pages/messages/index.wxss`、`pages/profile/index.wxss`、`packageBusiness/activity-editor/index.wxss`、`packageBusiness/application/index.wxss`、`packageAccount/profile-edit/index.wxss` 均 `success=true`；修正发布页底部按钮遮挡后再次编译 `packageBusiness/activity-editor/index.wxss`，`success=true`。
- 运行态页面验证：
  - `automation_navigate switchTab /pages/discover/index`：退出状态 0；当前页为 `pages/discover/index`，截图保存到 `tmp-redesign-discover.png`。
  - `automation_navigate switchTab /pages/messages/index`：退出状态 0；当前页为 `pages/messages/index`，截图保存到 `tmp-redesign-messages.png`。
  - `automation_navigate switchTab /pages/profile/index`：退出状态 0；当前页为 `pages/profile/index`，截图保存到 `tmp-redesign-profile.png`。
  - `simulator_open_page --page packageBusiness/activity-editor/index`：退出状态 0；当前页为 `packageBusiness/activity-editor/index`，截图保存到 `tmp-redesign-editor.png`。
  - `simulator_open_page --page packageBusiness/application/index --query activityId=2000`：退出状态 0；当前页为 `packageBusiness/application/index?activityId=2000`，截图保存到 `tmp-redesign-application.png`。
  - `simulator_open_page --page packageAccount/profile-edit/index`：退出状态 0；当前页为 `packageAccount/profile-edit/index`，截图保存到 `tmp-redesign-profile-edit.png`。
- `get_app_console_content --command "grep -i error"`：返回空字符串；未发现 error 日志。
- `get_app_network_content --command "grep -i fail"`：返回空字符串；未发现失败请求。

### 2026-07-12 广场页信息瘦身与安全区导航修复

- 设计依据：使用 `taste-skill` 对现有页面做重构审查，并参考微信开放文档的页面配置、`wx.getWindowInfo()`、`wx.getMenuButtonBoundingClientRect()` 以及 Meetup/Eventbrite 的活动发现页信息架构。
- 导航修复：新增共享 `custom-nav` 组件，按状态栏和胶囊真实 px 坐标计算标题栏高度与右侧保留区；广场、消息、我的三个透明标题栏页面统一接入，删除固定 `170rpx` 右内边距和多行装饰文案。
- 广场瘦身：删除无推荐算法支撑的重复“推荐先看”、只统计当前页的四格指标、不可达的认证提示、动态关键词大卡、重复进度表达和深色 Hero；保留搜索、活动形式筛选、发布、单一活动流、分页和详情跳转。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译和 5 个 Node 测试全部通过，其中新增 2 个自定义导航几何与兜底测试。
- 微信开发者工具 `compile_wxml`：`components/custom-nav/index.wxml`、`pages/discover/index.wxml`、`pages/messages/index.wxml`、`pages/profile/index.wxml` 均 `success=true`。
- 微信开发者工具 `compile_wxss`：`components/custom-nav/index.wxss`、`pages/discover/index.wxss`、`pages/messages/index.wxss`、`pages/profile/index.wxss` 均 `success=true`。
- 微信开发者工具当前实际注册表不提供 skill 文档中描述的 `compile_js`，调用返回 `unknown tool: compile_js`；TS 验证由 `npm run check` 完成，本项未伪报为开发者工具单文件编译通过。
- 运行态验证设备为 iPhone 15 Pro Max 模拟器，`statusBarHeight=54`：广场、消息、我的标题均位于系统状态栏下方并避开胶囊；截图为 `tmp-redesign-discover-taste-final.png`、`tmp-redesign-messages-nav-fixed.png`、`tmp-redesign-profile-nav-fixed.png`。
- 广场真实交互：线下筛选返回活动 1 条，线上筛选进入空状态；不存在关键词搜索返回 0 条，清空后恢复 1 条；活动卡跳转 `/pages/activity-detail/index?id=2000`，发布按钮跳转 `/packageBusiness/activity-editor/index`。
- `get_app_console_content --command "grep -i error"` 与 `get_app_network_content --command "grep -i fail"` 最终均返回空字符串。
- `automation_wx_api getMenuButtonBoundingClientRect`：当前 automator 会话超时，未取得单独 API 返回值；已由 `automation_runtime_info` 的 `statusBarHeight=54`、实际运行截图和导航几何单测交叉验证，未将该 API 调试调用标记为通过。

### 2026-07-12 微信小程序全页面视觉重构与克制动效

- 设计方向：面向学生的内容优先界面，使用浅色背景和单一翠绿色强调色；删除无推荐依据的统计、重复进度、实现细节和说明性 Hero，保留任务所需信息。动效强度为 `MOTION_INTENSITY=3`，只使用 `opacity + transform` 入场和按压反馈，并通过 `prefers-reduced-motion` 降级。
- 重构范围：广场、消息、我的、活动详情、发布活动、申请加入、活动工作区、我的活动与申请、活动业务管理、登录、校园认证、编辑资料、收藏与信誉、账号与隐私、安全中心，共 15 个页面；登录页因当前已有登录态会自动跳转广场，未单独保存登录态截图，其 WXML/WXSS 已编译通过。
- 本轮收尾修复：工作区完整展示起止时间和成员地点；活动详情完整展示公开地点并按生命周期映射状态色；活动管理的状态操作和申请决策接入 `actionBusy` 忙碌锁；我的活动总数与实际活动、申请记录一致；长列表动效移到容器；资料页删除 `JSON` 存储细节；补充 `EXPIRED -> 已过期` 状态文案。
- 约束：未新增依赖、未改后端接口、权限或业务状态机；继续使用本地图标和现有 MinIO 文件能力，未引入消息队列。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 类型检查、测试编译及 5 个 Node 测试全部通过，包含导航几何、时间/状态格式与查询字符串验证。
- 微信开发者工具全量逐文件编译：17 个 WXML 全部 `success=true`，18 个 WXSS（含 `app.wxss`）全部 `success=true`；收尾修复后再次编译受影响的 6 个 WXML 和 2 个 WXSS，全部 `success=true`。
- 运行态验证：14 个可达页面均在 iPhone 15 Pro Max 模拟器成功打开并截图；业务页参数分别使用 `id=2000`、`activityId=2000`、`conversationId=4000`。主要截图为 `tmp-redesign-discover-final.png`、`tmp-redesign-messages-final.png`、`tmp-redesign-profile-final.png`、`tmp-redesign-campus-bind-final.png`、`tmp-redesign-activity-detail-final.png`、`tmp-redesign-activity-editor-final.png`、`tmp-redesign-application-final.png`、`tmp-redesign-workspace-final.png`、`tmp-redesign-my-activities-final.png`、`tmp-redesign-application-manage-final.png`、`tmp-redesign-profile-edit-final.png`、`tmp-redesign-favorites-reputation-final.png`、`tmp-redesign-security-privacy-final.png`、`tmp-redesign-safety-final.png`。
- 交互反向检查：在活动业务管理页临时将 `actionBusy` 设为 `true` 后，运行态按钮 `disabled` 属性为 `true`；随后恢复为 `false`，未触发真实状态写操作。
- 登录页行为：已登录会话执行 `reLaunch /pages/login/index` 后实际进入 `/pages/discover/index`，符合登录态重定向逻辑。
- 最终 `get_app_console_content --command "grep -i error"` 与 `get_app_network_content --command "grep -i fail"` 均返回空字符串。

### 2026-07-12 广场首屏文字清晰度修复

- 原因：首屏标题继承 `motion-enter` 的位移合成层，模拟器在部分动画帧会出现文字边缘发虚；副标题颜色对比度也偏低。
- 修复：移除首屏介绍区的位移动效，保留搜索、筛选和活动列表动效；标题使用标准 `700` 字重，副标题提高对比度。
- 验证：`compile_wxml`、`compile_wxss` 和 `npm run check` 全部通过；截图保存为 `tmp-redesign-discover-text-sharp.png`。

### 2026-07-12 微信小程序头像上传闭环

- 前端补齐头像能力：资料编辑页使用 `wx.chooseMedia` 选择图片，调用现有 `wx.uploadFile -> /api/v1/files` 并以 `businessType=AVATAR` 上传；头像限制 2 MB，保留本地预览和审核状态。
- 审核绑定：头像处于 `PENDING_SCAN` 时显示审核中；审核员通过后，资料页刷新会调用已有 `PUT /api/v1/me/profile` 绑定 `avatarFileId`，随后通过 `/files/{id}/url` 获取短期签名地址。驳回不会覆盖当前头像。
- 回显：`UserView` 增加 `avatarFileId`，个人中心和资料编辑页均支持真实头像回显，失败时回退昵称首字母；未完成校园认证的用户会得到明确提示。
- 安全与并发：后端仅允许已认证学生上传/绑定 `AVATAR`，同校其他用户只能访问当前已绑定头像；前端上传、审核刷新、资料保存互斥，上传复用一次登录态刷新，图片加载失败回退首字头像。
- 已知边界：待审核文件指针目前保存在用户设备本地；清除小程序缓存或换设备后不会自动找回未绑定头像，需要重新选择上传。后续只有在确实需要跨设备恢复时再增加服务端候选接口或字段。
- 测试：新增头像审核状态单测，`npm run check` 6/6 通过；`mvn -o -Dtest=FileProfileClosureTest test` 9/9 通过，包含未认证拒绝和未绑定头像越权反向测试。WXML/WXSS 单文件编译均成功。
- 本地运行闭环：测试账号通过头像按钮选择、上传文件 `2076296436532670465`，审核状态从 `PENDING_SCAN` 到 `APPROVED`，数据库确认 `sys_user.avatar_file_id` 已绑定且旧头像已解绑；截图为 `tmp-avatar-upload-pending.png`、`tmp-avatar-upload-approved.png`、`tmp-avatar-upload-profile.png`。本地开发库账号 `2076290662251130881` 临时标记为校园 `1/APPROVED` 仅用于联调。

### 2026-07-12 微信开发版登录身份与空响应修复

- 根因：开发模式此前直接使用每次 `wx.login()` 返回的临时 code 生成 `dev-openid-*`，每次登录都会创建新的未认证用户；认证记录为空时，Jackson `NON_NULL` 策略又省略了响应 `data`，小程序将其写入 `setData` 时产生 `binding=undefined` 告警。
- 修复：微信开发者工具 `develop` 版固定发送 `student-a`，复用 `sql/data.sql` 中已认证测试用户；`trial/release` 和运行时信息不可用时仍走真实 `wx.login()`。后端 `WECHAT_DEV_LOGIN_ENABLED` 默认改为 `false`，本地启动命令显式设为 `true`。
- 修复统一响应：仅 `ApiResponse.data` 覆盖全局 `NON_NULL`，空业务结果序列化为 `data:null`，其他 DTO 的空字段策略不变。
- `npm test`（`frontend/miniprogram`）：第一次 RED 阶段按预期失败（开发版实际发送临时 code）；修复后退出状态 0，8/8 测试通过。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 检查和 8 项 Node 测试全部通过。
- `mvn -o -Dtest=ApiResponseSerializationTest test`：RED 阶段因缺少 `data` 失败；修复后退出状态 0，1 项通过。
- `mvn -o test`（`backend`）：首次运行因 MySQL deadlock 导致 4 个级联失败；立即重跑退出状态 0，26 项测试通过、失败 0、错误 0、跳过 0。
- `mvn -o -DskipTests package`：退出状态 0，生成可执行 JAR；以 `WECHAT_DEV_LOGIN_ENABLED=true` 重启本地后端成功监听 `8080`。
- 微信开发者工具：`simulator_open_page` 登录页成功；清理缓存后两次调用登录流程，network 均发送 `code=student-a`，HTTP 200 返回用户 `id=100`、`verificationStatus=APPROVED`，页面进入 `pages/discover/index`。`get_app_console_content --command "grep -i binding"` 返回空字符串。
- 真实空数据请求：未认证测试用户调用 `/api/v1/me/identity-bindings/current` 返回 HTTP 200 且 `data:null`。
- 工具边界：当前 DevTools 注册表没有文档所列 `compile_js`，调用原样返回 `unknown tool: compile_js`；本次以 `npm run check`、`simulator_open_page` 和真实运行请求作为替代证据，未将单文件 JS 编译标记为通过。

### 2026-07-12 聊天图片授权与 WebSocket 断线补拉修复

- 安全审查发现同校审核员可通过通用 reviewer 授权读取不属于其会话的 `CHAT_IMAGE` 签名 URL；现已将聊天图片访问前置为唯一分支，必须是审核通过、已绑定真实消息且当前用户仍是活动会话成员，上传者、校园审核员和平台管理员均不能绕过。
- 新增 `FileProfileClosureTest` 反向覆盖：合法成员可读，退出成员、上传者已离开、同校审核员和平台管理员均被拒绝。
- WebSocket 重连后由工作区监听 `CONNECTED`，按当前最后消息 ID 补拉历史并按字符串 Snowflake ID 去重排序；`DISCONNECTED` 时切换 REST 发送回退，避免断线期间丢消息。
- 连接层补齐旧 task/延迟票据/连接中关闭/票据请求失败的 generation 和 promise 清理，防止旧回调覆盖新连接并确保后续重连仍可建立。
- `mvn -o test`（`backend`）：退出状态 0；30 个测试通过，失败 0、错误 0、跳过 0，包含本地管理端 CORS 预检回归。
- `npm run check`（`frontend/miniprogram`）：退出状态 0；TypeScript 检查通过，15 个 Node 测试通过（含消息合并和 WebSocket 竞态回归）。
- 微信开发者工具运行态：`packageBusiness/workspace/index?conversationId=4000` 成功打开并截图到 `tmp-workspace-reconnect-final.png`；主动调用 `wx.closeSocket()` 后重新获取 `/ws-ticket`、收到 `CONNECTED`，并请求 `/conversations/4000/messages?afterId=2076288369925582849&limit=100`；页面数据保持 `state=ready/socketReady=true`，控制台 error 与 network fail 均为空。
- `mvn -o -DskipTests package`：首次因旧的 8080 Java 进程锁定 JAR 失败；停止旧进程后重跑退出状态 0，生成最新可执行 JAR，并以本地开发环境变量重启后端监听 `8080`。

### 2026-07-12 管理后台开发 CORS 与浏览器验收

- 根因：Vite 管理端运行在 `5174`，后端默认 CORS 仅允许 `5173`，登录请求被拒绝为 `403 Invalid CORS request`；开发默认白名单现包含 `localhost/127.0.0.1` 的 `5173/5174`，生产仍需显式覆盖 `CORS_ALLOWED_ORIGINS`。
- 管理端增加最小品牌 `favicon.svg`，消除浏览器 `/favicon.ico` 404。
- `npm run build`（`frontend/admin`）：退出状态 0，`vue-tsc` 与 Vite 构建通过。
- Playwright 真实浏览器：平台管理员 `admin` 登录进入工作台和用户配置页；校园审核员 `reviewer` 登录后仅显示审核导航，直接访问 `/platform/users` 跳转 403，带审核员令牌请求 `/api/v1/admin/users` 返回 403；登录和正常页面无 console error。
- CORS 预检：`OPTIONS /api/v1/auth/admin-login` 携带 `Origin: http://127.0.0.1:5174` 返回 200，并返回对应 `Access-Control-Allow-Origin`。

## 已知外部依赖

- 缺少真实微信小程序 AppID/AppSecret。
- 缺少真实校园身份接口或测试规范。
- 正式域名、TLS 证书、备案和微信后台权限未提供。

这些依赖不阻止使用测试适配器完成本地业务开发，但对应线上能力必须标记为未验证。
