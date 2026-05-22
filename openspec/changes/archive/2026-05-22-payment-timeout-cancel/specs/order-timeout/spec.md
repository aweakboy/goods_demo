## ADDED Requirements

### Requirement: 支付超时自动取消
系统 SHALL 定期扫描超过支付截止时间仍处于 PENDING_PAYMENT 状态的订单，将其自动取消并恢复库存。

#### Scenario: 超时订单自动取消
- **WHEN** 定时任务运行时发现存在 `expired_at < 当前时间` 且状态为 PENDING_PAYMENT 的订单
- **THEN** 系统将这些订单状态更新为 CANCELLED，并将每个订单中所有商品的库存恢复

#### Scenario: 未超时订单不受影响
- **WHEN** 定时任务运行时订单的 `expired_at > 当前时间`
- **THEN** 该订单不被取消，状态保持不变

#### Scenario: 已取消或已付款订单不被重复处理
- **WHEN** 定时任务运行时订单状态不为 PENDING_PAYMENT
- **THEN** 该订单不被处理（幂等保证）
