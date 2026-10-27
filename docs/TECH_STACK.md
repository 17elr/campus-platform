# 技术栈与架构

本文按当前源码和依赖清单编写。前端版本为 package.json 声明范围，实际安装由 package-lock.json 锁定；后端未指定版本的依赖由 Spring Boot / Spring Cloud BOM 管理。MySQL、Redis、RabbitMQ 的服务器版本没有在项目中锁定。

## 前端

| 技术 | 版本来源 | 用途与边界 |
| --- | --- | --- |
| Vue | ^3.5.13 | 单文件组件、Composition API、响应式状态 |
| Vite | ^5.4.11 | 开发服务器与生产打包 |
| @vitejs/plugin-vue | ^5.2.1 | Vue 单文件组件编译 |
| Axios | ^1.7.9 | API 请求、Bearer 请求头 |
| JavaScript / CSS | 浏览器原生 | 业务逻辑、样式与布局 |
| Fetch / localStorage | 浏览器原生 | 部分接口调用、登录信息存储 |
| vue-router | ^4.5.0 | 已声明依赖；当前 main.js 未注册路由，页面切换由 App.vue 管理 |

没有引入 TypeScript、Pinia 或组件 UI 框架。浏览器脚本使用 Playwright，但它没有列入前端项目依赖，需要单独准备。

## Java 服务

| 技术 | 版本 | 作用 |
| --- | --- | --- |
| Java | 17 | 后端语言与运行时 |
| Spring Boot | 3.3.13 | 主服务和公告服务启动、配置及 Web 容器 |
| Spring MVC / Jackson | Boot 管理 | JSON REST 接口、序列化 |
| Jakarta Validation | Boot 管理 | DTO 参数校验 |
| MyBatis-Plus | 3.5.7 | 用户、物品、留言、交易实体 CRUD |
| Spring JDBC / JdbcTemplate | Boot 管理 | 配送、推广、广告、运营配置、公告 SQL |
| MySQL Connector/J | Boot 管理 | JDBC 驱动 |
| Spring Cloud BOM | 2023.0.6 | Cloud 组件版本管理，不是 OpenFeign 本身版本 |
| OpenFeign | Cloud 管理 | NoticeServiceClient 声明式 HTTP 调用 |
| Spring Cloud LoadBalancer | Cloud 管理 | 网关 lb://campus-service 与 Feign notice-service 的实例选择 |
| Spring Cloud Netflix Eureka | Cloud 管理 | 注册中心及各服务注册、发现 |
| Spring Cloud Gateway | Cloud 管理 | WebFlux 网关，转发 /api/**，处理跨域预检 |
| Spring Cache / Data Redis | Boot 管理 | notices 缓存，统一 TTL 300 秒，JSON 序列化 |
| Spring AMQP | Boot 管理 | RabbitTemplate 发送与 RabbitListener 消费日志 |
| JJWT | 0.12.6 | JWT 签发、解析、验证 |
| Lombok | 1.18.36 | 实体访问器等编译期生成 |
| Maven | 未锁定 | Java 依赖与打包 |

## 数据与中间件

- MySQL：主服务与公告服务共用 campus_platform 数据库；没有 Flyway/Liquibase，SQL 迁移手动执行。
- Redis：公告结果缓存 5 分钟，缓存未命中时主服务调用公告服务；没有自动降级处理。
- RabbitMQ：持久化队列 campus.operation-log，消息转换为 JSON；配送业务事务先写 operation_log_outbox，再由定时发布器投递，消费者按 event_id 幂等写入 operation_log。RabbitMQ 暂时不可用时 outbox 保留记录并自动重试。
- 文件：JPG/PNG 图片经过校验后转为 PNG，写入 Java 服务工作目录下的 uploads；限制单文件 5MB、最多 2000 万像素。未接入对象存储。

## 身份与权限

密码采用 PBKDF2-HMAC-SHA256 加盐哈希。JWT 有效期两小时，签名密钥和撤销列表在进程内；重启会使旧 token 失效。权限校验由控制器及业务服务完成，网关透传 Authorization，由主服务验证；没有 Spring Security 过滤链。生产部署需额外评估权限一致性、HTTPS、密钥持久化与日志可靠性。

## 请求路径

1. Vue 调用网关 8088 的 /api，网关通过 Eureka 发现 campus-service，业务数据落到 MySQL。
2. GET /api/notices 先查询 Redis，未命中才通过 OpenFeign 按服务名 notice-service 发现并调用公告服务（默认 8081）。
3. 公告服务用 JdbcTemplate 读取有效公告，返回最多 10 条。
4. 配送业务完成相应操作后，可发布日志事件；订单状态写入不依赖 RabbitMQ。

主服务端口 8080，公告服务端口 8081，Vite 端口 5173。网关端口 8088，Eureka 端口 8761。默认单节点注册中心仅绑定回环地址。当前没有 Docker 编排或自动化部署流水线。

服务发现不代表主服务已支持任意多实例：当前 JWT 密钥/撤销列表和图片仍保存在单个实例本地。Eureka 注册状态也不等同于数据库、Redis 的深度健康检查。
