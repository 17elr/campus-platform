# 核心 Java 测试记录

2026-10-12（上海时区），Windows / JDK 23 / Maven 3.9.16，执行：

```powershell
mvn -B -ntp -f backend/pom.xml test
```

结果：**5 项执行，0 失败，0 错误，0 跳过**。测试直接调用业务服务并模拟 UserMapper / JdbcTemplate，不修改生产代码，不访问业务库、中间件或真实支付。

覆盖注册不能指定管理员角色、密码哈希与返回脱敏、停用及错误密码登录、成功登录后旧明文密码迁移、令牌撤销/停用/损坏、佣金 HALF_UP 和金额封顶。源码见 [CoreBusinessTest](../backend/src/test/java/com/campus/CoreBusinessTest.java)。

GitHub Actions 使用 JDK 17 重新运行测试并保存 Surefire 报告；远端运行状态以 Actions 页面为准。
