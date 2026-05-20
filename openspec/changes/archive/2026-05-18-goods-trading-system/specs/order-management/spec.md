## ADDED Requirements

### Requirement: 创建订单
系统 SHALL 允许已登录买家将购物车中选中的商品创建为一笔订单，并扣减库存。

#### Scenario: 下单成功
- **WHEN** 买家提交包含收货地址的下单请求
- **THEN** 系统创建订单（状态：PENDING_PAYMENT），扣减商品库存，清空已购商品的购物车条目，返回订单ID

#### Scenario: 库存不足
- **WHEN** 下单时某商品库存不足
- **THEN** 系统返回 400 状态码，提示具体商品库存不足，不创建订单

#### Scenario: 购物车为空下单
- **WHEN** 买家购物车为空时尝试下单
- **THEN** 系统返回 400 状态码并提示"请先添加商品到购物车"

### Requirement: 模拟支付
系统 SHALL 提供模拟支付接口，买家调用后将订单状态更新为已付款。

#### Scenario: 支付成功
- **WHEN** 买家对 PENDING_PAYMENT 状态的订单调用支付接口
- **THEN** 系统将订单状态更新为 PAID

#### Scenario: 重复支付
- **WHEN** 买家对非 PENDING_PAYMENT 状态的订单调用支付接口
- **THEN** 系统返回 400 状态码并提示订单状态不允许支付

### Requirement: 订单状态流转
系统 SHALL 按照固定状态机管理订单生命周期：PENDING_PAYMENT → PAID → SHIPPED → COMPLETED → CANCELLED。

#### Scenario: 卖家标记发货
- **WHEN** 卖家对 PAID 状态订单调用发货接口
- **THEN** 系统将订单状态更新为 SHIPPED

#### Scenario: 买家确认收货
- **WHEN** 买家对 SHIPPED 状态订单确认收货
- **THEN** 系统将订单状态更新为 COMPLETED

#### Scenario: 取消 PENDING_PAYMENT 订单
- **WHEN** 买家取消 PENDING_PAYMENT 状态的订单
- **THEN** 系统将订单状态更新为 CANCELLED，并恢复库存

### Requirement: 订单历史查询
系统 SHALL 允许买家查看自己的全部订单列表，支持按状态筛选。

#### Scenario: 查看全部订单
- **WHEN** 买家访问订单列表页
- **THEN** 系统返回该买家所有订单，按创建时间倒序

#### Scenario: 按状态筛选订单
- **WHEN** 买家选择指定状态筛选
- **THEN** 系统只返回该状态的订单

#### Scenario: 查看订单详情
- **WHEN** 买家查看某笔订单详情
- **THEN** 系统返回订单中所有商品、价格、收货地址和当前状态
