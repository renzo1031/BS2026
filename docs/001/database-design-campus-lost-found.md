# 校园失物招领中心数据库设计

## 1. 设计依据

本数据库设计承接 PRD 与系统设计，目标是支撑前台门户、个人中心、保管处理、管理后台和审计追踪。

调研结论：

- MySQL 8 支持 InnoDB、`utf8mb4`、索引、外键等常规关系型能力，适合毕业设计项目。
- MyBatis-Plus 支持逻辑删除字段，实体可通过 `@TableLogic` 映射 `deleted` 字段，避免关键业务数据被物理删除。
- OWASP Logging Cheat Sheet 建议日志能回答 when、where、who、what，因此 `sys_operation_log` 需要记录操作人、对象、动作、前后状态、IP、User-Agent、请求路径和时间。

参考链接：

- https://dev.mysql.com/doc/refman/8.0/en/create-table.html
- https://dev.mysql.com/doc/refman/8.0/en/constraint-foreign-key.html
- https://baomidou.com/en/guides/logic-delete/
- https://baomidou.com/en/reference/annotation/
- https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html

## 2. 总体取舍

### 2.1 需要做

- 13 张核心表，覆盖账号、角色、菜单、物品、图片、分类、地点、认领、线索、交接、通知、日志。
- 所有业务表保留创建人、创建时间、更新人、更新时间和逻辑删除字段。
- 关键业务表必须有状态字段。
- 管理后台查询需要的发布人、认领人、审核人、保管员、最后操作人和最后更新时间必须在主表或关联表中可查询。
- 审计日志独立成表，支持按用户、对象、动作、时间、结果查询。

### 2.2 不做

- 不做物理删除关键业务数据。
- 不做多租户、多校区复杂组织。
- 不做聊天消息表。
- 不做支付、物流、OCR 相关表。
- 不做把所有扩展信息塞进 JSON 的偷懒设计。

## 3. 表清单审核

| 表名 | 必要性 | 说明 |
|---|---|---|
| `sys_user` | 必须 | 登录、发布、认领、审计都依赖用户身份 |
| `sys_role` | 必须 | 支持普通用户、保管员、管理员 |
| `sys_user_role` | 必须 | 用户可能有多个角色，如管理员兼保管员 |
| `sys_menu` | 推荐 | 支撑后台菜单与权限标识 |
| `lf_category` | 必须 | 规范物品分类，便于筛选统计 |
| `lf_location` | 必须 | 规范校园地点，便于检索 |
| `lf_item` | 必须 | 失物/招领核心主表 |
| `lf_item_image` | 必须 | 一个物品多张图片 |
| `lf_claim_application` | 必须 | 支撑认领申请和核验 |
| `lf_clue_feedback` | 必须 | 支撑寻物线索闭环 |
| `lf_custody_handover` | 必须 | 支撑保管和交接证明 |
| `sys_notice` | 推荐 | 公告与站内通知 |
| `sys_operation_log` | 必须 | 审计链和后台追责 |

结论：13 张表都有业务必要性，没有为了凑数增加无意义表。

## 4. 关键表设计说明

### 4.1 `lf_item`

`lf_item` 是后台全量查询的中心表，必须直接保存或能关联出：

- 发布人：`publisher_id`
- 审核人：`reviewer_id`
- 当前认领人：`current_claimant_id`
- 当前保管员：`custodian_id`
- 最后操作人：`last_operator_id`
- 最后操作时间：`last_operation_time`
- 最后操作摘要：`last_operation_summary`

这样后台物品列表不用只靠日志表反查，也能满足筛选与排序。

### 4.2 `lf_claim_application`

认领申请独立成表是必要的。一个物品可能有多个认领申请，不能把认领信息只放在 `lf_item` 里，否则无法保留被驳回申请和争议记录。

### 4.3 `lf_clue_feedback`

线索反馈独立成表是必要的。寻物流程依赖多人提交线索，发布人需要确认有效或无效，不能简单用留言字段代替。

### 4.4 `lf_custody_handover`

保管交接独立成表是必要的。它记录实物保管位置、经办人、交接地点、交接时间和领取确认，是“已完成”的证据。

### 4.5 `sys_operation_log`

审计日志必须独立成表，不允许只保存在应用日志文件中。物品详情时间线可从该表按 `target_type`、`target_id` 查询聚合。

## 5. 索引设计

核心索引：

- `sys_user.username`、`sys_user.phone`、`sys_user.student_no` 唯一索引，避免账号重复。
- `lf_item.item_no` 唯一索引，便于用户按编号查询。
- `lf_item.status`、`lf_item.type`、`lf_item.category_id`、`lf_item.location_id` 普通索引，支持前台和后台筛选。
- `lf_item.publisher_id`、`current_claimant_id`、`reviewer_id`、`custodian_id` 普通索引，支持后台按责任人筛选。
- `lf_item.last_operation_time` 普通索引，支持按最后更新时间排序。
- `sys_operation_log.operator_id`、`target_type,target_id`、`action`、`created_at` 普通索引，支持审计查询。

## 6. 初始化数据

初始化数据应包含：

- 3 个角色：普通用户、物品保管员、管理员。
- 3 个测试账号：admin、staff、user。
- 常用分类：证件、电子产品、书籍文具、生活用品、衣物饰品、其他。
- 常用地点：图书馆、教学楼、食堂、宿舍区、操场、校门口、实验楼。
- 若干菜单权限。
- 1 条公告。
- 示例物品、认领、线索、交接和操作日志，方便前后端联调。

## 7. 自审记录

### 7.1 需不需要

需要。项目后续开发必须依赖稳定的表结构，否则后端实体、接口、前端字段无法统一。

### 7.2 必不必要

必要。尤其是 `lf_item`、`lf_claim_application`、`lf_clue_feedback`、`lf_custody_handover` 和 `sys_operation_log`，分别支撑三条主业务流程和审计闭环。

### 7.3 完不完整

当前表设计覆盖账号、权限、主业务、附件、分类地点、认领、线索、交接、通知和审计。后续如果增加文件上传服务，可扩展独立附件表，但本期 `lf_item_image` 足够。

### 7.4 合不合理

合理。13 张表均服务真实业务；没有加入聊天、支付、AI 识别、大屏等不必要能力。

### 7.5 缺少内容

- 后续后端实体需要与 SQL 字段逐一对齐。
- 默认账号密码哈希需要在后端登录实现后验证。
- 图片文件实际存储路径需要在后端文件上传模块落地。

### 7.6 什么没做

- 没有做多校区组织表。
- 没有做复杂部门权限表。
- 没有做物理删除日志。

### 7.7 什么不该做

- 不该用单表 `record` 承载所有业务。
- 不该把认领、线索、交接都写成备注字段。
- 不该删除或覆盖历史日志。
