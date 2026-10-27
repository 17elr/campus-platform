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

### 分类内容管理

`GET /api/admin/items` 仅管理员可用，支持 `type`（LOST / FOUND / SECOND_HAND）、`status`（PENDING / APPROVED / HIDDEN / WITHDRAWN / DELETED）、`keyword`、`location`、`page`、`size`。关键词搜索标题、描述与物品类别；地点精确匹配。筛选在数据库分页前执行。

响应包含 `records`、筛选后的 `total`、`page`、`size`，以及 `counts`。`counts` 按 `type` 统计分类全部数量和各状态数量，不受当前关键词、地点和状态筛选影响。省略 `type` 时管理全部类型。关键词和地点最长 100 字，非法分类或状态返回失败。

`PUT /api/admin/items/{id}/status` 沿用现有管理员权限检查，支持 `APPROVED`（审核通过或恢复公开）和 `HIDDEN`（管理员下架）。用户撤回或删除的记录在管理界面保留查看，不提供恢复操作。

管理员侧栏分类对应 `#admin/items/LOST`、`#admin/items/FOUND`、`#admin/items/SECOND_HAND`；`#admin/items/all` 管理全部信息。普通用户进入分类时继续使用公开信息大厅。

PC 管理界面按工作方式区分：寻物页使用线索列表与当前记录核查面板；招领页使用图片、拾获地点和联系信息的物品卡片；二手页使用商品目录、卖家、资料情况和展示状态表。资料情况只表示字段是否填写，不代表审核通过；缺少图片或联系方式时如实提示。三个页面共用现有筛选和状态接口，全部信息页保留汇总表格。

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
