# logistics-map-simulation Specification

## Purpose
TBD - created by archiving change map-logistics-simulation. Update Purpose after archive.
## Requirements
### Requirement: 物流地图模拟数据
系统 SHALL 为具备发货地和收货地坐标的 shipment 生成物流地图模拟数据，包括起点、终点、当前位置、路线进度和可用性说明。

#### Scenario: 生成地图数据
- **WHEN** shipment 存在发货地经纬度和收货地经纬度
- **THEN** 系统返回起点、终点、当前位置、路线进度、当前位置说明和 `routeAvailable=true`

#### Scenario: 缺少发货地坐标
- **WHEN** shipment 缺少发货地经纬度
- **THEN** 系统返回 `routeAvailable=false`，并提供无法展示地图路线的原因

#### Scenario: 缺少收货地坐标
- **WHEN** shipment 缺少收货地经纬度
- **THEN** 系统返回 `routeAvailable=false`，并提供无法展示地图路线的原因

#### Scenario: 历史物流单号
- **WHEN** 订单只有旧物流单号且没有 shipment
- **THEN** 系统不返回物流地图模拟数据，前端只展示文本物流信息

### Requirement: 物流状态到地图进度映射
系统 SHALL 根据 shipment 当前状态计算稳定的模拟路线进度和当前位置。

#### Scenario: 已揽收位置
- **WHEN** shipment 状态为 SHIPPED
- **THEN** 地图当前位置靠近发货地，路线进度小于 0.2

#### Scenario: 运输中位置
- **WHEN** shipment 状态为 IN_TRANSIT
- **THEN** 地图当前位置位于发货地和收货地之间，路线进度约为中段

#### Scenario: 派送中位置
- **WHEN** shipment 状态为 OUT_FOR_DELIVERY
- **THEN** 地图当前位置靠近收货地，路线进度大于 0.8 且小于 1

#### Scenario: 已签收位置
- **WHEN** shipment 状态为 DELIVERED
- **THEN** 地图当前位置等于收货地，路线进度为 1

#### Scenario: 异常位置
- **WHEN** shipment 状态为 EXCEPTION
- **THEN** 地图当前位置保持在最近可计算的模拟位置，并显示异常状态

### Requirement: 地图路线展示
系统 SHALL 在地图可用且物流地图模拟数据可用时展示发货地、收货地、路线和当前位置 marker。

#### Scenario: 展示模拟路线
- **WHEN** 页面收到 `routeAvailable=true` 的物流地图数据且高德 JS API 加载成功
- **THEN** 页面展示发货地 marker、收货地 marker、路线线段和当前位置 marker

#### Scenario: 地图 Key 未配置
- **WHEN** 页面没有配置高德 JS API Key
- **THEN** 页面不显示空白地图，而是展示文本降级提示并保留物流时间线

#### Scenario: 地图 SDK 加载失败
- **WHEN** 高德 JS API 加载失败
- **THEN** 页面展示地图加载失败提示，并保留物流时间线

#### Scenario: 坐标不可用
- **WHEN** 物流地图数据 `routeAvailable=false`
- **THEN** 页面展示不可绘制路线的原因，并保留物流时间线

### Requirement: 模拟推进联动地图
系统 SHALL 在物流状态被模拟推进或标记异常后返回最新物流地图模拟数据，前端 MUST 同步刷新地图当前位置。

#### Scenario: 推进后刷新地图
- **WHEN** 管理员在订单详情中推进物流状态
- **THEN** 系统返回最新 shipment、轨迹和地图模拟数据，页面更新当前位置 marker 和路线进度

#### Scenario: 标记异常后刷新地图
- **WHEN** 管理员在订单详情中标记物流异常
- **THEN** 系统返回异常状态、异常轨迹和地图模拟数据，页面展示异常状态并保持最近位置

#### Scenario: 终态不可推进
- **WHEN** shipment 已签收或已异常且管理员尝试继续推进
- **THEN** 系统返回业务错误，页面保留当前地图状态

