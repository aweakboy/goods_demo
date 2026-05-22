## 1. 依赖与配置

- [x] 1.1 在 `pom.xml` 中添加 `alipay-sdk-java` 依赖（4.38.0.ALL）
- [x] 1.2 在 `application.yml` 中新增支付宝沙箱配置块（appId、privateKey、alipayPublicKey、notifyUrl、returnUrl、gatewayUrl）
- [x] 1.3 创建 `AlipayConfig` 类，用 `@ConfigurationProperties` 绑定配置项

## 2. 数据模型

- [x] 2.1 在 `Order` 实体中新增 `alipayTradeNo` 字段（String，nullable）
- [x] 2.2 确认数据库 `orders` 表新增 `alipay_trade_no VARCHAR(64)` 列（通过 JPA DDL-auto 或手动迁移）

## 3. 支付服务

- [x] 3.1 创建 `PaymentService`，实现 `createPayForm(orderId, totalAmount)` 方法，调用 SDK `alipay.trade.page.pay`，返回支付表单 HTML
- [x] 3.2 在 `PaymentService` 中实现 `handleNotify(params)` 方法：验签、检查 trade_status、幂等更新订单状态为 PAID、保存 alipayTradeNo

## 4. 支付控制器

- [x] 4.1 创建 `PaymentController`，`POST /api/v1/payment/notify` 接口：接收支付宝回调参数，调用 `handleNotify()`，返回纯文本 "success" 或 "failure"
- [x] 4.2 在 `PaymentController` 中实现 `GET /api/v1/payment/return` 接口：从请求参数获取 `out_trade_no`，重定向到前端支付结果页

## 5. 修改订单支付接口

- [x] 5.1 修改 `OrderController.pay()`：调用 `PaymentService.createPayForm()`，返回支付表单 HTML 字符串（Content-Type: text/html）
- [x] 5.2 修改 `OrderService.pay()`：移除直接设置 PAID 的逻辑，改为仅校验订单状态，返回支付参数

## 6. 安全配置

- [x] 6.1 在 `SecurityConfig` 中将 `/api/v1/payment/notify` 添加到白名单（permitAll），确保支付宝服务器可以无 JWT 访问

## 7. 前端适配

- [x] 7.1 修改前端支付调用逻辑：接收后端返回的 HTML 字符串，创建隐藏 div 注入 HTML 并自动 submit 表单，跳转至支付宝页面
- [x] 7.2 新增前端支付结果页（`/payment/result`），展示支付成功/处理中提示，并提供查看订单详情的链接
- [x] 7.3 在前端路由中注册 `/payment/result` 路径

## 8. 本地调试验证

- [ ] 8.1 启动 ngrok（`ngrok http 8080`），将生成的 HTTPS URL 更新到 `application.yml` 的 `notify-url`
- [ ] 8.2 使用支付宝沙箱 App 或沙箱账号完成一笔测试支付，验证回调触发且订单变为 PAID
- [ ] 8.3 验证重复回调幂等性：对同一订单重复 POST notify，确认订单状态不被重复处理

