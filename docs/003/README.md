# 留守儿童帮扶平台

面向社区、学校、社会工作机构和志愿者的儿童关爱协作平台。系统由公开官网、志愿者门户和工作人员管理后台组成，围绕“建档审核、需求审核、志愿匹配、服务执行、验收评价、归档审计”形成完整业务闭环。

> 当前仓库提供完整系统源码和初始化数据。预置数据均为虚构数据，不得录入真实儿童隐私信息。

## 核心能力

- 公开官网、脱敏帮扶需求大厅和志愿者注册。
- JWT + 数据库会话认证、退出失效、账号停用和登录失败锁定。
- RBAC 权限、本人/部门/全局数据范围和后端状态权限校验。
- 儿童档案创建、提交、审核和归档，敏感字段使用 AES-256-GCM 加密。
- 帮扶需求创建、审核、公开、取消和状态同步。
- 志愿者认证、申请、唯一匹配和服务任务生成。
- 服务开始、回访、完成申请、主管验收、评价结案。
- 用户、部门、角色权限、审计日志和业务统计。

## 技术栈

- 后端：Java 17、Spring Boot 3.5.9、Spring Security、MyBatis 3.0.4、MySQL 8、Maven。
- 前端：Vue 3.5.39、TypeScript 5.9.3、Vite 8.1.4、Ant Design Vue 4.2.6、Pinia、Vue Router、Axios。
- 安全：BCrypt、JWT、数据库会话、AES-256-GCM、CORS 白名单、审计日志。

## 项目结构

```text
003-backend/                  Spring Boot 后端
003-frontend/                 Vue 3 前端
sql/003/init.sql              数据库初始化入口
sql/003/schema.sql            数据库结构
sql/003/data.sql              角色、权限和默认账号
docs/003/project-spec.md      项目规格事实来源
docs/003/development-plan.md  开发与验证记录
```

## 环境要求

- JDK 17 或更高版本
- Maven 3.9 或更高版本
- Node.js 20.19+、22.12+ 或 24+
- MySQL 8.0

以下命令以 PowerShell 7 为例，并从项目根目录执行。

## 初始化数据库

本机开发默认 MySQL 账号和密码均为 `root`。该凭据仅限开发环境，不得用于预发布或生产环境。

```powershell
cmd /c "mysql --host=localhost --port=3306 --user=root --password=root --default-character-set=utf8mb4 < sql\003\init.sql"
```

脚本会创建 `left_behind_aid` 数据库。`schema.sql` 使用 `IF NOT EXISTS`，`data.sql` 使用 upsert/ignore，可重复执行且不会重复生成基础账号和角色权限关系。

## 启动后端

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'root'
$env:JWT_SECRET = 'replace-with-a-local-secret-at-least-32-bytes'
Set-Location .\003-backend
mvn spring-boot:run
```

后端地址：`http://localhost:8080`  
健康检查：`http://localhost:8080/actuator/health`

常用环境变量：

| 变量 | 开发默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `left_behind_aid` | 数据库名 |
| `DB_USERNAME` | `root` | 数据库账号 |
| `DB_PASSWORD` | 无 | 必须在运行环境注入 |
| `JWT_SECRET` | 仅开发占位值 | 生产必须使用独立强密钥 |
| `PII_ENCRYPTION_KEY` | 仅开发占位值 | Base64 编码的 32 字节密钥，生产必须替换 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 逗号分隔的可信来源 |

## 启动前端

新开一个 PowerShell 7 终端：

```powershell
Set-Location .\003-frontend
npm install
npm run dev
```

访问地址：`http://localhost:5173`

Vite 会将 `/api` 请求代理到 `http://127.0.0.1:8080`。

## 默认账号

所有基础账号的初始密码均为 `123456`。

| 账号 | 角色 | 数据范围 |
| --- | --- | --- |
| `admin` | 系统管理员 | 全局 |
| `supervisor` | 部门主管 | 本部门 |
| `worker` | 个案人员 | 本部门 |
| `volunteer` | 志愿者 | 本人 |

## 构建与验证

```powershell
Set-Location .\003-backend
mvn test
mvn package -DskipTests

Set-Location ..\003-frontend
npm run build
```

完整的数据库、HTTP 主流程和浏览器验收证据见 [development-plan.md](development-plan.md)。

## 生产配置要求

- 使用独立数据库账号和强密码，通过环境变量或密钥管理服务注入。
- 替换 `JWT_SECRET` 和 `PII_ENCRYPTION_KEY`，禁止复用开发占位值。
- 将 `CORS_ALLOWED_ORIGINS` 限制为正式前端域名。
- 修改或停用所有默认账号，不向生产数据库导入 `sql/003/data.sql`。
- 在 HTTPS 反向代理后部署，并按实际环境接入日志留存、备份和监控。
