## ADDED Requirements

### Requirement: 买家申请退款
系统 SHALL 允许买家对处于 PAID、SHIPPED 或 COMPLETED 状态的订单提交退款申请，并填写退款原因。

#### Scenario: PAID 订单申请退款成功
- **WHEN** 买家对 PAID 状态的订单提交退款申请并填写原因
- **THEN** 系统将订单状态更新为 REFUND_REQUESTED，保存退款原因

#### Scenario: SHIPPED 订单申请退款成功
- **WHEN** 买家对 SHIPPED 状态的订单提交退款申请
- **THEN** 系统将订单状态更新为 REFUND_REQUESTED，保存退款原因

#### Scenario: COMPLETED 订单 7 天内申请退款成功
- **WHEN** 买家对下单时间在 7 天以内的 COMPLETED 订单提交退款申请
- **THEN** 系统将订单状态更新为 REFUND_REQUESTED，保存退款原因

#### Scenario: COMPLETED 订单超过 7 天无法退款
- **WHEN** 买家对下单时间超过 7 天的 COMPLETED 订单提交退款申请
- **THEN** 系统返回 400 状态码并提示"订单已超过退款期限"

#### Scenario: 不可退款状态申请
- **WHEN** 买家对 PENDING_PAYMENT、CANCELLED、REFUNDED、REFUND_REQUESTED 状态的订单申请退款
- **THEN** 系统返回 400 状态码并提示"当前订单状态不支持退款"

### Requirement: 管理员审批退款
系统 SHALL 允许管理员对 REFUND_REQUESTED 状态的订单进行审批，通过后自动调用支付宝退款接口。

#### Scenario: 管理员批准退款
- **WHEN** 管理员对 REFUND_REQUESTED 状态的订单点击"批准退款"
- **THEN** 系统调用支付宝退款 API 全额退款，成功后将订单状态更新为 REFUNDED

#### Scenario: 支付宝退款失败
- **WHEN** 管理员批准退款但支付宝 API 返回失败
- **THEN** 系统返回 500 状态码，订单状态回滚保持 REFUND_REQUESTED，管理员可重试

#### Scenario: 管理员拒绝退款
- **WHEN** 管理员对 REFUND_REQUESTED 状态的订单填写拒绝理由并点击"拒绝退款"
- **THEN** 系统将订单状态更新为 REFUND_REJECTED，保存拒绝理由
