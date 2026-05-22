## 1. 数据模型

- [x] 1.1 在 `OrderStatus` 枚举中新增 `REFUND_REQUESTED`、`REFUNDED`、`REFUND_REJECTED` 三个状态
- [x] 1.2 在 `Order` 实体中新增 `refundReason`（String，length=500）和 `refundRejectReason`（String，length=500）字段
- [x] 1.3 在 `application.yml` 中新增 `app.order.refund-window-days: 7`

## 2. 支付服务

- [x] 2.1 在 `PaymentService` 中新增 `refund(String alipayTradeNo, BigDecimal amount, String outRequestNo)` 方法，调用支付宝 `alipay.trade.refund` 接口

## 3. 订单服务

- [x] 3.1 在 `OrderService` 中新增 `requestRefund(Long orderId, Long buyerId, String reason)` 方法：校验状态（PAID/SHIPPED/COMPLETED）及 COMPLETED 订单的 7 天时间窗口，更新状态为 REFUND_REQUESTED
- [x] 3.2 在 `OrderService` 中新增 `approveRefund(Long orderId)` 方法：调用 `PaymentService.refund()`，成功后更新状态为 REFUNDED
- [x] 3.3 在 `OrderService` 中新增 `rejectRefund(Long orderId, String reason)` 方法：更新状态为 REFUND_REJECTED，保存拒绝理由
- [x] 3.4 在 `OrderRepository` 中新增 `findByStatus(OrderStatus status)` 查询方法（供管理员查待审批列表）

## 4. 后端接口

- [x] 4.1 在 `OrderController` 中新增 `POST /api/v1/orders/{id}/refund-request` 接口，买家提交退款申请（Body：`{ reason: String }`）
- [x] 4.2 在 `AdminController` 中新增 `GET /api/v1/admin/orders/refund-requests` 接口，返回所有 REFUND_REQUESTED 订单
- [x] 4.3 在 `AdminController` 中新增 `POST /api/v1/admin/orders/{id}/refund-approve` 接口
- [x] 4.4 在 `AdminController` 中新增 `POST /api/v1/admin/orders/{id}/refund-reject` 接口（Body：`{ reason: String }`）

## 5. 前端 — 买家侧

- [x] 5.1 在 `OrderDetailView.vue` 中，对 PAID / SHIPPED / COMPLETED 状态订单显示"申请退款"按钮
- [x] 5.2 新增退款申请弹窗，包含退款原因输入框，提交后刷新订单状态
- [x] 5.3 在订单状态标签中增加退款相关状态的中文映射和颜色（REFUND_REQUESTED=橙色、REFUNDED=绿色、REFUND_REJECTED=红色）
- [x] 5.4 在 `order.js` API 文件中新增 `refundRequest(id, reason)` 方法

## 6. 前端 — 管理员侧

- [x] 6.1 新增 `admin.js` 退款相关 API 方法：`refundRequests()`、`approveRefund(id)`、`rejectRefund(id, reason)`
- [x] 6.2 新增 `AdminRefunds.vue` 页面，展示待审批退款订单列表，含批准/拒绝操作
- [x] 6.3 在 `router/index.js` 中注册 `/admin/refunds` 路由
- [x] 6.4 在 `AdminLayout.vue`（或管理员导航菜单）中添加"退款管理"入口
