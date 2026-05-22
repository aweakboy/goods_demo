## MODIFIED Requirements

### Requirement: 查看全部商品
系统 SHALL 允许管理员分页查看平台所有商品，包含已下架商品，支持按状态和店铺名筛选。

#### Scenario: 查看商品列表
- **WHEN** 管理员访问商品审核页面
- **THEN** 系统返回分页商品列表，包含：商品ID、名称、价格、库存、状态、所属店铺名称、卖家用户名、分类、创建时间

#### Scenario: 按状态筛选商品
- **WHEN** 管理员选择商品状态筛选（ACTIVE / INACTIVE）
- **THEN** 系统只返回对应状态的商品

#### Scenario: 按店铺名筛选商品
- **WHEN** 管理员输入店铺名称关键词
- **THEN** 系统返回该店铺名称匹配的店铺的全部商品

## ADDED Requirements

### Requirement: 管理员查看所有店铺
系统 SHALL 允许管理员分页查看平台所有店铺，支持按状态筛选。

#### Scenario: 查看店铺列表
- **WHEN** 管理员访问店铺管理页面
- **THEN** 系统返回分页店铺列表，包含：店铺ID、店铺名称、卖家用户名、商品数量、状态、创建时间

#### Scenario: 按状态筛选店铺
- **WHEN** 管理员选择状态筛选（ACTIVE / INACTIVE）
- **THEN** 系统只返回对应状态的店铺

### Requirement: 管理员禁用/启用店铺
系统 SHALL 允许管理员将店铺状态设置为 INACTIVE 或 ACTIVE；禁用后该店铺商品买家端不可见。

#### Scenario: 禁用店铺
- **WHEN** 管理员将某店铺状态设置为 INACTIVE
- **THEN** 系统更新店铺状态，该店铺的商品不再展示给买家

#### Scenario: 启用店铺
- **WHEN** 管理员将 INACTIVE 状态店铺设置为 ACTIVE
- **THEN** 系统更新店铺状态，该店铺商品恢复在买家端展示
