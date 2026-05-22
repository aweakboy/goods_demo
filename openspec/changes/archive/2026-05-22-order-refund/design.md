## Context

当前订单状态机为：PENDING_PAYMENT → PAID → SHIPPED → COMPLETED → CANCELLED。支付宝退款需要 `alipayTradeNo`（已在 Order 实体中存储），因此只有通过支付宝完成付款的订单（PAID/SHIPPED/COMPLETED）才能退款。PENDING_PAYMENT 订单直接取消即可，无需退款流程。

## Goals / Non-Goals

**Goals:**
- 买家可对 PAID / SHIPPED / COMPLETED 订单提交退款申请（COMPLETED 限 7 天内）
- 管理员审批后自动调用支付宝退款接口
- 管理员可拒绝退款并填写理由

**Non-Goals:**
- 不支持部分退款（全额退款）
- 不向卖家发送退款通知（无消息系统）
- PENDING_PAYMENT 订单不走退款流程（直接取消）

## Decisions

### D1：新增三个订单状态，不复用 CANCELLED

```
PAID / SHIPPED / COMPLETED
         │  买家申请
         ▼
  REFUND_REQUESTED ──── 管理员拒绝 ──► REFUND_REJECTED（终态）
         │  管理员同意
         ▼
      REFUNDED（终态）
```

REFUNDED 与 CANCELLED 语义不同（前者有资金返还），需区分以便对账和统计。

### D2：COMPLETED 订单退款设 7 天时间窗口

COMPLETED 状态表示买家已确认收货，仍允许退款但需时间限制，默认 7 天（可配置）。通过比较 `order.createdAt`（或后续可添加 `completedAt`）与当前时间实现，暂用 `createdAt` 近似。

### D3：退款金额固定为订单全额

`alipay.trade.refund` 需传入 `refund_amount`，取 `order.totalAmount`。`out_request_no` 使用 `orderId + "-refund"` 保证唯一性。

### D4：管理员审批后立即调用支付宝 API，失败则回滚状态

审批接口在同一事务中：先改状态为 REFUNDED，再调支付宝 API。若支付宝返回失败，抛出异常触发回滚，管理员可重试。

### D5：退款原因存储在 Order 实体，不建单独表

退款场景相对简单，一单最多一次退款，直接在 Order 上加 `refundReason` 和 `refundRejectReason` 字段即可。

## Risks / Trade-offs

- **支付宝退款 API 延迟** → 沙箱环境偶尔慢，管理员看到"处理中"属正常，稍后刷新即可。
- **COMPLETED 订单用 createdAt 估算完成时间** → 存在误差（下单到完成可能跨天）。生产环境建议后续添加 `completedAt` 字段精确计算。
- **无退款通知** → 买家只能通过刷新订单状态得知退款结果，可后续接入站内消息。
