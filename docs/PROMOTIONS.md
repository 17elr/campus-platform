# 第一阶段交付：商品推广与分页（2026-09-18）

## 已实现

- 商品详情及个人中心“我的发布”可创建推广订单；“推广订单”可继续付款、取消待付款订单。
- TOP_24H：1.99 元 / 24 小时，公开大厅置顶；FEATURED_3D：4.99 元 / 72 小时，首页推荐；CATEGORY_7D：9.90 元 / 168 小时，按物品类别筛选时置顶。
- 只允许推广自己的、已公开的二手商品。服务端计算价格；支付时再次检查商品状态及归属。
- 商品行锁串行化同一商品的下单和支付，复用同类型待付款订单，拒绝重复有效推广和重复支付。
- 支付成功开始计时，到期查询返回 EXPIRED，列表即时移除加权和标识，无需定时任务。数据库保留 ACTIVE 历史原值，以 end_at 判定有效期。
- 下架或删除后不再公开展示；不暂停推广计时，不自动退款。
- 管理员查看分页推广订单、已付款总额、今日金额、本月金额、付款笔数及未到期笔数。到期的已付订单仍计入历史收入。
- 大厅、个人发布、买入/卖出、后台用户/物品/交易/待审核及推广接口统一规范分页参数（page >= 1，1 <= size <= 50），按时间和 ID 稳定排序。筛选计数与数据一致。
- 前端分页显示当前/总页数、总条数、页大小及空状态。大厅不再截取前 50 条做本地分页，不再在服务失败时填充演示商品。后台搜索框明确为“筛选本页”。

## 支付边界

当前是**本地模拟支付**，页面明确提示没有真实扣款。没有接入支付宝/微信网关、签名回调、退款或真实结算；推广收入为数据库中的模拟付款记录。生产接入支付时应将激活操作放到验签后的支付通知流程。

本阶段没有实现第二阶段阶梯佣金、会员、配送、广告、退款和纠纷。旧交易模块仍沿用原来的付款/佣金统计逻辑，应在后续订单状态及佣金阶段统一改造。

## 数据迁移

已有库执行 `backend/src/main/resources/migrations/004_item_promotion.sql`，该脚本可重复执行。新库 schema.sql 已包含推广表。迁移后重启后端。

## API

所有结果沿用 Result 包装，列表数据为 `{records,total,page,size}`。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/promotions/plans | 公共套餐价格 |
| POST | /api/promotions/orders | `{itemId,promotionType}`，创建/复用待付款订单 |
| POST | /api/promotions/orders/{id}/pay | 本人模拟支付并激活 |
| POST | /api/promotions/orders/{id}/cancel | 本人取消待付款订单 |
| GET | /api/promotions/orders?page=1&size=10 | 本人的推广订单 |
| GET | /api/admin/promotions/orders?page=1&size=10 | 管理员查询全部推广订单 |
| GET | /api/admin/promotions/stats | 管理员推广收入统计 |
| GET | /api/items?scene=home | 首页推广排序 |
| GET | /api/items?category=电子产品 | 精确类别筛选及分类推广排序 |
| GET | /api/trade/orders?role=buy | 买入订单（sell 为卖出，all 为双方） |

## 已完成验证

- MySQL 迁移执行成功，后端重启成功。
- Maven test（编译通过；项目当前没有 JUnit 单元测试）、Vite production build。
- `backend/scripts/promotion-smoke.py`：真实 MySQL/API 权限、固定价格、待付款订单复用、5 请求并发支付、收入仅一次累加、三种排序、到期、续购、取消、下架禁止付款、筛选总数和分页边界。
- `frontend/scripts/promotion-browser.cjs`：Edge 无头浏览器直接访问真实后端，验证详情创建订单/付款、推广标识、列表排序/翻页、个人推广订单、后台推广收入，无页面 JS 异常；检查桌面和手机截图。
- 测试只清理脚本自己创建的商品和推广订单，不修改既有业务记录。截图及浏览器报告位于 frontend/test-results/（不入库）。

运行回归前启动 MySQL/后端/前端，设置 DB_PASSWORD（默认本地开发密码）。浏览器脚本要求 Playwright 可用，可用 PLAYWRIGHT_MODULE 指向本地安装位置。脚本使用现有 admin/demo1/demo2 本地演示账号。
