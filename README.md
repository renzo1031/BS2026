<h1 align="center">BS2026 计算机毕业设计项目合集</h1>

<p align="center">
  基于 <strong>Spring Boot + Vue / 原生微信小程序</strong> 的计算机毕业设计项目合集<br>
  项目按编号独立存放，提供源码、数据库脚本和项目文档；部分项目包含效果截图，适合计算机毕设、课程设计、二次开发和技术学习参考。
  联系QQ：1852568062
</p>

<p align="center">
  <a href="#-推荐项目">推荐项目</a> ·
  <a href="#-项目列表">项目列表</a> ·
  <a href="#-快速开始">快速开始</a> ·
  <a href="#-查看说明">查看说明</a>
</p>

---

## 📌 快速导航

| 入口 | 说明 |
| --- | --- |
| 🖼️ 项目截图 | 优先看效果，快速判断项目是否合适 |
| 📚 项目文档 | 查看业务流程、数据库设计、系统设计 |
| 💻 前后端源码 | 查看完整 Spring Boot、Vue 和原生微信小程序项目代码 |
| 🗄️ 数据库脚本 | 一键初始化项目所需数据表和预置数据 |

## 📊 项目统计

| 指标 | 当前数量 |
| --- | --- |
| 项目总数 | 4 个 |
| 已完成项目 | 3 个 |
| 后端项目 | 4 个 |
| 前端项目 | 4 个 |

## 🌟 推荐项目

| 编号 | 推荐项目 | 推荐理由 | 快速查看 |
| --- | --- | --- | --- |
| 001 | 校园失物招领中心 | 🌞 前台门户 + 个人中心 + 管理后台，覆盖发布、审核、认领、核验、交接、审计完整闭环 | [截图](./docs/001/screenshots/) · [文档](./docs/001/) · [前端](./001-frontend/) · [后端](./001-backend/) |
| 002 | 大学生一体化服务平台 | 宿舍报修、在读证明、活动场地三条完整办理流程，覆盖学生、部门人员和管理员权限 | [截图](./docs/002/screenshots/) · [PRD](./docs/002/PRD-campus-integrated-service-platform.md) · [前端](./002-frontend/) · [后端](./002-backend/) |
| 003 | 留守儿童帮扶平台 | 公开官网 + 志愿者门户 + 工作人员后台，覆盖建档、审核、匹配、服务、验收、评价和审计闭环 | [说明](./docs/003/README.md) · [规格](./docs/003/project-spec.md) · [前端](./003-frontend/) · [后端](./003-backend/) |
| 004 | 校园搭子平台 | 原生微信小程序 + 管理后台，覆盖校园认证、活动发布审核、报名组队、消息协作、完成评价、举报申诉和审计闭环 | [说明](./docs/004/README.md) · [规格](./docs/004/project-spec.md) · [小程序](./004-miniproject/) · [管理端](./004-admin/) · [后端](./004-backend/) |

## 🚀 项目列表

| 编号 | 项目名称 | 类型 | 技术栈 | 状态 | 入口 |
| --- | --- | --- | --- | --- | --- |
| 001 | 校园失物招领中心 | 校园服务 / 后台管理 | Spring Boot + Vue 3 + MySQL | ✅ 已完成 | [后端](./001-backend/) · [前端](./001-frontend/) · [SQL](./sql/001/init.sql) · [文档](./docs/001/) · [截图](./docs/001/screenshots/) |
| 002 | 大学生一体化服务平台 | 校园事务 / 多角色办理 | Spring Boot + Vue 3 + MySQL | ✅ 已完成 | [后端](./002-backend/) · [前端](./002-frontend/) · [SQL](./sql/002/init.sql) · [PRD](./docs/002/PRD-campus-integrated-service-platform.md) · [截图](./docs/002/screenshots/) |
| 003 | 留守儿童帮扶平台 | 公益服务 / 多角色协作 | Spring Boot + Vue 3 + MySQL | ✅ 已完成 | [后端](./003-backend/) · [前端](./003-frontend/) · [SQL](./sql/003/init.sql) · [说明](./docs/003/README.md) · [规格](./docs/003/project-spec.md) |
| 004 | 校园搭子平台 | 校园社交 / 活动治理 | Spring Boot + 原生微信小程序 + Vue 3 + MySQL + Redis + MinIO | 🔄 开发中 | [后端](./004-backend/) · [小程序](./004-miniproject/) · [管理端](./004-admin/) · [SQL](./sql/004/) · [说明](./docs/004/README.md) |

## ⚡ 快速开始

以 `003` 项目为例：

```powershell
# 1. 初始化数据库
cmd /c "mysql -uroot -proot < sql\003\init.sql"

# 2. 设置后端开发环境变量
$env:DB_PASSWORD = 'root'
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
$env:CORS_ALLOWED_ORIGINS = 'http://localhost:5173'

# 3. 启动后端
Set-Location ".\003-backend"
mvn spring-boot:run

# 4. 另开终端启动前端
Set-Location ".\003-frontend"
npm install
npm run dev
```

常用地址：

```text
前端：http://localhost:5173/
后端：http://localhost:8080
```

## 👀 查看说明

- 想先看效果：进入项目的 `docs/编号/screenshots/`（如有）
- 想看项目说明：进入 `docs/编号/`
- 想看源码：进入 `编号-backend/` 和 `编号-frontend/`；独立管理端位于 `编号-admin/`
- 想导入数据库：优先执行 `sql/编号/init.sql`；未提供统一入口时按项目说明依次执行结构和数据脚本

## 🛠️ 本地环境

- JDK 17+
- Maven 3.8+
- Node.js 20.19+
- MySQL 8+

## 💬 购买前常见问题

**Q: 项目是否可以直接运行？**  
A: 项目整理时会尽量完成基础运行验证，并提供源码、数据库脚本和说明文档。由于不同电脑的 JDK、MySQL、Node.js 版本可能不同，购买后可协助排查启动问题和本地环境配置。

**Q: 是否支持功能定制？**  
A: 支持。购买项目后可以继续定制页面、业务流程、数据库字段或新增功能模块，也可以按学校要求调整题目方向。

**Q: 是否提供论文 / PPT 支持？**  
A: 可以提供论文思路、系统功能说明、答辩 PPT 内容建议和答辩流程梳理，让项目更适合现场答辩展示。

**Q: 不确定哪个项目适合自己怎么办？**  
A: 可以先说明专业方向、学校要求和想做的题目类型，再推荐更合适的项目或定制方案。

## 📞 购买与技术支持

想购买项目源码、需要部署指导、功能定制或答辩材料协助，可以联系：

- **QQ：1852568062**

可提供服务：

- ✅ 项目源码获取
- ✅ 本地部署与运行指导
- ✅ 功能定制与页面修改
- ✅ 数据库和接口问题排查
- ✅ 答辩 PPT 内容协助
- ✅ 论文思路与系统说明建议

> 选题、部署、定制、答辩展示都可以一起沟通，适合希望快速完成项目落地的同学。

> 每个项目都尽量按真实业务系统设计。

最后更新：2026 年 7 月 13 日
