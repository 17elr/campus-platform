# 开发接口文档（v1.0.0）

基础地址：`http://localhost:8080`。响应结构：`{ "code": 200, "message": "操作成功", "data": ... }`。需要登录的接口携带 `Authorization: Bearer <token>`。

## 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/register` | 注册，JSON：username、password、nickname |
| POST | `/login` | 登录，返回 JWT |
| GET | `/me` | 当前用户 |
| POST | `/logout` | 撤销 token |

用户名 3–30 位字母/数字/下划线，密码 8–72 位；普通注册角色固定为 STUDENT。

## 物品 `/api/items`

| 方法 | 路径 | 登录 | 说明 |
|---|---|---|---|
| GET | `/` | 否 | 列表，参数 keyword、type、location、page、size |
| GET | `/counts` | 否 | 各类型数量 |
| GET | `/{id}` | 否 | 详情 |
| POST | `/` | 是 | 发布物品 |
| GET | `/mine` | 是 | 我的发布 |
| PUT | `/{id}/approve` | 管理员 | 审核通过 |
| PUT | `/{id}/withdraw` | 是 | 下架自己的信息 |
| DELETE | `/{id}` | 是 | 软删除自己的信息 |

类型：`LOST`、`FOUND`、`SECOND_HAND`；状态：`PENDING`、`APPROVED`、`WITHDRAWN`、`DELETED`。

## 留言 `/api/items/{itemId}/comments`

- `GET /`：查看留言（公开）。
- `POST /`：发表留言，JSON 为 `content`（1–500 字）和可选 `parentId`。
- `DELETE /{id}`：作者或管理员删除，保留回复占位。

## 交易 `/api/trade`

- `POST /orders`：创建订单，JSON 为 `productId`、`amount`、`paymentMethod`。
- `POST /orders/{id}/pay`：将本人订单标记为 PAID。
- `GET /orders`：查询买家或卖家订单。

v1 使用演示支付状态更新，不接入第三方支付。

## 其他接口

- `GET /api/users`、`PUT /api/users/profile`：用户列表与资料更新。
- `POST /api/images`、`GET /api/images/{name}`：图片上传（multipart file，单文件最大 5MB）与读取。
- `GET /api/notices`：公告。
- 管理员接口：`/api/admin/stats`、`/trade/stats`、`/trade/orders`、`/trade/commission`、`/users`、`/items`、`/items/pending` 及用户/物品状态管理接口。

JWT 有效期 2 小时，撤销列表在内存中，服务重启后需重新登录。生产环境应使用 HTTPS、环境变量、持久化文件存储和真实支付服务。


## 2026-09 公告与基础设施更新

`GET /api/notices` 保持公开接口，data 为公告数组，字段 title、content、createdAt。主服务使用 Redis 缓存 300 秒，未命中时通过 OpenFeign 请求公告服务同名路径。公告服务最多返回 10 条有效公告；不需要浏览器直接访问 8081。

## 广告接口 `/api/advertisements`

| 方法 | 路径 | 权限 | 请求说明 |
| --- | --- | --- | --- |
| GET | / | 公开 | 可选 placement |
| POST | /{id}/impression | 公开 | 曝光计数 |
| POST | /{id}/click | 公开 | 点击计数 |
| POST | / | 管理员 | 新建广告 |
| PUT | /{id} | 管理员 | 更新广告 |
| DELETE | /{id} | 管理员 | 删除广告 |
| GET | /admin | 管理员 | page=1、size=10 |
| GET | /admin/stats | 管理员 | 广告与计数统计 |
| PUT | /{id}/status | 管理员 | JSON status |

广告请求字段：merchantName、title、imageUrl、targetUrl、placement、amount、startAt、endAt。具体校验和投放位置以 AdvertisementService 为准；该模块记录投放金额，不代表真实收款。

配送新接口、申请审核及纠纷请求见 [配送文档](DELIVERY.md)。推广接口见 [推广文档](PROMOTIONS.md)。

业务 code 与 HTTP 状态不一定一致，客户端应同时处理网络/HTTP 异常和响应体 code；成功 code 为 200。
