# 校园失物招领中心系统设计

## 1. 设计定位

本文档承接 `docs/PRD-campus-lost-found.md`，用于指导真实开发落地。PRD 说明做什么和为什么做，本文档说明怎么做、按什么结构做、哪些规则必须在后端和前端同时体现。

## 2. 调研结论与技术取舍

### 2.1 调研来源

- Spring Boot 官方文档确认：REST Controller 可结合 Bean Validation 做请求参数校验，复杂异常可用 `@ControllerAdvice` 和 `@ExceptionHandler` 统一处理。
- MyBatis-Plus 官方文档确认：`BaseMapper`、`IService` 和分页插件能减少基础 CRUD 与分页样板代码；分页插件需要注册 `MybatisPlusInterceptor` 和 `PaginationInnerInterceptor`。
- Vue 3 官方文档确认：Vue 3 推荐使用 Composition API 与 `<script setup>` 编写组件，状态管理可通过响应式机制或 Pinia 组织。
- OWASP Logging Cheat Sheet 强调日志要支持回答 when、where、who、what，本项目审计日志必须能查询和展示，不只是写入文件。

### 2.2 技术取舍

- 后端选择 Spring Boot + MyBatis-Plus + MySQL。理由：毕业设计需要业务表多、分页多、后台列表多，MyBatis-Plus 能减少重复 Mapper 和分页代码。
- 鉴权选择 JWT + BCrypt。理由：前后端分离场景清晰，便于前端存储 token 并调用接口；BCrypt 满足密码安全哈希要求。
- 前端选择 Vue 3 + Vite + Vue Router + Pinia + Axios + Element Plus。理由：适合快速构建前台门户、个人中心和后台管理页面。
- 删除策略选择逻辑删除、下架、归档，不做关键业务数据物理删除。理由：失物招领涉及争议追溯，必须保留历史。
- 日志策略选择数据库操作日志 + 物品详情时间线。理由：管理员要能查谁发布、谁审核、谁认领、谁处理、最后谁改过。

## 3. 系统总架构

```text
frontend/
  Vue 3 + Vite + Element Plus
  -> Axios 请求
backend/
  Spring Boot REST API
  -> JWT 鉴权
  -> Service 业务规则
  -> MyBatis-Plus Mapper
mysql/
  业务表 + 审计日志表
```

核心原则：

- 前端负责展示和交互，不能作为权限最终判断。
- Controller 负责请求入口和参数校验。
- Service 负责业务规则、状态流转、权限边界和日志写入。
- Mapper 负责数据访问，不写业务决策。
- 操作日志由后端统一记录，前端不允许伪造日志字段。

## 4. 后端模块划分

```text
com.campus.lostfound
  common/              通用返回、异常、分页、常量
  config/              Web、MyBatis-Plus、JWT、跨域配置
  security/            登录用户上下文、JWT 工具、鉴权拦截器
  system/
    user/              用户账号、角色、个人资料
    menu/              菜单和权限标识
    notice/            公告与站内通知
    log/               操作日志
  lostfound/
    item/              失物/招领主业务
    category/          分类
    location/          校园地点
    claim/             认领申请
    clue/              线索反馈
    custody/           保管交接
  dashboard/           后台统计
```

### 4.1 通用层

必须提供：

- `Result<T>`：统一返回结构，包含 `code`、`message`、`data`。
- `PageResult<T>`：分页返回结构，包含 `records`、`total`、`pageNum`、`pageSize`。
- `BizException`：业务异常。
- `GlobalExceptionHandler`：统一处理参数校验、鉴权失败、业务异常、系统异常。
- `BaseEntity`：`createdBy`、`createdAt`、`updatedBy`、`updatedAt`、`deleted`。

必要性：没有统一返回和异常处理，前端会出现多种错误格式，联调成本高。

### 4.2 安全层

必须提供：

- 注册：用户名、手机号或学号/工号唯一。
- 登录：校验密码哈希，返回 JWT。
- 当前用户：从 JWT 解析用户 ID 和角色。
- 权限注解或工具方法：区分普通用户、保管员、管理员。
- 数据权限方法：判断是否本人数据、是否后台角色、是否可处理当前状态。

不做：

