## Context

当前系统已经有两个相关基础：

- `integrate-address-validation-map-service`：为店铺地址和订单收货地址提供经纬度，并在页面上展示单点地图 marker。
- `upgrade-shipping-model`：将发货从单个物流单号升级为 shipment、物流状态和轨迹事件。

本次 change 连接这两块能力：使用 shipment 的发货地/收货地坐标，在订单页面中展示配送路线、当前位置和状态进度。该能力是演示/模拟性质，不代表真实快递车辆位置。

## Goals / Non-Goals

**Goals:**

- 在买家、卖家、管理员订单视图中展示物流地图路线。
- 基于 shipment 状态计算模拟当前位置和路线进度。
- 管理员推进物流状态后，地图上的当前位置同步更新。
- 缺少地图 Key、缺少坐标、历史物流单号或地图 SDK 加载失败时平稳降级。
- 复用已有高德 JS API 加载封装，避免重复加载地图 SDK。

**Non-Goals:**

- 不接入真实物流平台或真实车辆位置。
- 不要求后端调用路径规划 API。
- 不做多包裹、多路线或拆单配送。
- 不做实时 WebSocket 推送；页面刷新或接口返回后更新即可。

## Decisions

### 路线由前端地图组件绘制，后端只返回模拟状态

后端负责返回路线端点、模拟进度、当前位置和状态说明；前端使用高德 JS API 绘制起点、终点、路线和当前位置 marker。

备选方案是后端调用地图路线规划接口并持久化路线结果。但这会增加 Web 服务调用量、缓存策略和失败处理复杂度。本次更适合前端绘制直线/折线，并在 JS API 可用时尝试路线规划。

### 默认使用端点插值计算当前位置

当前位置根据发货点和收货点做线性插值：

- SHIPPED：进度 0.05，靠近发货点
- IN_TRANSIT：进度 0.50，位于途中
- OUT_FOR_DELIVERY：进度 0.85，靠近收货地
- DELIVERED：进度 1.00，到达收货地
- EXCEPTION：保持最近可计算进度，若无法判断则使用 0.50

这比随机位置更稳定，也便于测试。后续可以扩展为多 waypoint 或真实路径规划。

### 扩展 ShipmentResponse 而不是新增独立查询接口

`ShipmentResponse` 增加 `map` 或等价字段，包含 origin、destination、currentPosition、progress、routeAvailable、fallbackReason。订单详情、卖家列表和管理员详情已有 shipment 响应，直接携带地图数据可以减少额外请求。

备选方案是新增 `/shipments/{id}/map` 查询接口，但页面本来已经请求 shipment 数据，单独接口会增加前端状态管理和加载时序。

### 地图组件与时间线组件分离

新增 `ShipmentMap.vue` 或等价组件，专门负责地图路线和 marker；保留 `ShipmentTimeline.vue` 展示文本轨迹。页面组合两者，而不是把地图逻辑塞进时间线组件。

这样在无地图 Key 或无坐标时仍能稳定展示时间线，也便于后续替换地图实现。

### 地图失败必须降级，不阻断订单页面

地图组件遇到未配置 Key、SDK 加载失败、缺少坐标、路线规划失败时 SHALL 展示清晰的文本降级状态，并继续显示物流时间线。

## Risks / Trade-offs

- [Risk] 直线插值不是真实道路路线。→ 明确标记为模拟路线；JS API 可用时可优先尝试路线规划，失败回退直线。
- [Risk] 地图 Key 或安全密钥配置错误导致地图空白。→ 组件内提供 fallback，不影响订单详情主流程。
- [Risk] 后端 shipment 缺少发货或收货坐标。→ 响应 `routeAvailable=false`，前端只展示文本轨迹。
- [Risk] 管理员推进后页面数据不同步。→ 推进接口返回最新 shipment，前端替换当前详情中的 shipment。
- [Risk] 与未归档的发货模型 change 依赖。→ 实施顺序应在 `upgrade-shipping-model` 之后；若并行实施，先合并 shipment 响应字段。

## Migration Plan

1. 扩展 shipment 响应 DTO，添加物流地图模拟字段。
2. 新增后端 `ShipmentMapSimulationService` 或等价计算逻辑，不新增数据库表。
3. 新增前端 `ShipmentMap.vue`，复用 `useAmap.js`。
4. 在买家、卖家、管理员订单页面组合显示 `ShipmentMap` 和 `ShipmentTimeline`。
5. 保持旧订单和无坐标订单降级为文本物流，不需要数据迁移。

## Open Questions

- 是否需要在地图上显示真实道路路线？建议本次先做直线/折线模拟，JS API 路线规划作为增强而非验收必需。
- 是否需要买家端自动轮询物流状态？建议本次不做轮询，先由页面刷新或管理员推进返回数据驱动。
