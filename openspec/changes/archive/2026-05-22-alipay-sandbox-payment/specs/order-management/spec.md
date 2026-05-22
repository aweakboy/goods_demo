## MODIFIED Requirements

### Requirement: 模拟支付
系统 SHALL 提供支付接口，买家调用后跳转至支付宝沙箱完成支付，支付成功后订单状态更新为已付款。

#### Scenario: 发起支付
- **WHEN** 买家对 PENDING_PAYMENT 状态的订单调用支付接口
- **THEN** 系统返回支付宝支付表单 HTML，前端提交后跳转至支付宝沙箱支付页面

#### Scenario: 支付完成
- **WHEN** 买家在支付宝完成付款，支付宝回调系统 notify_url
- **THEN** 系统验签通过后将订单状态更新为 PAID，记录支付宝交易流水号

#### Scenario: 重复支付
- **WHEN** 买家对非 PENDING_PAYMENT 状态的订单调用支付接口
- **THEN** 系统返回 400 状态码并提示订单状态不允许支付
