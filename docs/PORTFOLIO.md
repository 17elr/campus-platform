# 负责模块与关键技术决策

## 个人负责范围

本人主导需求梳理、功能设计、前后端开发、联调及文档整理，使用 AI 辅助编码、排错和测试。AI 辅助方式依据本人确认；模块范围依据本仓库源码，不把开源依赖能力或工具生成代码全部归为原创实现。

| 负责模块 | 代码入口 | 可讲解内容 |
| --- | --- | --- |
| 用户登录与注册 | [UserServiceImpl](../backend/src/main/java/com/campus/service/impl/UserServiceImpl.java)、[Tokens](../backend/src/main/java/com/campus/security/Tokens.java) | PBKDF2、注册角色限制、JWT 撤销与停用校验 |
| 校园信息与留言 | [ItemController](../backend/src/main/java/com/campus/controller/ItemController.java)、[CommentServiceImpl](../backend/src/main/java/com/campus/service/impl/CommentServiceImpl.java) | 分页筛选、发布审核、作者与管理员操作范围 |
| 推广与佣金 | [PromotionService](../backend/src/main/java/com/campus/service/PromotionService.java)、[CommissionService](../backend/src/main/java/com/campus/service/CommissionService.java) | 推广时段、阶梯佣金、金额舍入与封顶 |
| 配送履约 | [DeliveryService](../backend/src/main/java/com/campus/service/DeliveryService.java) | 配送员申请、条件抢单、状态流转与纠纷处理 |
| 服务集成与页面 | [主服务](../backend/)、[公告服务](../notice-service/)、[Vue 前端](../frontend/src/) | OpenFeign、Redis 缓存、可选 RabbitMQ 日志 |

## 技术决策

| 决策 | 原因与代价 |
| --- | --- |
| 主业务服务 + 独立公告服务 | 用小规模服务拆分演示跨服务调用；两个服务共享数据库，尚未实现完整数据自治。 |
| 公告缓存 TTL 5 分钟 | 降低重复读取；存在短暂旧数据，缓存或公告服务不可用时尚无自动降级。 |
| RabbitMQ 用于可选配送日志 | 将日志消费者与业务解耦；当前发送失败仅记录警告，不承诺必达。 |
| 条件 UPDATE 接单与状态变更 | 用受影响行数判断并发冲突；新增单元测试不等同于数据库并发压测。 |
| JWT + PBKDF2 | 不返回密码摘要，限制注册角色；签名密钥和撤销列表在进程内，重启需要重新登录。 |
| BigDecimal 计算佣金 | 明确 HALF_UP 到分及上限，避免二进制浮点金额误差。 |

## 可核验的证据

- [5 项核心 Java 测试](../backend/src/test/java/com/campus/CoreBusinessTest.java)：注册防提权与密码脱敏、禁用/错误密码登录、旧密码迁移、JWT 撤销与停用、佣金舍入与封顶。
- [本次测试记录](verification-core-tests.md)。这些是隔离单元测试，不启动 Spring 上下文，不连接数据库或中间件。
- [历史演示截图来源](images/SOURCES.md)，[架构图](architecture-overview.md)，[GitHub Actions](../.github/workflows/ci.yml)。

## 简历表述

主导 Vue 3 + Spring Boot 校园互助平台开发，覆盖信息发布、二手交易、推广与配送履约；通过 OpenFeign 调用独立公告服务，Redis 缓存公告，RabbitMQ 可选异步记录配送日志；补充 5 项核心 Java 单元测试与 CI，覆盖登录安全、注册角色隔离和佣金金额边界。

支付为演示流程；截图中的记录数量属于测试数据，不表示真实用户或业务规模。
