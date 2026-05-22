## Context

当前订单发货由 `OrderService.ship()` 直接把订单状态从 `PAID` 改为 `SHIPPED`，并在 `orders.tracking_number` 保存一个文本物流单号。这个模型无法表达物流公司、发货地、收货地、包裹当前状态、轨迹事件、预计送达和异常状态，也无法支撑真实一点的发货演示。

项目已经具备店铺结构化地址、收货地址快照、地址校验和地图坐标基础。本次设计在此基础上增加独立 shipment 模型，让订单状态继续保持业务主状态，让 shipment 承载物流履约细节。

## Goals / Non-Goals

**Goals:**

- 把单字段物流单号升级为独立发货记录和物流轨迹。
- 支持卖家创建发货记录，记录物流公司、运单号、发货地址快照、收货地址快照、预计送达时间和当前物流状态。
- 支持模拟物流状态推进，覆盖已揽收、运输中、派送中、已签收和异常。
- 让买家订单详情、卖家订单页、管理员订单详情都能查看物流摘要和轨迹。
- 兼容历史订单的 `trackingNumber` 字段，不要求一次性迁移旧数据。

**Non-Goals:**

- 本次不接入真实快递公司或第三方物流查询 API。
- 本次不做多包裹拆单发货；一笔订单最多一个 shipment。
- 本次不做骑手/司机调度、路线规划或实时地图轨迹。
- 本次不改变支付、退款和库存扣减的核心流程。

## Decisions

### 独立 shipment 表，而不是继续扩展 orders

新增 `Shipment` 实体保存发货主记录，新增 `ShipmentEvent` 实体保存物流时间线。`orders.tracking_number` 保留为历史兼容字段，也可在发货时同步写入最新运单号，方便旧页面或旧数据继续展示。

备选方案是继续在 `orders` 表增加多个物流字段，但轨迹事件是一对多数据，后续接入真实物流查询也需要保留多条事件。独立模型更符合履约领域边界。

### 订单状态与物流状态分离

订单状态仍使用 `PAID`、`SHIPPED`、`COMPLETED` 等主流程状态；shipment 状态使用 `SHIPPED`、`IN_TRANSIT`、`OUT_FOR_DELIVERY`、`DELIVERED`、`EXCEPTION` 表示物流过程。

这样可以避免把物流细节塞进订单状态枚举，也能让退款、取消、完成等订单状态保持相对稳定。

### 发货时快照地址，而不是每次读取当前店铺地址

shipment 创建时保存发货地址快照和收货地址快照，包括文本地址、经纬度和地址校验状态。后续店铺地址或用户地址变更不影响历史发货记录。

这与订单创建时保存收货地址快照的原则一致，也便于管理员复核历史订单。

### 先提供模拟物流服务，后续再接入真实物流适配器

新增 `ShipmentSimulationService` 或等价服务，根据当前 shipment 状态推进下一条事件。模拟功能通过配置开关控制，开发环境可以开启，生产环境默认关闭或仅限管理员触发。

真实物流 API 后续可新增 `LogisticsTrackingClient` 接口和 provider 实现，不影响本次创建发货记录和展示轨迹的前后端契约。

### 运单号唯一性按物流公司维度校验

系统校验 `(carrierCode, trackingNumber)` 唯一，避免同一物流公司的同一运单号绑定多个订单。不同物流公司理论上可能存在相同单号，因此不使用单独 trackingNumber 全局唯一。

### 预计送达时间先用规则估算

预计送达时间根据配置的默认配送天数生成；若店铺和收货地址都有经纬度，可在后续实现中扩展为按距离粗略分档。本次不要求精确路线距离计算。

## Risks / Trade-offs

- [Risk] 订单状态和 shipment 状态可能不一致。→ 发货、模拟推进、确认收货必须通过统一 service 修改，测试覆盖状态同步规则。
- [Risk] 旧订单只有 `trackingNumber` 没有 shipment。→ 响应层返回 legacy logistics summary，页面继续展示文本物流单号。
- [Risk] 退款流程与发货流程冲突。→ 对 `REFUND_REQUESTED`、`REFUNDED` 等非 `PAID` 状态禁止创建发货；已发货订单仍按现有退款规则处理。
- [Risk] 模拟接口被生产误用。→ 增加 `shipping.simulation.enabled` 配置，关闭时返回业务错误。
- [Risk] 后续接入真实物流 API 需要字段映射。→ 提前保留 carrierCode、status、eventTime、location、description 等通用字段。

## Migration Plan

1. 新增 shipment 和 shipment event 表，保留 orders 表现有字段。
2. 发货接口改为创建 shipment，同时将订单状态更新为 `SHIPPED` 并同步 `orders.tracking_number`。
3. 订单响应新增 shipment summary/events 字段；旧订单无 shipment 时从 `orders.tracking_number` 构造兼容展示。
4. 前端先读取新 shipment 字段；没有新字段时回退展示旧物流单号。
5. 如需回滚，保留 orders.tracking_number 后旧发货展示仍可工作；新增表可不参与读取。

## Open Questions

- 物流公司列表先使用固定枚举还是数据库配置表？建议本次先用后端固定列表和前端选项，后续有运营需求再做后台配置。
- 模拟物流推进由卖家触发、管理员触发，还是定时任务自动触发？建议本次提供后端 service 和管理员/开发触发接口，避免后台定时任务影响演示可控性。
