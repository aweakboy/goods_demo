## Context

当前系统使用单一模拟支付接口（`POST /orders/{id}/pay`），直接将订单状态置为 PAID，无任何外部交互。本次引入支付宝沙箱，需要新增异步回调处理路径，并将原有同步支付接口改造为发起支付请求的入口。

技术栈：Spring Boot 3.2 + Java 17 + MySQL + JWT，前端 Vue 3 + Vite。

## Goals / Non-Goals

**Goals:**

- 接入支付宝沙箱，实现完整的"发起支付 → 跳转 → 付款 → 异步回调 → 订单状态更新"流程
- 代码架构与生产支付宝环境兼容，后续仅需替换配置即可上线
- 本地开发通过 ngrok 内网穿透接收沙箱回调

**Non-Goals:**

- 不接入微信支付、Stripe 等其他渠道
- 不实现退款流程
- 不处理支付超时自动取消（可后续扩展）
- 不接入生产环境（无营业执照要求）

## Decisions

### D1：使用 PC 网页支付（alipay.trade.page.pay）

支付宝提供多种支付方式：PC 网页支付、手机网站支付、APP 支付、当面付（扫码）。

选择 **PC 网页支付**，原因：
- 与当前 Web 系统契合，无需额外 App
- 沙箱支持完整，调试方便
- 返回 HTML 表单，前端自动提交即可跳转，集成最简单

弃用方案：扫码支付（需轮询支付结果，前端复杂度高）；APP 支付（需移动端）。

### D2：回调验签使用 SDK 内置方法

支付宝回调必须验签以防伪造。使用 `AlipaySignature.rsaCheckV1()` SDK 内置方法，避免手写签名逻辑。

**关键安全点**：回调接口返回字符串 `"success"` 或 `"failure"`，不返回 JSON；不能有 JWT 鉴权。

### D3：Order 实体新增 alipayTradeNo 字段

回调中只携带 `out_trade_no`（即我们的订单 ID）和 `trade_no`（支付宝流水号）。需将 `trade_no` 持久化以备对账。

不单独建支付记录表——当前场景一单一付，Order 直接扩展字段即可。

### D4：配置外部化，密钥不进代码

支付宝密钥通过 `application.yml` 注入，用 `@ConfigurationProperties` 绑定到 `AlipayConfig`。
生产环境通过环境变量覆盖，本地开发用沙箱值，沙箱值可提交（因为是测试密钥）。

### D5：前端支付跳转方式

后端返回支付宝生成的 HTML 表单字符串，前端创建隐藏 `<div>` 注入 HTML 并自动 `submit()`，实现跳转到支付宝页面。支付完成后支付宝跳回 `return_url`，前端展示结果页（仅 UI，不作业务依据）。

## Risks / Trade-offs

- **回调需公网 URL** → 本地开发必须用 ngrok，每次重启 ngrok URL 会变，需更新配置。缓解：固定用同一 ngrok 会话，或使用 ngrok 付费版固定域名。

- **沙箱偶尔不稳定** → 支付宝沙箱环境有时延迟高或回调失败。缓解：提供"手动查单"兜底接口（可后续添加），开发时多等几秒。

- **notify 接口放行 JWT** → `/api/v1/payment/notify` 无鉴权，任何人可以 POST。缓解：依赖验签保证安全，不做业务操作前必须 `rsaCheckV1` 通过。

## Migration Plan

1. 数据库：Order 表新增 `alipay_trade_no VARCHAR(64)` 列（nullable，存量订单为 null）
2. 部署：先更新后端（新接口无破坏性），再更新前端支付逻辑
3. 回滚：前端回滚到旧 `/pay` 调用，后端保留原 `pay()` 逻辑作为降级

## Open Questions

- ngrok 是否已安装？若未安装需在实现前准备好。
- 沙箱 APPID / 密钥对是否已在 open.alipay.com 配置完成？
