## MODIFIED Requirements

### Requirement: 订单状态流转
系统 SHALL 按照固定状态机管理订单生命周期：PENDING_PAYMENT → PAID → SHIPPED → COMPLETED，以及 PENDING_PAYMENT → CANCELLED；订单进入 SHIPPED 时 MUST 关联发货记录或历史物流单号。

#### Scenario: 卖家标记发货
- **WHEN** 卖家对 PAID 状态订单调用发货接口并提交有效物流公司和运单号
- **THEN** 系统创建 shipment，记录物流快照和初始轨迹，将订单状态更新为 SHIPPED，并保存物流摘要

#### Scenario: 买家确认已签收订单
- **WHEN** 买家对 SHIPPED 状态且 shipment 状态为 DELIVERED 的订单确认收货
- **THEN** 系统将订单状态更新为 COMPLETED

#### Scenario: 买家手动确认未签收订单
- **WHEN** 买家对 SHIPPED 状态但 shipment 尚未 DELIVERED 的订单确认收货
- **THEN** 系统允许确认收货并将订单状态更新为 COMPLETED，同时保留原 shipment 状态和轨迹

#### Scenario: 取消 PENDING_PAYMENT 订单
- **WHEN** 买家取消 PENDING_PAYMENT 状态的订单
- **THEN** 系统将订单状态更新为 CANCELLED，并恢复库存

### Requirement: 订单历史查询
系统 SHALL 允许买家查看自己的全部订单列表，支持按状态筛选；订单详情中每个商品条目显示所属店铺名称，并在订单已发货时展示物流摘要和轨迹。

#### Scenario: 查看全部订单
- **WHEN** 买家访问订单列表页
- **THEN** 系统返回该买家所有订单，按创建时间倒序

#### Scenario: 按状态筛选订单
- **WHEN** 买家选择指定状态筛选
- **THEN** 系统只返回该状态的订单

#### Scenario: 查看订单详情
- **WHEN** 买家查看某笔订单详情
- **THEN** 系统返回订单中所有商品、价格、数量、收货地址、当前状态、物流摘要和轨迹，以及每个商品条目所属的店铺名称

#### Scenario: 查看未发货订单详情
- **WHEN** 买家查看尚未发货的订单详情
- **THEN** 系统返回订单基本信息且不返回物流轨迹

#### Scenario: 查看历史物流单号订单
- **WHEN** 买家查看只有旧 `trackingNumber` 且没有 shipment 的已发货订单详情
- **THEN** 系统展示旧物流单号，并提示暂无物流轨迹
