# 开发接口文档（v1.0.0）

基础地址：`http://localhost:8080`。响应结构：`{ "code": 200, "message": "ok", "data": ... }`。需要登录的接口携带 `Authorization: Bearer <token>`。

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
