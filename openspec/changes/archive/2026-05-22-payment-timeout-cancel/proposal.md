## Why

订单创建后若买家长时间未付款，库存被占用但无法销售，影响其他买家购买。引入支付超时自动取消机制，超过设定时间（默认 30 分钟）仍处于 PENDING_PAYMENT 状态的订单自动取消并恢复库存。

## What Changes

- **新增**：定时任务 `OrderTimeoutScheduler`，每分钟扫描超时未付款订单并批量取消
- **修改**：`Order` 实体新增 `expiredAt` 字段，记录支付截止时间
- **修改**：`OrderService.createOrder()` 在创建订单时设置 `expiredAt = 创建时间 + 超时时长`
- **新增**：`application.yml` 配置 `app.order.payment-timeout-minutes`（默认 30）

## Capabilities

### New Capabilities

- `order-timeout`: 支付超时自动取消——定时扫描过期订单，执行取消并恢复库存

### Modified Capabilities

- `order-management`: 创建订单时需设置支付截止时间；订单取消新增"超时自动取消"触发来源

## Impact

- **后端**：`Order` 实体、`OrderService`、新增 `OrderTimeoutScheduler`
- **配置**：`application.yml` 新增超时时长配置
- **数据库**：`orders` 表新增 `expired_at DATETIME` 列（JPA update 模式自动添加）
- **无前端改动**：超时取消纯后端行为，前端刷新订单时自然看到 CANCELLED 状态
