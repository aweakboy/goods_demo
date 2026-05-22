## 1. 优惠券退回服务

- [x] 1.1 在 `BuyerCouponService` 中新增按订单退回优惠券的方法，输入当前订单的 `buyerCouponId` 和订单 ID
- [x] 1.2 退回方法仅在买家优惠券状态为 `USED` 且 `usedOrderId` 等于当前订单 ID 时执行更新
- [x] 1.3 退回时将买家优惠券状态恢复为 `UNUSED`，并清空 `usedAt` 和 `usedOrderId`
- [x] 1.4 确保退回方法不修改优惠券活动的 `claimedQuantity`，不清空订单上的优惠券快照字段
- [x] 1.5 确保已过期优惠券退回后仍通过现有列表规则展示为 `EXPIRED`，且不进入可用优惠券列表

## 2. 订单状态流转接入

- [x] 2.1 在 `OrderService.doCancelOrder()` 中接入优惠券退回逻辑，覆盖买家主动取消 `PENDING_PAYMENT` 订单
- [x] 2.2 确认支付超时自动取消路径复用 `doCancelOrder()` 后同样退回优惠券
- [x] 2.3 在 `OrderService.approveRefund()` 中仅在 `PaymentService.refund()` 成功后退回优惠券并保存 `REFUNDED` 状态
- [x] 2.4 确认退款申请 `requestRefund()` 不退回优惠券，退款拒绝 `rejectRefund()` 不退回优惠券
- [x] 2.5 确认取消和退款成功后订单原始金额、优惠金额、实付金额、优惠券名称和金额快照保持不变

## 3. 后端自动化测试

- [x] 3.1 添加 `BuyerCouponService` 测试：匹配 `usedOrderId` 的 `USED` 优惠券可退回为 `UNUSED`
- [x] 3.2 添加 `BuyerCouponService` 测试：重复退回保持幂等，不修改已经退回的优惠券
- [x] 3.3 添加 `BuyerCouponService` 测试：`usedOrderId` 不匹配时不修改其他订单占用的优惠券
- [x] 3.4 添加 `OrderService` 测试：使用优惠券的 `PENDING_PAYMENT` 订单取消后恢复库存并退回优惠券
- [x] 3.5 添加 `OrderService` 测试：系统路径调用 `doCancelOrder()` 时同样退回优惠券
- [x] 3.6 添加 `OrderService` 测试：退款审批成功后订单变为 `REFUNDED` 且优惠券退回
- [x] 3.7 添加 `OrderService` 测试：退款拒绝后优惠券保持 `USED`
- [x] 3.8 添加 `OrderService` 测试：支付渠道退款失败时订单保持 `REFUND_REQUESTED` 且优惠券保持 `USED`
- [x] 3.9 添加优惠券列表或服务测试：退回时已过期的优惠券展示为 `EXPIRED` 且不会被 `listUsable()` 返回

## 4. 验证

- [x] 4.1 运行 `mvn.cmd test`
- [x] 4.2 运行 `openspec validate fix-coupon-return-on-order-cancel-refund`
- [ ] 4.3 手动验证：买家领取优惠券并创建待付款订单后取消订单，确认“我的优惠券”重新显示该券且未过期时可再次结算使用
- [ ] 4.4 手动验证：买家使用优惠券的已付款订单申请退款，管理员拒绝退款后该券仍不回到可用列表
- [ ] 4.5 手动验证：买家使用优惠券的已付款订单申请退款，管理员批准退款成功后该券回到“我的优惠券”；若已过期则显示为过期且不可再次使用
