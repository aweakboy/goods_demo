## ADDED Requirements

### Requirement: 查看全部商品
系统 SHALL 允许管理员分页查看平台所有商品，包含已下架商品，支持按状态和卖家筛选。

#### Scenario: 查看商品列表
- **WHEN** 管理员访问商品审核页面
- **THEN** 系统返回分页商品列表，包含：商品ID、名称、价格、库存、状态、卖家用户名、分类、创建时间

#### Scenario: 按状态筛选商品
- **WHEN** 管理员选择商品状态筛选（ACTIVE / INACTIVE）
- **THEN** 系统只返回对应状态的商品

#### Scenario: 按卖家筛选商品
- **WHEN** 管理员输入卖家用户名关键词
- **THEN** 系统返回该卖家名称匹配的卖家的全部商品

### Requirement: 强制下架商品
系统 SHALL 允许管理员将任意商品状态设置为 INACTIVE，不受商品归属卖家限制。

#### Scenario: 管理员强制下架商品
- **WHEN** 管理员将某商品状态设置为 INACTIVE
- **THEN** 系统更新商品状态为 INACTIVE，买家端不再展示该商品

#### Scenario: 管理员重新上架商品
- **WHEN** 管理员将已下架商品状态设置为 ACTIVE
- **THEN** 系统更新商品状态为 ACTIVE，买家端恢复展示
