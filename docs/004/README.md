# 校园搭子平台

基于原生微信小程序、Vue 3 管理后台和 Spring Boot 的校园搭子平台。

当前状态：开发中。已确认范围见 [project-spec.md](project-spec.md)，阶段与验证证据见 [development-plan.md](development-plan.md)。

## 目录

```text
004-backend/           Spring Boot 后端
004-admin/             Vue 3 管理后台
004-miniproject/       原生微信小程序
sql/004/               建表、初始化数据与增量脚本
docs/004/              项目规格、开发计划和设计验收记录
```

## 本地启动

本项目不提供容器化部署脚本。先准备本机 MySQL、Redis 和 MinIO，并按 `004-backend/env.example` 配置后端环境变量。

首次启动后初始化数据库：

```powershell
cmd /c "mysql -uroot -proot -e \"CREATE DATABASE IF NOT EXISTS campus_buddy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci\""
cmd /c "mysql -uroot -proot campus_buddy < sql\004\schema.sql"
cmd /c "mysql -uroot -proot campus_buddy < sql\004\data.sql"
```

后端本地运行：

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/campus_buddy?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='root'
$env:REDIS_PORT='6379'
$env:JWT_SECRET='local-dev-jwt-secret-at-least-thirty-two-bytes'
$env:IDENTITY_HMAC_SECRET='local-dev-identity-hmac-secret-32-bytes'
$env:IDENTITY_ENCRYPTION_SECRET='local-dev-encryption-secret-32-bytes'
$env:WECHAT_DEV_LOGIN_ENABLED='true'
Set-Location .\004-backend
mvn spring-boot:run
```

管理后台：

```powershell
Set-Location .\004-admin
npm ci
npm run dev
```

开发环境管理后台地址：`http://127.0.0.1:5174`。后端本地默认 CORS 已允许 `localhost/127.0.0.1` 的 5173、5174 端口；生产环境必须通过 `CORS_ALLOWED_ORIGINS` 覆盖为正式白名单。

微信小程序项目路径：`004-miniproject/project.config.json`。已在微信开发者工具中完成登录态检查，并验证核心页面的 WXML/WXSS 编译；真实 AppID、线上域名和微信审核能力仍未配置。

## 默认测试账号

- 平台管理员：`admin / Admin123!`
- 校园审核员：`reviewer / Reviewer123!`
- 微信开发者工具 `develop` 版固定使用 `student-a`，重复登录会复用已认证测试账号；`student-b` 等其他测试 code 仍可用于接口测试。
- `trial` / `release` 版使用真实 `wx.login`，后端必须配置 `WECHAT_APP_ID`、`WECHAT_APP_SECRET` 并保持 `WECHAT_DEV_LOGIN_ENABLED=false`。

## 已验证命令

- `mvn -o test`（`004-backend`）：33 个后端测试通过。
- `npm run lint`、`npm run typecheck`、`npm test -- --run`、`npm run build`（`004-admin`）：管理后台检查、1 个组件测试和生产构建通过。
- `npm run check`（`004-miniproject`）：TypeScript 检查、测试编译和 17 个 Node 测试通过。
- `wechatide -c Codex -t check_devtools_status --skill-version 0.2.5`：开发者工具已登录，skill 版本一致；头像资料页 WXML/WXSS 单文件编译通过。
- 管理端浏览器验收：平台管理员登录、用户配置页、校园审核员登录、平台配置路由 403 反向权限和 `http://127.0.0.1:5174` CORS 预检均通过。
