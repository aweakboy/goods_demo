## ADDED Requirements

### Requirement: 创建发货记录
系统 SHALL 在卖家对已付款订单发货时创建一条 shipment 记录，记录物流公司、运单号、发货地址快照、收货地址快照、发货时间、预计送达时间和当前物流状态。

#### Scenario: 发货成功
- **WHEN** 卖家对属于自己店铺商品的 PAID 订单提交物流公司和运单号
- **THEN** 系统创建 shipment，状态为 SHIPPED，记录发货地址和收货地址快照，并将订单状态更新为 SHIPPED

#### Scenario: 发货信息缺失
- **WHEN** 卖家发货时未提交物流公司或运单号
- **THEN** 系统返回 400 状态码，不创建 shipment，不修改订单状态

#### Scenario: 非已付款订单发货
- **WHEN** 卖家对非 PAID 状态订单提交发货
- **THEN** 系统返回 400 状态码，不创建 shipment

#### Scenario: 重复发货
- **WHEN** 卖家对已经存在 shipment 的订单再次提交发货
- **THEN** 系统返回 400 状态码，不创建第二条 shipment

#### Scenario: 运单号重复
- **WHEN** 卖家提交的物流公司和运单号已经绑定到其他订单
- **THEN** 系统返回 400 状态码，提示运单号已存在

### Requirement: 发货地址快照
系统 SHALL 在 shipment 中保存发货地和收货地快照，快照 MUST 独立于店铺地址和订单地址后续变更。

#### Scenario: 保存发货地址快照
- **WHEN** 卖家发货成功
- **THEN** shipment 保存当前店铺地址、店铺经纬度、收货地址、收货经纬度和地址校验状态

#### Scenario: 店铺地址后续变更
- **WHEN** shipment 创建后卖家修改店铺地址
- **THEN** 已创建 shipment 的发货地址快照保持不变

#### Scenario: 历史地址缺少经纬度
- **WHEN** 订单或店铺历史数据缺少经纬度
- **THEN** 系统仍允许按文本地址创建 shipment，并将缺失的坐标字段保持为空

### Requirement: 物流状态与轨迹事件
系统 SHALL 为每条 shipment 维护当前物流状态和按时间排序的轨迹事件。

#### Scenario: 创建初始轨迹
- **WHEN** shipment 创建成功
- **THEN** 系统创建第一条轨迹事件，描述卖家已发货或快递已揽收

#### Scenario: 推进到运输中
- **WHEN** 模拟物流服务将 SHIPPED 状态 shipment 推进一段
- **THEN** shipment 状态更新为 IN_TRANSIT，并新增运输中轨迹事件

#### Scenario: 推进到派送中
- **WHEN** 模拟物流服务将 IN_TRANSIT 状态 shipment 推进一段
- **THEN** shipment 状态更新为 OUT_FOR_DELIVERY，并新增派送中轨迹事件

#### Scenario: 推进到已签收
- **WHEN** 模拟物流服务将 OUT_FOR_DELIVERY 状态 shipment 推进一段
- **THEN** shipment 状态更新为 DELIVERED，记录签收时间，并新增已签收轨迹事件

#### Scenario: 标记物流异常
- **WHEN** 模拟物流服务或管理操作将 shipment 标记为 EXCEPTION 并提供原因
- **THEN** shipment 状态更新为 EXCEPTION，并新增包含异常原因的轨迹事件

### Requirement: 模拟物流控制
系统 SHALL 通过配置开关控制模拟物流推进能力，关闭时不得改变 shipment 状态。

#### Scenario: 模拟物流开启
- **WHEN** `shipping.simulation.enabled` 为 true 且调用模拟推进能力
- **THEN** 系统按当前状态生成下一条物流轨迹并更新 shipment 状态

#### Scenario: 模拟物流关闭
- **WHEN** `shipping.simulation.enabled` 为 false 且调用模拟推进能力
- **THEN** 系统返回业务错误，不修改 shipment 状态和轨迹事件

#### Scenario: 终态 shipment 推进
- **WHEN** 模拟物流服务尝试推进 DELIVERED 或 EXCEPTION 状态 shipment
- **THEN** 系统保持原状态并返回明确提示

### Requirement: 物流查询与历史兼容
系统 SHALL 向买家、卖家和管理员返回订单的物流摘要和轨迹；历史订单无 shipment 但有旧物流单号时 MUST 继续展示旧物流信息。

#### Scenario: 查看完整物流
- **WHEN** 买家、卖家或管理员查看存在 shipment 的订单详情
- **THEN** 系统返回物流公司、运单号、物流状态、预计送达时间和轨迹事件列表

#### Scenario: 查看历史物流单号
- **WHEN** 订单没有 shipment 但 `trackingNumber` 不为空
- **THEN** 系统返回兼容物流摘要，展示旧物流单号，并标记轨迹不可用

#### Scenario: 无物流信息
- **WHEN** 订单尚未发货且不存在 shipment
- **THEN** 系统不返回物流轨迹，页面展示未发货状态
