# 校园互助平台：商业化功能开发文档

## 1. 项目目标

本项目是一个校园失物招领、寻物启事和二手交易平台。下一阶段重点从“信息展示平台”升级为“可交易、可结算、可运营”的校园交易平台。

商业化目标：

- 提高二手商品成交率
- 通过佣金、推广、会员、配送和广告获得收入
- 为管理员提供收入、交易和用户增长分析
- 保持普通用户基础发布和浏览免费

## 2. 当前技术栈

### 前端

- Vue 3
- Vite
- Axios
- 项目目录：`frontend/src`
- 管理员页面：`frontend/src/components/Admin.vue`
- 个人中心：`frontend/src/components/Account.vue`
- 主应用和信息大厅：`frontend/src/App.vue`

### 后端

- Spring Boot 3
- MyBatis-Plus
- MySQL
- 项目目录：`backend/src/main/java`
- 端口：`8080`
- API 前缀：`/api`

### 数据库

- 数据库名：`campus_platform`
- 配置文件：`backend/src/main/resources/application.yml`
- 用户名：`root`
- 默认密码：`123456`

## 3. 已有账号和数据

管理员账号：

```text
用户名：admin
密码：admin123
```

演示账号：

```text
用户名：demo1 到 demo50
密码：demo12345
```

演示数据迁移文件：

```text
backend/src/main/resources/migrations/003_demo_seed.sql
```

该迁移包含 50 个演示用户和 50 个演示商品。

## 4. 已有核心表

### user

用户表，主要字段：

- `id`
- `username`
- `password`
- `nickname`
- `role`
- `status`
- `created_at`

### item

信息和商品表，主要字段：

- `id`
- `title`
- `description`
- `type`
- `category`
- `location`
- `contact`
- `status`
- `user_id`
- `created_at`

商品类型：

```text
LOST
FOUND
SECOND_HAND
```

商品状态：

```text
PENDING
APPROVED
HIDDEN
WITHDRAWN
DELETED
```

### trade_order

交易订单表，主要字段：

- `id`
- `order_no`
- `product_id`
- `buyer_id`
- `seller_id`
- `amount`
- `commission`
- `seller_amount`
- `payment_method`
- `status`
- `created_at`
- `paid_at`

当前佣金默认值为 3%。

## 5. 已有接口

### 用户端

```text
GET    /api/items?page=1&size=12
GET    /api/items/{id}
GET    /api/items/mine?page=1&size=10
POST   /api/items
PUT    /api/items/{id}/withdraw
DELETE /api/items/{id}

POST   /api/trade/orders
POST   /api/trade/orders/{id}/pay
GET    /api/trade/orders?page=1&size=10

GET    /api/auth/me
PUT    /api/users/profile
```

### 管理员端

```text
GET /api/admin/stats
GET /api/admin/users?page=1&size=10
GET /api/admin/items?page=1&size=10
GET /api/admin/items/pending
GET /api/admin/trade/stats
GET /api/admin/trade/orders?page=1&size=10
PUT /api/admin/trade/commission
PUT /api/admin/users/{id}/status
DELETE /api/admin/users/{id}
PUT /api/admin/items/{id}/status
```

分页接口统一返回：

```json
{
  "records": [],
  "total": 0,
  "page": 1,
  "size": 10
}
```

## 6. 下一阶段开发优先级

### 第一阶段：推广商品

新增商品推广能力。

建议新增表：`item_promotion`

字段建议：

- `id`
- `item_id`
- `user_id`
- `promotion_type`
- `amount`
- `start_at`
- `end_at`
- `status`
- `created_at`

推广类型：

```text
TOP_24H       24 小时置顶
FEATURED_3D   首页推荐 3 天
CATEGORY_7D   分类置顶 7 天
```

建议价格：

```text
TOP_24H：1.99 元
FEATURED_3D：4.99 元
CATEGORY_7D：9.90 元
```

需要实现：

- 商品详情页购买推广
- 推广订单
- 推广支付
- 商品列表推广标识
- 推广商品排序
- 管理员推广收入统计

### 第二阶段：佣金规则

当前佣金是固定 3%，需要改为后台可配置的阶梯佣金。

建议新增表：`commission_rule`

字段建议：

- `id`
- `min_amount`
- `max_amount`
- `rate`
- `max_fee`
- `status`
- `created_at`

默认规则：

| 金额区间 | 比例 | 最高佣金 |
|---|---:|---:|
| 0–50 | 2% | 10 元 |
| 50–200 | 3% | 15 元 |
| 200–500 | 4% | 20 元 |
| 500 以上 | 5% | 30 元 |

必须保证：

- 下单时锁定佣金金额
- 订单完成后不能因规则变化影响旧订单
- 管理员可以查看佣金规则历史

