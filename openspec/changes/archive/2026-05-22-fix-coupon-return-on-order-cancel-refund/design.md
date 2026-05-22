## Context

优惠券功能在创建订单时会把买家优惠券从 `UNUSED` 标记为 `USED`，并记录 `usedOrderId`。当前 `OrderService.doCancelOrder()` 只负责把 `PENDING_PAYMENT` 订单改为 `CANCELLED` 并恢复库存；`approveRefund()` 在支付宝退款成功后把订单改为 `REFUNDED`。这两个终态都没有释放优惠券占用，导致未完成交易或全额退款后的优惠券仍不可用。

订单表已经保存了优惠券快照字段，买家优惠券表也已经有 `status`、`usedAt`、`usedOrderId`，因此本修复应复用现有数据结构。优惠券过期展示由 `BuyerCouponService.displayStatus()` 派生，退回后的优惠券如果已经超过有效期，应在列表中表现为 `EXPIRED`，而不是重新变成可使用券。

## Goals / Non-Goals

**Goals:**

- 使用优惠券的 `PENDING_PAYMENT` 订单被买家取消时，退回该订单占用的买家优惠券。
- 支付超时自动取消复用同一取消逻辑，退回该订单占用的买家优惠券。
- 管理员批准退款且退款调用成功后，退回该订单占用的买家优惠券。
- 退回操作必须只作用于当前订单占用的买家优惠券，并支持重复调用不破坏数据。
- 保持订单上的优惠券名称、门槛金额、抵扣金额、原始金额、优惠金额和实付金额快照不变。
- 用自动化测试覆盖取消、超时取消、退款批准、退款拒绝和幂等退回场景。

**Non-Goals:**

- 不改变优惠券领取数量 `claimedQuantity`，退回已领取券不是重新发放券。
- 不支持部分退款场景的优惠券按比例返还。
- 不新增前端页面；现有“我的优惠券”和结算页通过后端状态自然反映退回结果。
- 不改变退款申请、退款拒绝、订单支付和订单发货的状态规则。

## Decisions

### D1：在 `BuyerCouponService` 中封装订单维度的退回方法

新增类似 `releaseForOrder(Order order)` 或 `releaseForOrder(Long buyerCouponId, Long orderId)` 的服务方法，负责读取买家优惠券、校验 `status == USED` 且 `usedOrderId` 等于当前订单 ID，然后把状态改回 `UNUSED` 并清空 `usedAt`、`usedOrderId`。

备选方案是在 `OrderService` 中直接操作 `BuyerCouponRepository`。不采用该方案，因为优惠券状态、过期展示和可用性校验都已经集中在 `BuyerCouponService`，继续封装可以减少重复状态判断。

### D2：取消和退款成功路径在同一事务内释放优惠券

`OrderService.doCancelOrder()` 在确认订单仍为 `PENDING_PAYMENT` 后，恢复库存并释放优惠券，再保存 `CANCELLED` 状态。买家主动取消和超时自动取消都复用该方法，因此行为一致。

`OrderService.approveRefund()` 只在 `PaymentService.refund()` 成功返回后释放优惠券并保存 `REFUNDED` 状态。若支付宝退款失败抛出异常，事务回滚，订单保持 `REFUND_REQUESTED`，优惠券也保持 `USED`。

备选方案是在退款申请时立即释放优惠券。不采用该方案，因为退款申请可能被拒绝，交易仍然有效，优惠券应继续被该订单消耗。

### D3：优惠券退回只清理占用，不改变领取库存和订单快照

退回时不减少优惠券活动的 `claimedQuantity`，因为买家仍持有同一张领取记录。订单上的优惠券快照字段不清空，历史订单详情仍展示创建订单时的抵扣信息。

如果优惠券活动已经过期，买家优惠券状态仍可恢复为 `UNUSED`，列表层按现有规则展示为 `EXPIRED`，且不会进入可用优惠券列表。

### D4：退回逻辑按订单绑定做幂等保护

退回方法只在买家优惠券当前为 `USED` 且 `usedOrderId` 等于当前订单 ID 时执行更新。若重复取消、定时任务重试或退款审批重试触发同一释放逻辑，方法应直接返回，不修改已经退回的券，也不影响其他订单占用的券。

## Risks / Trade-offs

- **取消和退款逻辑分散导致遗漏** → 将买家主动取消、超时取消统一落到 `doCancelOrder()`，退款只在 `approveRefund()` 成功路径调用退回方法。
- **退款外部调用成功但数据库保存失败** → 保持现有事务模式；本变更不扩大外部退款的一致性问题。若保存失败，管理员仍需按现有异常处理和重试流程处理。
- **过期券退回后用户误以为可再次使用** → 后端 `listUsable()` 继续过滤过期券，`listMine()` 展示派生 `EXPIRED` 状态。
- **并发状态流转重复释放** → 退回方法使用 `usedOrderId` 和 `USED` 状态共同保护，必要时沿用现有 `findByIdForUpdate` 加锁读取。

## Migration Plan

无需数据库迁移。部署后，新发生的取消和退款成功会自动释放对应优惠券。历史上已经进入 `CANCELLED` 或 `REFUNDED` 但优惠券仍为 `USED` 的订单不在本次自动修复范围内，可后续按数据修复脚本单独处理。

## Testing Strategy

- 为 `BuyerCouponService` 增加退回服务测试：匹配订单占用时退回、重复退回幂等、`usedOrderId` 不匹配时不修改。
- 为 `OrderService` 增加取消订单测试：使用优惠券的 `PENDING_PAYMENT` 订单取消后恢复库存并退回优惠券。
- 为超时取消路径增加测试或复用服务测试：`doCancelOrder()` 被系统路径调用时同样退回优惠券。
- 为退款审批增加测试：退款成功后退回优惠券；退款拒绝或退款调用失败时优惠券保持 `USED`。
- 运行 `mvn.cmd test`；前端未改动时不要求新增前端自动化测试。

## Open Questions

- 是否需要一次性修复历史异常数据。本 change 默认只修复新状态流转，历史数据修复可另开变更。