- 不做 OAuth2、单点登录、短信验证码。
- 不做复杂数据权限表达式引擎。

## 5. 角色与权限设计

### 5.1 角色编码

| 角色 | 编码 | 说明 |
|---|---|---|
| 普通用户 | `USER` | 发布、认领、线索、个人中心 |
| 物品保管员 | `STAFF` | 认领核验、保管、交接 |
| 管理员 | `ADMIN` | 审核、全量管理、日志审计、系统配置 |

### 5.2 权限落点

| 操作 | 前端控制 | 后端控制 | 日志 |
|---|---|---|---|
| 发布物品 | 登录后显示发布入口 | `USER/STAFF/ADMIN` 均可发布，记录发布人 | 创建日志 |
| 编辑物品 | 本人草稿/驳回记录显示编辑 | Service 校验本人和状态 | 修改日志 |
| 审核物品 | 管理员后台显示审核按钮 | 仅 `ADMIN`，且状态为待审核 | 审核日志 |
| 提交认领 | 已上架物品显示按钮 | 登录用户，不能认领本人发布的拾物 | 申请日志 |
| 核验认领 | 保管员/管理员显示核验按钮 | `STAFF/ADMIN`，且申请状态待审核 | 核验日志 |
| 交接完成 | 保管员/管理员显示交接按钮 | `STAFF/ADMIN`，且状态待交接 | 交接日志 |
| 下架归档 | 管理员后台显示 | 仅 `ADMIN`，必须填写原因 | 下架/归档日志 |
| 查看全量日志 | 管理员菜单 | 仅 `ADMIN` | 查询日志可不记录 |

## 6. 状态机设计

### 6.1 物品类型

| 类型 | 编码 | 说明 |
|---|---|---|
| 招领 | `FOUND` | 用户拾到物品并发布 |
| 寻物 | `LOST` | 用户遗失物品并发布 |

### 6.2 物品状态

| 状态 | 编码 | 可见性 | 允许操作 |
|---|---|---|---|
| 草稿 | `DRAFT` | 仅发布人 | 编辑、删除、提交 |
| 待审核 | `PENDING_REVIEW` | 发布人、管理员 | 撤回、审核通过、审核驳回 |
| 已驳回 | `REJECTED` | 发布人、管理员 | 编辑后重新提交 |
| 已上架 | `PUBLISHED` | 前台公开 | 认领、线索、下架 |
| 待认领核验 | `CLAIM_REVIEWING` | 申请人、发布人、保管员、管理员 | 核验通过、核验驳回 |
| 待交接 | `HANDOVER_PENDING` | 申请人、保管员、管理员 | 交接完成 |
| 已完成 | `COMPLETED` | 相关人、后台 | 归档 |
| 已下架 | `OFFLINE` | 后台 | 重新上架或归档 |
| 已归档 | `ARCHIVED` | 后台 | 只读 |

### 6.3 状态流转规则

```text
DRAFT -> PENDING_REVIEW -> PUBLISHED -> CLAIM_REVIEWING -> HANDOVER_PENDING -> COMPLETED -> ARCHIVED
                         -> REJECTED
                         -> OFFLINE -> ARCHIVED
```

规则：

- 普通用户不能直接把物品改为已上架。
- 管理员驳回、下架、归档必须填写原因。
- 已完成和已归档记录不能被普通用户编辑。
- 状态流转必须记录操作前状态、操作后状态、操作人和原因。

## 7. 数据库设计概要

字段类型以 MySQL 8 为准。所有表使用 `utf8mb4`，主键使用 `BIGINT`，时间使用 `DATETIME`。

### 7.1 表清单

| 表名 | 用途 | 是否必要 |
|---|---|---|
| `sys_user` | 用户账号 | 必须 |
| `sys_role` | 角色 | 必须 |
| `sys_user_role` | 用户角色关系 | 必须 |
| `sys_menu` | 菜单权限 | 推荐，后台权限可扩展 |
| `lf_item` | 失物/招领主表 | 必须 |
| `lf_item_image` | 物品图片 | 必须 |
| `lf_category` | 分类 | 必须 |
| `lf_location` | 校园地点 | 必须 |
| `lf_claim_application` | 认领申请 | 必须 |
| `lf_clue_feedback` | 线索反馈 | 必须 |
| `lf_custody_handover` | 保管交接 | 必须 |
| `sys_notice` | 公告/站内通知 | 推荐 |
| `sys_operation_log` | 审计日志 | 必须 |

