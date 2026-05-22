## ADDED Requirements

### Requirement: 管理员退款审批
系统 SHALL 为管理员提供退款申请管理页面，展示所有待审批的退款订单，并支持批准或拒绝操作。

#### Scenario: 查看待退款订单列表
- **WHEN** 管理员访问退款管理页面
- **THEN** 系统返回所有 REFUND_REQUESTED 状态的订单，展示订单号、买家、金额、退款原因、申请时间

#### Scenario: 批准退款
- **WHEN** 管理员点击"批准"并确认
- **THEN** 系统调用支付宝退款接口并将订单标记为 REFUNDED，列表中该订单消失

#### Scenario: 拒绝退款
- **WHEN** 管理员填写拒绝理由并点击"拒绝"
- **THEN** 系统将订单标记为 REFUND_REJECTED，列表中该订单消失
