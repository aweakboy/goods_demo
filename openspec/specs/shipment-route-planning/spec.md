# shipment-route-planning Specification

## Purpose
TBD - created by archiving change add-cached-route-planning. Update Purpose after archive.
## Requirements
### Requirement: Shipment 路线快照规划
系统 SHALL 在 shipment 创建后基于发货地和收货地坐标执行一次路径规划，并将规划结果保存为 shipment 路线快照。

#### Scenario: 发货时规划路线成功
- **WHEN** 卖家创建 shipment 且发货地和收货地经纬度完整、路径规划已启用、地图服务返回有效路线
- **THEN** 系统保存路线快照，包含供应商、起终点坐标、路线点、距离、预计耗时、规划时间和 `AVAILABLE` 状态

#### Scenario: 缺少发货地或收货地坐标
- **WHEN** 卖家创建 shipment 但发货地或收货地缺少经纬度
- **THEN** 系统不调用外部路径规划服务，保存或返回路线不可用原因，并继续完成发货流程

#### Scenario: 路径规划未配置或被关闭
- **WHEN** 路径规划配置未启用或缺少后端 Web 服务 Key
- **THEN** 系统不调用外部路径规划服务，路线状态为 `SKIPPED` 或等价状态，并继续使用模拟路线

#### Scenario: 外部路径规划失败
- **WHEN** 高德路径规划返回失败、配额限制、认证错误、空路线、网络异常或超时
- **THEN** 系统记录失败原因，继续完成发货流程，并在地图响应中降级为模拟路线

### Requirement: 订单读取不触发路径规划
系统 SHALL 将路径规划调用限制在 shipment 创建和管理员显式刷新入口，订单读取接口 MUST NOT 触发外部路径规划调用。

#### Scenario: 买家查看订单详情
- **WHEN** 买家查看已发货订单详情
- **THEN** 系统只读取已有路线快照或模拟数据，不调用外部路径规划服务

#### Scenario: 卖家查看订单列表
- **WHEN** 卖家查看订单管理页面
- **THEN** 系统只读取已有路线快照或模拟数据，不调用外部路径规划服务

#### Scenario: 管理员查看订单详情
- **WHEN** 管理员查看订单详情但未点击刷新路线
- **THEN** 系统只读取已有路线快照或模拟数据，不调用外部路径规划服务

### Requirement: 路线快照地图响应
系统 SHALL 在 shipment 地图响应中优先返回已保存的真实路线快照，并在快照不可用时返回现有模拟路线数据。

#### Scenario: 路线快照可用
- **WHEN** shipment 存在 `AVAILABLE` 路线快照
- **THEN** 地图响应包含 `routeSource=PLANNED`、完整路线点、已完成路线点、当前位置、距离、预计耗时、规划时间和路线状态

#### Scenario: 路线快照不可用但坐标完整
- **WHEN** shipment 没有可用路线快照但发货地和收货地坐标完整
- **THEN** 地图响应包含 `routeSource=SIMULATED`、模拟路线、当前位置和路线不可用原因

#### Scenario: 路线快照和坐标都不可用
- **WHEN** shipment 没有可用路线快照且缺少发货地或收货地坐标
- **THEN** 地图响应返回 `routeAvailable=false`，并提供无法展示地图路线的原因

### Requirement: 基于真实路线计算当前位置
系统 SHALL 在真实路线快照可用时沿路线点按物流状态进度计算当前位置和已完成路线段。

#### Scenario: 已揽收位置
- **WHEN** shipment 状态为 `SHIPPED` 且路线快照可用
- **THEN** 当前位置位于路线起点附近，已完成路线段小于整条路线的 20%

#### Scenario: 运输中位置
- **WHEN** shipment 状态为 `IN_TRANSIT` 且路线快照可用
- **THEN** 当前位置位于路线中段附近，已完成路线段约为整条路线的一半

#### Scenario: 派送中位置
- **WHEN** shipment 状态为 `OUT_FOR_DELIVERY` 且路线快照可用
- **THEN** 当前位置位于路线终点附近，已完成路线段大于整条路线的 80% 且小于 100%

#### Scenario: 已签收位置
- **WHEN** shipment 状态为 `DELIVERED` 且路线快照可用
- **THEN** 当前位置等于路线终点，已完成路线段覆盖整条路线

#### Scenario: 物流异常位置
- **WHEN** shipment 状态为 `EXCEPTION` 且路线快照可用
- **THEN** 系统返回最近可计算的异常位置和异常说明，最低要求保持在路线中段

### Requirement: 管理员刷新路线规划
系统 SHALL 允许管理员对单个 shipment 手动刷新路线规划，并限制重复刷新造成的外部调用。

#### Scenario: 管理员刷新成功
- **WHEN** 管理员对坐标完整的 shipment 手动刷新路线且刷新间隔满足配置要求
- **THEN** 系统调用外部路径规划服务，更新路线快照，并返回最新 shipment 地图响应

#### Scenario: 非管理员刷新路线
- **WHEN** 非管理员尝试刷新 shipment 路线规划
- **THEN** 系统拒绝请求并返回 403

#### Scenario: 刷新过于频繁
- **WHEN** 管理员在最小刷新间隔内重复刷新同一 shipment 路线
- **THEN** 系统拒绝重复刷新，不调用外部路径规划服务，并返回明确提示

#### Scenario: 刷新失败保留旧路线
- **WHEN** shipment 已有可用路线快照且管理员刷新路线失败
- **THEN** 系统保留旧的可用路线快照，记录新的失败原因，并返回当前仍可展示的路线数据

