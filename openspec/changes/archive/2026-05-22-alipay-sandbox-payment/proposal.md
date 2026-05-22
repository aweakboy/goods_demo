## Why

当前系统使用模拟支付接口，调用后订单直接变为 PAID，无法演示真实的支付流程。接入支付宝沙箱环境，在不需要营业执照的前提下，实现完整的支付跳转、异步回调、验签和状态更新流程，使系统具备可升级为生产支付的基础架构。

## What Changes

- **新增**：支付宝沙箱支付服务，封装 Alipay SDK 调用，生成支付表单
- **新增**：支付回调接口 `POST /api/v1/payment/notify`，接收支付宝异步通知并验签
- **新增**：支付结果跳回接口 `GET /api/v1/payment/return`，引导前端展示结果页
- **修改**：`POST /api/v1/orders/{id}/pay` 从直接改状态改为返回支付宝支付表单 HTML
- **修改**：`Order` 实体新增 `alipayTradeNo` 字段，存储支付宝交易流水号
- **修改**：`SecurityConfig` 放行 `/api/v1/payment/notify`（支付宝服务器无 JWT）
- **新增**：`pom.xml` 引入 `alipay-sdk-java` 依赖
- **新增**：`application.yml` 新增支付宝沙箱配置项（appId、privateKey、alipayPublicKey、notifyUrl、returnUrl）

## Capabilities

### New Capabilities

- `payment`: 支付流程管理，包括发起支付、接收异步回调、验证签名、更新订单状态

### Modified Capabilities

- `order-management`: 支付接口行为变更——从直接更新状态改为发起真实支付流程，订单实体新增支付流水号字段

## Impact

- **后端**：`OrderController`、`OrderService`、`Order` 实体、`SecurityConfig`
- **新增类**：`PaymentController`、`PaymentService`、`AlipayConfig`
- **依赖**：`alipay-sdk-java` 4.38.0
- **配置**：`application.yml` 新增沙箱密钥配置
- **本地开发**：需配合 ngrok 或其他内网穿透工具暴露回调地址
