## Why

当前店铺没有地址信息，订单也只保存一段自由文本收货地址，无法支持后续更真实的发货模拟、地图展示、距离估算和地址校验。先把店铺地址与订单收货地址结构化，是后续物流与地图能力的基础。

## What Changes

- 卖家注册或编辑店铺时需要填写结构化店铺地址。
- 买家创建订单时需要填写结构化收货地址，而不是单一 textarea 文本。
- 订单创建时保存收货地址快照，确保历史订单不受后续地址修改影响。
- 后端为店铺和订单保存标准化地址字段，包括省、市、区、详细地址、完整地址、经纬度和地址校验状态。
- 前端店铺表单、结算页、订单详情、卖家订单页和管理员订单详情展示结构化地址信息。
- 暂不接入真实地图 SDK、路线规划或物流轨迹模拟；本 change 只建立地址数据基础。

## Capabilities

### New Capabilities

<!-- No standalone new capability. Structured address behavior modifies existing shop and order capabilities. -->

### Modified Capabilities

- `seller-shop-management`: 店铺注册、查看和编辑要求包含结构化店铺地址。
- `order-management`: 创建订单要求包含结构化收货地址，并在订单中保存地址快照。

## Impact

- 后端实体：`Shop`、`Order`
- 后端 DTO：`ShopRequest`、`ShopResponse`、`ShopStorefrontResponse`、`OrderRequest`、`AdminOrderResponse`
- 后端服务：`ShopService`、`OrderService`
- 数据库：`shops`、`orders` 表新增结构化地址和经纬度字段；保留兼容用的完整地址文本。
- 前端页面：`SellerShopView.vue`、`CheckoutView.vue`、`OrderDetailView.vue`、`SellerOrdersView.vue`、`AdminOrders.vue`、`ShopStorefrontView.vue`
- 测试：补充后端 service 单元测试，并运行 `mvn.cmd test` 和 `npm.cmd run build`。