### 7.2 `lf_item` 关键字段

| 字段 | 说明 |
|---|---|
| `id` | 主键 |
| `item_no` | 物品编号 |
| `type` | `LOST` 或 `FOUND` |
| `title` | 标题 |
| `category_id` | 分类 |
| `location_id` | 地点 |
| `event_time` | 遗失/拾到时间 |
| `description` | 描述 |
| `contact_name` | 联系人 |
| `contact_phone` | 联系电话 |
| `status` | 物品状态 |
| `publisher_id` | 发布人 |
| `reviewer_id` | 审核人 |
| `review_time` | 审核时间 |
| `review_reason` | 审核意见 |
| `current_claimant_id` | 当前认领人 |
| `custodian_id` | 当前保管员 |
| `custody_location` | 保管位置 |
| `last_operator_id` | 最后操作人 |
| `last_operation_summary` | 最后操作摘要 |
| `last_operation_time` | 最后操作时间 |

### 7.3 `sys_operation_log` 关键字段

| 字段 | 说明 |
|---|---|
| `id` | 主键 |
| `operator_id` | 操作人 |
| `operator_name` | 操作人姓名快照 |
| `operator_role` | 操作角色快照 |
| `target_type` | 对象类型 |
| `target_id` | 对象 ID |
| `action` | 操作动作 |
| `before_status` | 操作前状态 |
| `after_status` | 操作后状态 |
| `result` | 成功/失败 |
| `reason` | 原因 |
| `request_ip` | 请求 IP |
| `user_agent` | User-Agent |
| `request_path` | 请求路径 |
| `created_at` | 操作时间 |

必要性：后台追责必须能回答谁、对什么、做了什么、前后状态是什么、为什么做、什么时候做。

## 8. 接口设计概要

### 8.1 接口返回规范

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页返回：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 8.2 后台全量物品列表

`GET /api/admin/items`

查询参数：

| 参数 | 说明 |
|---|---|
| `keyword` | 标题、编号、描述 |
| `type` | `LOST` / `FOUND` |
| `status` | 状态 |
| `categoryId` | 分类 |
| `locationId` | 地点 |
| `publisherKeyword` | 发布人姓名/账号/手机号 |
| `claimantKeyword` | 认领人姓名/账号/手机号 |
| `reviewerKeyword` | 审核人姓名/账号 |
| `updatedStart` | 更新时间开始 |
| `updatedEnd` | 更新时间结束 |
| `pageNum` | 页码 |
| `pageSize` | 每页数量 |

返回字段必须包含：

- 物品编号、标题、类型、分类、地点、状态。
- 发布人姓名、账号、手机号。
- 当前认领人姓名、账号、手机号。
- 审核人姓名、审核时间、审核结果。
- 保管员姓名。
- 创建时间、最后更新时间、最后操作人、最后操作摘要。

### 8.3 物品详情

`GET /api/admin/items/{id}`

返回聚合信息：

- `item`：基础信息。
- `images`：图片。
- `publisher`：发布人信息。
- `review`：审核信息。
- `claims`：认领申请。
- `clues`：线索反馈。
- `handover`：保管交接。
- `timeline`：状态时间线，从 `sys_operation_log` 聚合。

## 9. 前端结构设计

```text
frontend/src
  api/                 Axios API 封装
  assets/              静态资源
  components/          通用组件
  layouts/
    PublicLayout.vue   前台布局
    UserLayout.vue     个人中心布局
    AdminLayout.vue    后台布局
  router/              路由与权限守卫
  stores/              Pinia store
  views/
    public/            首页、列表、详情
    auth/              登录、注册
    user/              个人中心、我的发布、我的认领、我的线索
    staff/             认领核验、保管交接
    admin/             审核、全量管理、日志、用户、分类、地点、公告
```

### 9.1 路由权限

