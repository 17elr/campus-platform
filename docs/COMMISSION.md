# 阶梯佣金

- 迁移：`backend/src/main/resources/migrations/005_commission_rules.sql`。
- 规则匹配在服务端执行，按下单金额选择当前启用规则。佣金为 `min(amount * rate, max_fee)`，四舍五入到分。
- `trade_order.commission` 和 `commission_rule_id` 在创建订单时写入，之后规则调整不会影响旧订单。
- 管理员可通过 `/api/admin/trade/commission/rules` 查看当前规则和历史；PUT 请求提交完整规则数组后旧规则停用、新规则写入并记录历史。
- 当前订单仍使用旧的 PAID 模拟支付状态；订单完成后确认收入将在后续状态流转阶段实现。
