## Why

当前物流升级已经能记录 shipment 状态时间线，但用户看到的仍是文本轨迹，无法直观理解“从店铺发出、运输中、派送中、到达收货地”的空间过程。
在店铺地址和收货地址已有经纬度后，引入地图路线与当前位置模拟，可以把物流状态升级为可视化配送过程，更接近真实电商订单追踪体验。

## What Changes

- 新增物流地图模拟能力：基于 shipment 的发货地坐标和收货地坐标，在地图上展示起点、终点、模拟路线和当前位置 marker。
- 新增后端路线快照/模拟位置输出：根据 shipment 状态返回路线点、当前进度、当前位置、当前位置说明和是否可继续推进。
- 物流推进时同步更新地图进度：`SHIPPED` 靠近发货点，`IN_TRANSIT` 位于中途，`OUT_FOR_DELIVERY` 靠近收货地，`DELIVERED` 到达收货地，`EXCEPTION` 保持最近位置并显示异常。
- 前端新增物流地图组件，复用已有高德 JS API 加载能力；地图不可用、Key 未配置、缺少坐标或历史物流单号时降级为文本轨迹。
- 买家订单详情、卖家订单管理、管理员订单详情展示物流地图；管理员模拟推进后地图 marker 和路线进度实时刷新。
- 本次仍为模拟路线，不接入真实快递实时位置或真实车辆轨迹。

## Capabilities

### New Capabilities

- `logistics-map-simulation`: 物流地图路线、当前位置模拟、地图进度展示和地图降级体验。

### Modified Capabilities

- `order-management`: 买家订单详情在物流具备坐标时展示地图路线和当前位置。
- `seller-dashboard`: 卖家订单管理在物流具备坐标时展示配送地图，并与物流状态同步。
- `admin-order-oversight`: 管理员订单详情展示配送地图，并在模拟推进或异常后刷新当前位置。

## Impact

- 后端：扩展 shipment 响应或新增物流地图 DTO/service，计算路线点、当前坐标和进度；更新模拟推进返回数据。
- 前端：新增物流地图组件；更新 `OrderDetailView.vue`、`SellerOrdersView.vue`、`AdminOrders.vue`；复用 `useAmap.js`，必要时扩展 `AddressMap.vue` 的能力。
- 数据库：优先不新增表，路线可由发货/收货坐标和 shipment 状态实时计算；如实现需要可在 shipment 上增加当前位置/进度字段。
- 配置：继续依赖 `VITE_AMAP_JS_API_KEY`、`VITE_AMAP_SECURITY_JS_CODE`；无 Key 时必须降级展示时间线。
- 依赖关系：建议在 `integrate-address-validation-map-service` 和 `upgrade-shipping-model` 已实现后应用；历史无坐标或 legacy trackingNumber 订单只能文本展示。
