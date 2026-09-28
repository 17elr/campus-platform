# 校园配送与履约接口

基础路径 /api/delivery。除 /public 外均需 Authorization: Bearer token，/admin 路径需管理员。配送员必须提交申请并审核通过才能接单。

## 接口清单

| 方法 | 路径 | 请求与用途 |
| --- | --- | --- |
| POST | / | tradeOrderId 可选、deliveryType、pickupLocation、deliveryLocation、requesterNote；独立需求或关联交易配送 |
| GET | /public | page、size、type；公开展示 |
| GET | / | 当前用户的配送订单，page、size |
| GET | /available | page、size、type；配送员可接订单 |
| POST | /{id}/claim | 配送员接单 |
| PUT | /{id}/status | JSON status；履约状态流转 |
| POST | /{id}/dispute | JSON reason；发起纠纷 |
| GET | /courier/me | 当前配送员申请 |
| POST | /courier/applications | JSON campusId、note；申请配送员 |
| GET | /admin/couriers | page、size；申请列表 |
| PUT | /admin/couriers/{id} | JSON status=APPROVED/REJECTED、note；驳回需理由 |
| PUT | /admin/orders/{id}/dispute | JSON action、note；管理员处理纠纷 |
| GET | /admin/orders | page、size、type；全部订单 |
| GET | /admin/stats | 配送统计 |

## 流程与数据

常规状态：WAITING → ACCEPTED → PICKED_UP → DELIVERING → COMPLETED；取消为 CANCELLED，纠纷为 DISPUTED。服务端根据发起人、配送员身份和当前状态限制操作，并通过条件更新防止重复接单。

配送费和抽成由平台设置读取，初始化值为 5.00 元和 15%，并非不可修改的固定常量。关联交易配送要求满足服务端对交易订单与用户关系的校验；tradeOrderId 为空时创建独立需求。

011_delivery_fulfillment.sql 增加配送员申请及纠纷字段。使用旧数据库时需先迁移。支付未接入真实网关；费用字段和状态变化不代表真实资金结算。

启用 CAMPUS_MQ_ENABLED 后，相关配送操作通过 RabbitMQ 异步写入 operation_log；关闭时跳过，订单主流程不依赖消息队列。
