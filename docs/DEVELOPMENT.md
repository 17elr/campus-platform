# 开发与部署指南

## 准备与启动

需要 JDK 17、Maven、Node.js 18+、npm、MySQL 8 和 Redis。RabbitMQ 为可选组件。默认端口：前端 5173、主服务 8080、公告 8081、MySQL 3306、Redis 6379、RabbitMQ 5672。

全新数据库在 MySQL 客户端执行 backend/src/main/resources/schema.sql。脚本包含建库及演示数据；含 ALTER TABLE，不能视为可重复执行的迁移工具。已有数据库先备份，再根据实际已应用版本逐个执行 migrations 下未执行的脚本；不要在新 schema 上重新执行所有迁移。011 增加配送员申请表与纠纷字段。

分别在两个终端配置 DB_PASSWORD，先启动 notice-service，再启动 backend，最后在 frontend 中执行 npm ci 和 npm run dev。完整命令见 README。

## 配置表

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| DB_PASSWORD | 123456 | 两个 Java 服务的数据库密码 |
| SPRING_DATASOURCE_URL | application.yml 中的本机 JDBC URL | 覆盖数据库地址，两个服务分别设置 |
| SPRING_DATASOURCE_USERNAME | root | 覆盖数据库用户 |
| NOTICE_SERVICE_URL | http://localhost:8081 | 主服务调用公告的地址 |
| NOTICE_SERVICE_PORT | 8081 | 公告服务监听端口 |
| REDIS_HOST / REDIS_PORT | localhost / 6379 | Redis 连接 |
| RABBITMQ_HOST / RABBITMQ_PORT | localhost / 5672 | RabbitMQ 连接 |
| RABBITMQ_USERNAME / RABBITMQ_PASSWORD | guest / guest | MQ 用户，仅用于本机开发 |
| CAMPUS_MQ_ENABLED | false | 开启配送事件发布和日志消费 |
| VITE_API_BASE_URL | http://127.0.0.1:8080/api | 前端 API 根地址，包含 /api |

Spring 标准属性也可由环境变量覆盖，例如 SERVER_PORT、SPRING_DATA_REDIS_PASSWORD。当前 JWT 签名密钥由代码在内存中生成，没有可用的 JWT_SECRET 配置项。Vite 环境变量在构建时写入静态资源；项目不会自动读取 Java 服务的 .env 文件。

```powershell
$env:CAMPUS_MQ_ENABLED = "true"
$env:DB_PASSWORD = "你的密码"
mvn -f backend/pom.xml spring-boot:run
```

## 构建与验证

```powershell
mvn -f backend/pom.xml clean package
mvn -f notice-service/pom.xml clean package
cd frontend
npm ci
npm run build
```

生成 backend/target 与 notice-service/target 中的 jar，以及 frontend/dist。当前没有 Java 单元测试目录，Maven test 主要验证编译和生命周期执行，不能替代真实数据库验收。

| 检查脚本 | 前提与覆盖范围 |
| --- | --- |
| frontend/scripts/browser-smoke.cjs | Playwright、Edge、前端服务；拦截接口验证浏览器交互 |
| frontend/scripts/promotion-browser.cjs | 查看脚本的本机浏览器与数据要求，再执行推广交互检查 |
| backend/scripts/promotion-smoke.py | 已运行的后端、数据库和演示账号；会写入测试业务数据 |
| scripts/infrastructure-smoke.ps1 | 主服务、公告服务、Redis、redis-cli；检查公告调用及缓存 TTL |
| delivery-acceptance.cjs | 本机 Playwright 绝对路径、演示账号和服务；会修改业务数据，移植前需调整路径 |

基础设施检查：在仓库根目录执行 `powershell -File scripts/infrastructure-smoke.ps1`。请在测试数据库运行会写入数据的脚本。

## 部署

1. 备份并升级数据库，准备 Redis；启用异步日志时准备 RabbitMQ。
2. 使用独立低权限数据库/MQ 账号，在进程环境中配置凭据。
3. 分别通过 java -jar 启动两个 jar。将公告服务和中间件限制在内部网络。
4. 静态站点部署 frontend/dist；构建前设置 VITE_API_BASE_URL=/api，由反向代理把 /api 转发给主服务。
5. 如果前端跨域部署，调整 CorsConfig；当前只允许 localhost:5173 与 127.0.0.1:5173。Vite 当前没有配置代理。
6. 固定主服务工作目录，持久化并备份 uploads 和 MySQL 数据。
7. 部署后验证公告、登录、物品、配送和管理员接口；开启 HTTPS。

## 排错

- 公告失败：检查 Redis、8081 公告服务、NOTICE_SERVICE_URL 和 notice 表。
- 页面跨域：核对前端域名和 CorsConfig；修改端口后也需匹配。
- SQL 缺字段：核对已执行迁移，勿反复运行初始化脚本。
- 重启后登录失效：当前内存 JWT 密钥的预期行为。
- 没有配送日志：检查 CAMPUS_MQ_ENABLED、RabbitMQ 连接及 campus.operation-log 队列；日志失败不代表订单写入失败。
