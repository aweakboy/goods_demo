## 1. 配置

- [x] 1.1 在 `application.yml` 中新增 `app.order.payment-timeout-minutes: 30`
- [x] 1.2 在 `TradingApplication` 上添加 `@EnableScheduling` 注解

## 2. 数据模型

- [x] 2.1 在 `Order` 实体中新增 `expiredAt` 字段（`LocalDateTime`，nullable，列名 `expired_at`）

## 3. 订单服务

- [x] 3.1 在 `OrderService.createOrder()` 中，创建订单时设置 `expiredAt = LocalDateTime.now() + timeoutMinutes`（从配置注入）
- [x] 3.2 在 `OrderService` 中提取内部方法 `doCancelOrder(Order order)`，将状态变更和库存恢复逻辑移入，供 `cancel()` 和定时任务共用
- [x] 3.3 在 `OrderRepository` 中新增查询方法 `findByStatusAndExpiredAtBefore(OrderStatus status, LocalDateTime time)`

## 4. 定时任务

- [x] 4.1 创建 `OrderTimeoutScheduler`，每 60 秒执行一次，查询所有超时的 PENDING_PAYMENT 订单并逐一调用 `doCancelOrder()`

## 5. 验证

- [ ] 5.1 启动应用，手动将某订单的 `expired_at` 改为过去时间，等待下一次定时任务触发，确认订单变为 CANCELLED 且库存恢复
