## 1. 后端地图模拟 DTO

- [x] 1.1 新增物流地图点位 DTO，包含经度、纬度、标题、地址文本
- [x] 1.2 新增物流地图响应 DTO，包含 origin、destination、currentPosition、progress、routeAvailable、fallbackReason、statusLabel
- [x] 1.3 在 `ShipmentResponse` 中新增 map/mapSimulation 字段
- [x] 1.4 确保 legacy shipment 响应不返回可用地图路线

## 2. 后端地图模拟服务

- [x] 2.1 新增 `ShipmentMapSimulationService` 或等价服务
- [x] 2.2 校验 shipment 是否具备发货地和收货地经纬度
- [x] 2.3 实现 SHIPPED 状态的进度和当前位置计算
- [x] 2.4 实现 IN_TRANSIT 状态的进度和当前位置计算
- [x] 2.5 实现 OUT_FOR_DELIVERY 状态的进度和当前位置计算
- [x] 2.6 实现 DELIVERED 状态的进度和当前位置计算
- [x] 2.7 实现 EXCEPTION 状态的最近模拟位置和异常说明
- [x] 2.8 将地图模拟服务接入 `ShipmentService.toResponse()`

## 3. 后端接口联动

- [x] 3.1 买家订单详情响应返回 shipment 地图模拟数据
- [x] 3.2 卖家订单列表响应返回 shipment 地图模拟数据
- [x] 3.3 管理员订单详情响应返回 shipment 地图模拟数据
- [x] 3.4 管理员模拟推进物流后返回最新地图模拟数据
- [x] 3.5 管理员标记物流异常后返回最新地图模拟数据
- [x] 3.6 无坐标、无地图数据或 legacy 物流时接口保持向后兼容

## 4. 前端物流地图组件

- [x] 4.1 新增 `ShipmentMap.vue`，复用 `useAmap.js` 加载高德 JS API
- [x] 4.2 组件展示发货地 marker、收货地 marker、当前位置 marker
- [x] 4.3 组件绘制模拟路线和已完成进度线
- [x] 4.4 组件处理地图 Key 未配置的降级状态
- [x] 4.5 组件处理 SDK 加载失败的降级状态
- [x] 4.6 组件处理 `routeAvailable=false` 和缺少坐标的降级状态
- [x] 4.7 组件在桌面和移动端保持稳定高度，不挤压订单主要信息

## 5. 买家订单详情接入

- [x] 5.1 更新 `OrderDetailView.vue`，在物流时间线附近展示 `ShipmentMap`
- [x] 5.2 已发货且地图数据可用时展示路线和当前位置
- [x] 5.3 未发货订单不展示空地图
- [x] 5.4 legacy 物流单号展示文本降级，不展示路线
- [x] 5.5 地图不可用时仍展示物流时间线

## 6. 卖家订单管理接入

- [x] 6.1 更新 `SellerOrdersView.vue`，在 shipment 卡片中展示 `ShipmentMap`
- [x] 6.2 已发货且坐标完整时展示路线和当前位置
- [x] 6.3 物流异常时地图展示异常状态并保留最近位置
- [x] 6.4 地图不可用时仍展示物流摘要和时间线

## 7. 管理员订单详情接入

- [x] 7.1 更新 `AdminOrders.vue`，在订单详情弹窗展示 `ShipmentMap`
- [x] 7.2 管理员推进物流后同步刷新地图当前位置
- [x] 7.3 管理员标记异常后同步刷新地图异常状态
- [x] 7.4 终态不可推进时页面保留当前地图状态
- [x] 7.5 legacy 或缺坐标订单显示地图降级说明

## 8. 后端测试

- [x] 8.1 为坐标完整 shipment 生成地图模拟数据添加单元测试
- [x] 8.2 为缺少发货地坐标添加降级测试
- [x] 8.3 为缺少收货地坐标添加降级测试
- [x] 8.4 为 SHIPPED / IN_TRANSIT / OUT_FOR_DELIVERY / DELIVERED 状态进度映射添加测试
- [x] 8.5 为 EXCEPTION 状态位置和说明添加测试
- [x] 8.6 为 legacy shipment 不返回可用地图路线添加测试
- [x] 8.7 为模拟推进后返回最新地图模拟数据添加测试

## 9. 前端验证与构建

- [x] 9.1 在 `backend/` 运行 `mvn.cmd test`
- [x] 9.2 在 `frontend/` 运行 `npm.cmd run build`
- [ ] 9.3 手动验证：买家订单详情展示物流地图路线和当前位置
- [ ] 9.4 手动验证：卖家订单管理展示物流地图路线和当前位置
- [ ] 9.5 手动验证：管理员推进物流后地图当前位置变化
- [ ] 9.6 手动验证：管理员标记异常后地图展示异常状态
- [ ] 9.7 手动验证：未配置地图 Key 时页面降级但物流时间线可用
- [ ] 9.8 手动验证：历史物流单号和缺坐标订单不显示空白地图
