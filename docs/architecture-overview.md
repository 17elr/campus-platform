# 当前系统架构

```mermaid
flowchart LR
    U[Vue 3 用户端 / 管理后台] --> B[Spring Boot REST API / JWT]
    B --> S[交易 / 推广 / 配送业务]
    S --> DB[(MySQL / MyBatis-Plus / JDBC)]
    B --> R[(Redis 公告缓存 / TTL 5 分钟)]
    B -->|缓存未命中 / OpenFeign| N[独立公告服务]
    N --> DB
    S -. 可选配送操作日志 .-> Q[RabbitMQ]
    Q -. 消费日志 .-> DB
```

图中节点对应当前仓库代码；虚线表示可选链路，不代表已完成外部生产验收。具体取舍见[负责模块与技术决策](PORTFOLIO.md)。