### 第三阶段：卖家会员

新增卖家会员计划。

建议新增表：`seller_subscription`

字段建议：

- `id`
- `user_id`
- `plan`
- `amount`
- `start_at`
- `end_at`
- `status`
- `created_at`

建议套餐：

```text
FREE：免费，每月最多发布 10 条
PRO_MONTHLY：9.90 元/月
PRO_SEASON：24.90 元/季度
```

Pro 权益：

- 发布数量不限
- 每月 3 次免费置顶
- 卖家数据统计
- 交易完成率展示
- 更高搜索排序

### 第四阶段：校园配送

新增配送订单：`delivery_order`

字段建议：

- `id`
- `trade_order_id`
- `sender_id`
- `receiver_id`
- `pickup_location`
- `delivery_location`
- `fee`
- `platform_fee`
- `courier_id`
- `status`
- `created_at`
- `completed_at`

配送状态：

```text
WAITING
ACCEPTED
PICKED_UP
DELIVERING
COMPLETED
CANCELLED
```

平台抽成建议：15%。

### 第五阶段：校园商家广告

新增广告表：`advertisement`

字段建议：

- `id`
- `merchant_name`
- `title`
- `image_url`
- `target_url`
- `placement`
- `amount`
- `start_at`
- `end_at`
- `status`
- `impressions`
- `clicks`
- `created_at`

广告位置：

```text
HOME_BANNER
SIDEBAR
CATEGORY_TOP
DETAIL_RECOMMEND
```

建议价格：

```text
侧边栏：199 元/月
分类页：399 元/月
首页 Banner：999 元/月
```

## 7. 管理员后台需要增加的模块

### 收入概览

展示：

- 总收入
- 交易佣金
- 商品推广收入
- 会员收入
- 配送抽成
- 广告收入
- 今日收入
- 本月收入

### 收入趋势图

需要支持：

- 近 7 天
- 近 30 天
- 本季度
- 自定义时间范围

图表建议：

- 收入折线图
- 收入来源堆叠柱状图
- 订单金额趋势图
- 佣金趋势图

### 交易分析

展示：

- 成交订单数
- 交易总额
- 平均订单金额
- 交易成功率
- 取消率
- 退款率
- 商品分类成交排行
- 热门地点排行

### 用户分析

展示：

- 注册用户数
- 日活用户
- 月活用户
- 发布用户数
- 交易用户数
- 付费用户数
- 用户留存率

### 运营管理

需要支持：

- 推广商品管理
- 会员管理
- 广告位管理
- 商家管理
- 配送员管理
- 佣金规则管理
- 退款和纠纷处理

## 8. 订单状态建议

当前状态较少，建议扩展为：

```text
PENDING_PAYMENT
PAID
SELLER_CONFIRMED
DELIVERING
COMPLETED
REFUND_REQUESTED
REFUNDED
CANCELLED
DISPUTED
```

状态流转：

```text
PENDING_PAYMENT -> PAID
PAID -> SELLER_CONFIRMED
SELLER_CONFIRMED -> DELIVERING
DELIVERING -> COMPLETED
PAID -> REFUND_REQUESTED
REFUND_REQUESTED -> REFUNDED
```

佣金应在 `COMPLETED` 后正式计入平台收入，不能只在创建订单时统计。

## 9. 关键开发要求

### 分页

所有列表都必须分页：

- 信息大厅
- 管理员用户
- 管理员商品
- 管理员订单
- 个人中心我的发布
- 个人中心买到的订单
- 个人中心卖出的订单
- 广告列表
- 推广订单
- 配送订单

前端必须显示：

- 当前页
- 总页数
- 上一页
- 下一页
- 每页数量
- 无数据状态

### 权限

- 普通用户不能访问管理员接口
- 用户只能修改自己的商品和订单
- 管理员账号不能被禁用或删除
- 佣金、广告、会员配置只允许管理员修改
- 支付和退款必须校验订单所属关系

### 数据一致性

- 金额使用 `BigDecimal`
- 佣金在服务端计算
- 不能相信前端传入的佣金金额
- 订单支付接口必须防止重复支付
- 订单状态变更要校验当前状态
- 收入报表只统计符合状态的订单

## 10. 推荐开发顺序

新窗口建议按以下顺序执行：

1. 先修复并验证所有分页接口
2. 新增推广商品表和推广订单
3. 实现商品置顶和首页推荐
4. 新增佣金规则表和后台配置
5. 增加收入统计接口和可视化图表
6. 开发卖家会员
7. 开发校园配送
8. 开发校园广告
9. 最后统一做退款、纠纷和运营报表

每完成一个阶段，都必须：

