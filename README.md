# 校园失物招领与二手物品互助平台

面向校园场景的失物招领、二手物品发布与交易平台。当前版本 **v1.0.0**，后续持续迭代。

## 功能

- 游客浏览、搜索、类型/地点筛选和详情
- 注册、登录、JWT 会话、退出和个人资料
- 失物、招领、二手物品发布、审核、下架、删除
- 留言与回复，作者/管理员删除
- 二手交易订单创建、支付状态和订单查询
- 管理员用户、物品、审核和交易数据管理
- 图片上传与访问

## 技术栈

Vue 3 + Vite + Axios；Java 17 + Spring Boot 3.3 + MyBatis-Plus；MySQL 8；JWT。

## 快速启动

1. 创建 MySQL 数据库并执行 `backend/src/main/resources/schema.sql`；已有数据库按顺序执行 `backend/src/main/resources/migrations/`。
2. 设置 `DB_PASSWORD`，或修改 `backend/src/main/resources/application.yml`。
3. 启动后端：`cd backend && mvn spring-boot:run`（8080）。
4. 启动前端：`cd frontend && npm install && npm run dev`（5173）。

演示管理员：`admin / admin123`。生产环境请立即修改并配置安全凭据。

## 项目结构

```text
backend/   Spring Boot API 与数据库脚本
frontend/  Vue 3 页面与组件
docs/      API、开发部署、路线图
.github/   CI 配置
```

## 文档

- [开发接口文档](docs/API.md)
- [开发与部署指南](docs/DEVELOPMENT.md)
- [v1 路线图与限制](docs/ROADMAP.md)

## 验证

后端：`cd backend && mvn test`；前端：`cd frontend && npm run build`。浏览器回归脚本位于 `frontend/scripts/browser-smoke.cjs`。

## 许可证

当前未指定开源许可证，默认保留所有权利。
