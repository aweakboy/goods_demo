## MODIFIED Requirements

### Requirement: 订单状态流转
系统 SHALL 按照固定状态机管理订单生命周期：PENDING_PAYMENT → PAID → SHIPPED → COMPLETED → CANCELLED，并支持退款相关状态流转。

#### Scenario: 卖家标记发货
- **WHEN** 卖家对 PAID 状态订单调用发货接口
- **THEN** 系统将订单状态更新为 SHIPPED

#### Scenario: 买家确认收货
- **WHEN** 买家对 SHIPPED 状态订单确认收货
- **THEN** 系统将订单状态更新为 COMPLETED

#### Scenario: 取消 PENDING_PAYMENT 订单
- **WHEN** 买家取消 PENDING_PAYMENT 状态的订单
- **THEN** 系统将订单状态更新为 CANCELLED，并恢复库存

#### Scenario: 买家申请退款
- **WHEN** 买家对 PAID / SHIPPED / COMPLETED（7天内）订单提交退款申请
- **THEN** 系统将订单状态更新为 REFUND_REQUESTED

#### Scenario: 管理员批准退款
- **WHEN** 管理员批准 REFUND_REQUESTED 订单的退款请求
- **THEN** 系统调用支付宝退款并将订单状态更新为 REFUNDED

#### Scenario: 管理员拒绝退款
- **WHEN** 管理员拒绝 REFUND_REQUESTED 订单的退款请求
- **THEN** 系统将订单状态更新为 REFUND_REJECTED，保存拒绝理由