- 未登录可访问：首页、物品列表、物品详情、登录、注册。
- 登录用户可访问：发布、认领申请、线索反馈、个人中心。
- `STAFF` 可访问：保管交接、认领核验。
- `ADMIN` 可访问：后台所有菜单和日志。

### 9.2 页面设计重点

- 后台物品列表以业务追踪为主，不做花哨大屏。
- 详情页必须展示业务时间线，不能只展示基础信息。
- 权限按钮按角色和状态显示，但所有限制必须由后端再次校验。

## 10. 审计链设计

### 10.1 写日志的操作

必须写日志：

- 注册、登录失败、禁用账号。
- 创建物品、提交审核、撤回、修改。
- 审核通过、审核驳回。
- 提交认领、核验通过、核验驳回。
- 提交线索、确认线索有效/无效。
- 更新保管位置、交接完成。
- 下架、归档、状态纠偏。

### 10.2 时间线展示

物品详情时间线按时间倒序或正序展示：

```text
2026-07-04 10:00 张三 创建招领信息
2026-07-04 10:05 张三 提交审核 DRAFT -> PENDING_REVIEW
2026-07-04 10:20 管理员 审核通过 PENDING_REVIEW -> PUBLISHED
2026-07-04 11:00 李四 提交认领申请
2026-07-04 11:30 保管员 核验通过 CLAIM_REVIEWING -> HANDOVER_PENDING
2026-07-04 15:00 保管员 完成交接 HANDOVER_PENDING -> COMPLETED
```

## 11. 开发顺序

1. 数据库 SQL 与初始化数据。
2. 后端基础工程、统一返回、异常处理、MyBatis-Plus 配置。
3. 用户注册登录、JWT、角色权限。
4. 分类、地点、公告基础数据。
5. 物品发布、图片、审核、前台列表详情。
6. 认领申请、线索反馈。
7. 保管交接和状态机。
8. 操作日志和时间线。
9. 后台全量管理、筛选、统计。
10. Vue3 前台、个人中心、后台页面。
11. 联调主流程和权限反例。

## 12. 测试与验收设计

### 12.1 后端验证

- 注册后数据库密码不是明文。
- 未登录访问个人中心接口返回 401。
- 普通用户访问后台接口返回 403。
- 本人可编辑草稿，不能编辑已完成记录。
- 管理员审核待审核物品成功，审核已完成物品失败。
- 认领申请不能跳过核验直接交接。
- 每次状态变化都写入 `sys_operation_log`。

### 12.2 前端验证

- 未登录用户能浏览已上架物品。
- 登录用户能进入个人中心。
- 普通用户看不到后台菜单。
- 保管员能看到认领核验和交接菜单。
- 管理员能看到后台全量物品、日志、用户、分类、地点。
- 后台物品列表显示发布人、认领人、审核人、最后更新时间和最后操作人。

## 13. 自审记录

### 13.1 需不需要

需要。PRD 已明确业务范围，但开发前还缺少模块边界、状态规则、字段级后台要求和权限落点。没有系统设计会导致后端、前端、数据库各自理解不一致。

### 13.2 必不必要

必要。尤其是状态机、后台全量物品列表、审计日志字段和前端路由权限，是本项目“真实业务闭环”的关键。

### 13.3 完不完整

当前设计覆盖后端模块、数据库核心字段、接口返回规范、后台关键接口、前端结构、权限、状态、审计和测试。详细 SQL、完整接口 DTO 和页面组件将在后续开发中落地。

### 13.4 合不合理

合理。页面和接口数量超过 AGENTS 参考线，但原因是前台、个人中心、保管处理和后台审计都有独立业务任务，不是为了凑数。

### 13.5 缺少内容

- 具体 SQL 建表语句。
- 初始化测试账号和测试数据。
- 完整 DTO 字段。
- 前端视觉主题细节。

这些内容将在后续 SQL、后端、前端阶段补齐。

### 13.6 什么没做

- 没有引入微信、小程序、短信、OCR、地图定位。
- 没有做多校区复杂组织。
- 没有做大屏和营销首页。

### 13.7 什么不该做

- 不该把管理员后台做成只有统计卡片。
- 不该让普通用户看到他人敏感联系方式。
- 不该物理删除关键业务数据和日志。
- 不该让前端隐藏按钮替代后端权限校验。
