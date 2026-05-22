# payment Specification

## Purpose
TBD - created by archiving change alipay-sandbox-payment. Update Purpose after archive.
## Requirements
### Requirement: 发起支付宝支付
系统 SHALL 允许买家对 PENDING_PAYMENT 状态的订单发起支付宝支付，返回支付表单供前端跳转。

#### Scenario: 发起支付成功
- **WHEN** 买家对 PENDING_PAYMENT 状态的订单调用支付接口
- **THEN** 系统调用支付宝沙箱生成支付表单 HTML，返回给前端，前端自动提交跳转至支付宝支付页面

#### Scenario: 订单状态不允许支付
- **WHEN** 买家对非 PENDING_PAYMENT 状态的订单调用支付接口
- **THEN** 系统返回 400 状态码并提示"订单状态不允许支付"

#### Scenario: 订单不属于当前用户
- **WHEN** 买家尝试支付他人的订单
- **THEN** 系统返回 403 状态码

### Requirement: 接收支付宝异步回调
系统 SHALL 提供公开的回调接口，接收支付宝的异步支付通知，验签后更新订单状态。

#### Scenario: 支付成功回调
- **WHEN** 支付宝向 notify_url 发送 trade_status=TRADE_SUCCESS 的 POST 请求
- **THEN** 系统验签通过后将对应订单状态更新为 PAID，记录 alipayTradeNo，返回字符串 "success"

#### Scenario: 验签失败
- **WHEN** 回调请求签名验证不通过
- **THEN** 系统返回字符串 "failure"，不更新订单状态

#### Scenario: 重复回调
- **WHEN** 支付宝对同一笔订单重复发送成功回调
- **THEN** 系统幂等处理，若订单已为 PAID 则直接返回 "success"，不重复操作

### Requirement: 支付结果页跳回
系统 SHALL 提供 return_url 接口，供支付宝在用户付款后将浏览器重定向回系统。

#### Scenario: 支付完成跳回
- **WHEN** 支付宝在用户完成支付后将浏览器重定向到 return_url
- **THEN** 系统将浏览器重定向到前端支付结果页，携带订单 ID 参数