- 执行数据库迁移
- 重启后端
- 运行 Maven 编译
- 运行前端 `npm run build`
- 使用真实接口验证
- 检查浏览器页面是否显示真实数据

## 11. 本地运行

后端：

```powershell
cd backend
mvn spring-boot:run
```

前端：

```powershell
cd frontend
npm run dev
```

如果出现 8080 端口占用：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
Stop-Process -Id <PID> -Force
```

## 12. 新窗口启动提示词

将以下内容复制到新窗口：

```text
请阅读项目根目录的 DEVELOPMENT_PLAN.md，继续开发校园互助平台商业化功能。

当前优先实现第一阶段：商品推广和置顶。

要求：
1. 先检查现有代码和数据库，不要破坏已有功能。
2. 新增推广订单、推广支付、推广状态和管理员统计。
3. 所有列表必须分页。
4. 使用真实数据库数据，不能用前端假数据代替。
5. 每完成一个功能都执行数据库迁移、后端编译、前端构建和接口验证。
6. 遇到错误直接修复并继续，不要只报告问题。
7. 最后返回完整实现结果和验证结果。
```


## 13. 实施进度（2026-09-18）

第一阶段“商品推广和置顶”及关联分页修复已完成并通过真实接口/浏览器验证。
实现范围、接口、迁移、回归命令与支付限制见 [docs/PROMOTIONS.md](docs/PROMOTIONS.md)。

- [x] 推广表、订单、模拟支付、取消、到期状态
- [x] 大厅置顶、首页推荐、物品类别置顶、推广标识
- [x] 管理员推广收入及订单分页
- [x] 大厅/个人中心/后台关联分页修复
- [x] 数据库迁移、后端重启、编译、前端构建、真实接口和浏览器验证
- [ ] 第二阶段：持久化阶梯佣金和规则历史
- [ ] 交易完成状态及收入确认、后续会员/配送/广告/退款与纠纷

下一轮优先推进第二阶段。当前推广支付为模拟，不产生真实扣款。


## 14. 第二阶段进度（2026-09-18）

已完成阶梯佣金服务端基础能力：新增 `commission_rule` 与 `commission_rule_history`，默认 0–50/2%/10 元、50–200/3%/15 元、200–500/4%/20 元、500 以上/5%/30 元；新交易下单时由服务端匹配规则、计算并锁定佣金金额及规则 ID，旧订单不受后续规则修改影响。管理员接口：`GET/PUT /api/admin/trade/commission/rules`。

已执行迁移、Maven 编译并验证管理员规则接口。后台规则编辑界面和更完整的订单完成/收入确认仍待继续补齐。


## 15. 校园配送模块（2026-09-18）

已完成配送订单基础模块：配送表、创建配送、分页查询、WAITING → ACCEPTED → PICKED_UP → DELIVERING → COMPLETED 状态流转、取消、配送费 5 元及平台抽成 15%。仅已支付交易的买卖双方可创建和查看配送，配送订单不可被第三方操作；管理员可查看全部配送订单和统计。个人中心新增配送订单页，管理员后台新增配送订单列表。

接口详见 `DeliveryController`：`POST/GET /api/delivery`、`PUT /api/delivery/{id}/status`、`GET /api/delivery/admin/orders`、`GET /api/delivery/admin/stats`。


## 16. 管理员首页布局优化（2026-09-18）

管理员登录后首页自动显示运营总览：平台统计卡片、成交订单/交易金额/平台佣金/访客用户、近 7 日趋势图和最近交易订单表。普通用户和游客仍显示原首页。管理员后台页面保留用户、商品、订单、推广、配送、佣金及平台配置等增删改操作，避免运营查看与数据维护混在同一页面。


## 17. 管理员运营总览重构（2026-09-18）

管理员首页与管理后台已彻底分离：管理员首页显示运营仪表盘，包含统计卡片、系统数据分布环图、收入来源横向图、系统数据表格、交易趋势及最近订单；管理员后台隐藏趋势图和运营分析图，只保留用户/商品/订单增删改、推广、配送、佣金及平台配置。


## 18. 配送业务扩展与日期筛选（2026-09-18）

管理员运营首页新增本日、近 24 小时、近 7 天、近 30 天、本季度及自定义日期区间筛选。校园配送新增业务类型：快递代拿（EXPRESS）、校园跑腿（ERRAND）、小吃街外卖（FOOD）；用户和访客页面显示类型，管理员配送列表可按类型筛选。


## 19. 校园配送页面视觉升级（2026-09-18）

校园配送页改为服务大厅布局：渐变英雄区、快递代拿/校园跑腿/小吃街外卖三类服务卡、订单统计、类型筛选、进度条订单卡片和响应式空状态。游客可直接访问 `#delivery`，路由不会被重置。
