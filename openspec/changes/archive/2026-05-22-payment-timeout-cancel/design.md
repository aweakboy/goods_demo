## Context

当前订单在 PENDING_PAYMENT 状态可以无限期停留，库存被锁定但不释放。`OrderService.cancel()` 已实现取消逻辑（改状态 + 恢复库存），可直接复用。技术栈为 Spring Boot 3.2，无 Redis/MQ。

## Goals / Non-Goals

**Goals:**
- 超时未付款订单自动取消并恢复库存
- 超时时长可配置
- 实现简单，无需引入新中间件

**Non-Goals:**
- 不实现精确到秒的实时取消（允许约 1 分钟延迟）
- 不发送超时通知（短信/邮件）
- 不处理已发起支付宝跳转但未付款的中间状态区分

## Decisions

### D1：使用 Spring @Scheduled 定时扫描，不引入 MQ/Redis

**方案对比：**

| 方案 | 精度 | 复杂度 | 新依赖 |
|------|------|--------|--------|
| @Scheduled 扫描 | ~1分钟 | 极低 | 无 |
| Redis 延迟队列 | 秒级 | 高 | Redis |
| RabbitMQ 死信队列 | 秒级 | 高 | RabbitMQ |

选择 `@Scheduled`：对电商场景"30分钟内付款"而言，1分钟误差完全可接受，零额外依赖。

### D2：Order 实体新增 expiredAt 字段，而非运行时计算

将截止时间持久化到数据库，可以直接用 `WHERE status = 'PENDING_PAYMENT' AND expired_at < NOW()` 一条 SQL 查出所有超时订单，高效且无需在代码里计算。

### D3：复用 OrderService.cancel() 逻辑，不重复实现

现有 `cancel()` 已处理状态变更和库存恢复，定时任务直接调用，保证行为一致。唯一区别：定时任务调用时不需要验证买家身份（系统触发）。需在 `OrderService` 中拆出内部 `cancelOrder(Order)` 方法供两处调用。

### D4：@EnableScheduling 开关放在主应用类

Spring Boot 需要 `@EnableScheduling` 才能激活定时任务，加在 `TradingApplication` 上，不引入额外配置类。

## Risks / Trade-offs

- **多实例部署重复执行** → 若未来水平扩展多个实例，同一批订单可能被多个实例同时取消。缓解：利用数据库事务 + 状态检查（cancel 前再次确认 status == PENDING_PAYMENT），保证幂等；生产环境可用 ShedLock 或 Quartz 集群锁，当前阶段不需要。
- **大量超时订单积压时扫描慢** → 为 `orders(status, expired_at)` 建复合索引。
