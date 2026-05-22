## Why

买家完成支付后，若商品存在问题或其他原因需要退款，当前系统无任何退款渠道，只能线下处理。引入退款申请与管理员审批流程，买家可对已付款的任意阶段订单提交退款申请，管理员审核后由系统调用支付宝退款接口完成资金返还。

## What Changes

- **新增**：订单状态 `REFUND_REQUESTED`（退款申请中）、`REFUNDED`（已退款）、`REFUND_REJECTED`（退款被拒绝）
- **新增**：`Order` 实体新增 `refundReason`（退款原因）、`refundRejectReason`（拒绝原因）字段
- **新增**：买家接口 `POST /api/v1/orders/{id}/refund-request`，提交退款申请（适用于 PAID / SHIPPED / COMPLETED 状态；COMPLETED 订单仅限 7 天内）
- **新增**：管理员接口 `POST /api/v1/admin/orders/{id}/refund-approve`，审批通过并调用支付宝退款 API
- **新增**：管理员接口 `POST /api/v1/admin/orders/{id}/refund-reject`，拒绝退款并附理由
- **新增**：管理员后台新增退款管理页，展示所有 REFUND_REQUESTED 订单
- **新增**：`PaymentService.refund()` 方法，调用支付宝 `alipay.trade.refund` 接口
- **修改**：买家订单详情页，符合条件的订单显示"申请退款"按钮及退款状态

## Capabilities

### New Capabilities

- `order-refund`: 退款申请与审批——买家提交、管理员审核、支付宝退款

### Modified Capabilities

- `order-management`: 订单状态机新增退款相关状态；买家可对已付款订单申请退款
- `admin-order-oversight`: 管理员新增退款审批操作

## Impact

- **后端**：`OrderStatus` 枚举、`Order` 实体、`OrderService`、`PaymentService`、`AdminController`、`OrderController`
- **前端**：`OrderDetailView.vue`、新增 `AdminRefunds.vue`、`router/index.js`
- **数据库**：`orders` 表新增 `refund_reason`、`refund_reject_reason` 列（JPA update 自动添加）
- **支付宝**：使用 `alipay.trade.refund` API，需订单有 `alipayTradeNo`（即已通过支付宝付款的订单）
