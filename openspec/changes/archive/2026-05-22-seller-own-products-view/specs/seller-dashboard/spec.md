## MODIFIED Requirements

### Requirement: 卖家登录入口
系统 SHALL 在卖家成功登录后直接跳转至卖家商品管理页，而非全平台公开商品列表。

#### Scenario: 卖家登录后落地页
- **WHEN** 卖家使用正确的邮箱和密码登录
- **THEN** 系统跳转至 `/seller/products`（商品管理页），展示该卖家自己的商品列表

#### Scenario: 买家登录后落地页不受影响
- **WHEN** 买家使用正确的邮箱和密码登录
- **THEN** 系统跳转至 `/`（公开商品列表），行为与原来一致

### Requirement: 卖家导航视图
系统 SHALL 在卖家登录状态下隐藏指向全平台商品列表的「商品」导航链接。

#### Scenario: 卖家导航栏内容
- **WHEN** 卖家已登录并查看导航栏
- **THEN** 导航栏显示「我的店铺」「商品管理」「订单管理」，不显示「商品」链接

#### Scenario: 卖家访问 /products 被重定向
- **WHEN** 卖家直接访问 `/products`（如通过书签或手动输入）
- **THEN** 系统自动重定向至 `/seller/products`
