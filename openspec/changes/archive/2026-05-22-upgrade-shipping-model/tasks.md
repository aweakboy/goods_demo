## 1. 数据模型与配置

- [x] 1.1 新增 `ShipmentStatus` 枚举，覆盖 SHIPPED、IN_TRANSIT、OUT_FOR_DELIVERY、DELIVERED、EXCEPTION
- [x] 1.2 新增 `Shipment` 实体，包含 orderId、carrierCode、carrierName、trackingNumber、status、origin/destination 地址快照、shippedAt、estimatedDeliveredAt、deliveredAt
- [x] 1.3 新增 `ShipmentEvent` 实体，包含 shipmentId、status、eventTime、location、description、createdAt
- [x] 1.4 新增 `ShipmentRepository` 和 `ShipmentEventRepository`
- [x] 1.5 在数据库 schema 中补充 `shipments`、`shipment_events` 表结构和 `(carrier_code, tracking_number)` 唯一索引
- [x] 1.6 新增 `shipping.simulation.enabled`、默认配送天数等配置项

## 2. 后端 DTO 与 API 契约

- [x] 2.1 新增 `ShipRequest`，将发货请求从单个 trackingNumber 升级为 carrierCode、carrierName、trackingNumber
- [x] 2.2 新增 `ShipmentResponse` / `ShipmentEventResponse` 或等价响应 DTO
- [x] 2.3 新增物流公司选项 DTO 或常量列表，供前端发货弹窗选择
- [x] 2.4 更新买家订单详情响应，返回物流摘要和轨迹
- [x] 2.5 更新卖家订单列表/详情响应，返回物流摘要和异常状态
- [x] 2.6 更新管理员订单详情响应，返回物流摘要和轨迹
- [x] 2.7 保留旧 `trackingNumber` 字段兼容，避免历史页面或历史订单无法展示

## 3. 发货履约服务

- [x] 3.1 新增 `ShipmentService.createShipment()`，统一处理发货创建逻辑
- [x] 3.2 发货前校验订单存在、状态为 PAID、订单包含当前卖家商品
- [x] 3.3 发货前校验物流公司、物流公司名称和运单号必填
- [x] 3.4 发货前校验订单不存在既有 shipment
- [x] 3.5 发货前校验 `(carrierCode, trackingNumber)` 未被其他订单使用
- [x] 3.6 创建 shipment 时保存店铺发货地址快照和订单收货地址快照
- [x] 3.7 创建 shipment 时写入初始 shipment event
- [x] 3.8 发货成功后将订单状态更新为 SHIPPED，并同步 `orders.tracking_number`
- [x] 3.9 更新 `OrderService.ship()` 或迁移为调用 `ShipmentService`
- [x] 3.10 实现历史订单物流摘要构造：无 shipment 但有 trackingNumber 时返回 legacy summary

## 4. 物流模拟与状态流转

- [x] 4.1 新增 `ShipmentSimulationService` 或等价服务
- [x] 4.2 实现 SHIPPED → IN_TRANSIT 的模拟推进和轨迹事件
- [x] 4.3 实现 IN_TRANSIT → OUT_FOR_DELIVERY 的模拟推进和轨迹事件
- [x] 4.4 实现 OUT_FOR_DELIVERY → DELIVERED 的模拟推进、签收时间和轨迹事件
- [x] 4.5 实现标记 EXCEPTION 的服务方法和异常原因轨迹
- [x] 4.6 模拟开关关闭时拒绝推进且不修改 shipment
- [x] 4.7 DELIVERED / EXCEPTION 终态再次推进时返回明确业务提示
- [x] 4.8 新增管理员或开发用模拟推进接口，并受配置开关保护

## 5. 订单确认与相关业务兼容

- [x] 5.1 更新买家确认收货逻辑：shipment 为 DELIVERED 时正常完成订单
- [x] 5.2 保留买家对 SHIPPED 订单的手动确认能力，并保留原 shipment 状态和轨迹
- [x] 5.3 确保 PENDING_PAYMENT 订单取消恢复库存逻辑不受 shipment 新模型影响
- [x] 5.4 确保退款申请中、已退款、退款拒绝等非 PAID 状态不得创建 shipment
- [x] 5.5 确保历史 SHIPPED 订单无 shipment 时仍可确认收货

## 6. 前端卖家发货体验

- [x] 6.1 更新 `orderApi.ship()`，提交 carrierCode、carrierName、trackingNumber
- [x] 6.2 更新 `SellerOrdersView.vue` 发货弹窗，新增物流公司选择和运单号输入校验
- [x] 6.3 卖家订单卡片展示物流公司、运单号、物流状态和预计送达时间
- [x] 6.4 卖家订单卡片或详情区域展示物流轨迹时间线
- [x] 6.5 物流异常时在卖家订单页突出显示异常状态和最近异常说明
- [x] 6.6 历史订单只有旧物流单号时继续展示文本物流信息

## 7. 前端买家与管理员展示

- [x] 7.1 更新买家订单详情页，展示物流摘要和轨迹时间线
- [x] 7.2 买家订单详情页对未发货订单展示未发货状态，不显示空轨迹
- [x] 7.3 买家订单详情页对 legacy 物流单号展示“暂无物流轨迹”
- [x] 7.4 更新管理员订单详情，展示物流公司、运单号、物流状态、预计送达和轨迹
- [x] 7.5 管理员订单详情展示物流异常原因和最近更新时间
- [x] 7.6 如实现模拟推进接口，在管理员订单详情提供开发/演示用推进入口

## 8. 后端测试

- [x] 8.1 为发货成功创建 shipment、初始事件和订单状态同步添加单元测试
- [x] 8.2 为非 PAID 订单发货失败添加单元测试
- [x] 8.3 为卖家发货权限校验添加单元测试
- [x] 8.4 为物流公司或运单号缺失添加单元测试
- [x] 8.5 为订单重复发货添加单元测试
- [x] 8.6 为同物流公司运单号重复添加单元测试
- [x] 8.7 为历史 trackingNumber 兼容摘要添加单元测试
- [x] 8.8 为模拟物流状态推进全链路添加单元测试
- [x] 8.9 为模拟开关关闭和终态不可推进添加单元测试
- [x] 8.10 为买家确认收货与 shipment 状态兼容添加单元测试

## 9. 前端验证与构建

- [x] 9.1 在 `backend/` 运行 `mvn.cmd test`
- [x] 9.2 在 `frontend/` 运行 `npm.cmd run build`
- [ ] 9.3 手动验证：卖家对已付款订单选择物流公司并填写运单号后发货成功
- [ ] 9.4 手动验证：缺少物流公司或运单号时无法发货，页面展示错误
- [ ] 9.5 手动验证：发货后买家、卖家、管理员均可看到物流摘要和轨迹
- [ ] 9.6 手动验证：模拟推进后物流状态和轨迹按顺序变化
- [ ] 9.7 手动验证：物流异常状态在卖家和管理员页面清晰展示
- [ ] 9.8 手动验证：历史只有 trackingNumber 的订单仍能展示旧物流单号
