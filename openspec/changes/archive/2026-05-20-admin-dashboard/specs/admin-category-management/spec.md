## ADDED Requirements

### Requirement: 查看分类列表
系统 SHALL 允许管理员查看所有商品分类，包含已禁用的分类。

#### Scenario: 查看完整分类列表
- **WHEN** 管理员访问分类管理页面
- **THEN** 系统返回所有分类，包含：分类ID、名称、状态（ACTIVE/INACTIVE）、关联商品数量

### Requirement: 创建分类
系统 SHALL 允许管理员创建新的商品分类。

#### Scenario: 成功创建分类
- **WHEN** 管理员提交唯一的分类名称
- **THEN** 系统创建分类（默认状态 ACTIVE）并返回新分类信息

#### Scenario: 分类名称重复
- **WHEN** 管理员提交的分类名称已存在
- **THEN** 系统返回 400 状态码并提示"分类名称已存在"

### Requirement: 编辑分类
系统 SHALL 允许管理员修改已有分类的名称。

#### Scenario: 成功修改分类名称
- **WHEN** 管理员提交新的分类名称
- **THEN** 系统更新分类名称并返回最新分类信息

#### Scenario: 修改为已存在的名称
- **WHEN** 管理员提交的新名称与其他分类重复
- **THEN** 系统返回 400 状态码并提示"分类名称已存在"

### Requirement: 启用/禁用分类
系统 SHALL 允许管理员控制分类的可见状态，禁用后该分类不出现在买家端筛选列表。

#### Scenario: 禁用分类
- **WHEN** 管理员将某分类状态设置为 INACTIVE
- **THEN** 系统更新分类状态为 INACTIVE，买家端分类列表不再包含该分类

#### Scenario: 启用分类
- **WHEN** 管理员将 INACTIVE 分类设置为 ACTIVE
- **THEN** 系统更新分类状态为 ACTIVE，买家端分类列表恢复该分类
