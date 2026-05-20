## Purpose

用户管理：允许管理员查看、筛选平台所有用户，并对用户账户执行禁用或启用操作，ADMIN 账户不可被禁用。

## Requirements

### Requirement: 查看全部用户
系统 SHALL 允许管理员分页查看平台所有用户，支持按角色和账户状态筛选。

#### Scenario: 查看用户列表
- **WHEN** 管理员访问用户管理页面
- **THEN** 系统返回分页用户列表，每条记录包含：用户ID、用户名、邮箱、角色、账户状态、注册时间

#### Scenario: 按角色筛选用户
- **WHEN** 管理员选择角色筛选（BUYER / SELLER）
- **THEN** 系统只返回该角色的用户列表

#### Scenario: 按状态筛选用户
- **WHEN** 管理员选择状态筛选（ACTIVE / DISABLED）
- **THEN** 系统只返回该状态的用户列表

### Requirement: 禁用/启用用户账户
系统 SHALL 允许管理员将用户账户状态设置为 DISABLED 或 ACTIVE，禁用后该账户无法登录。

#### Scenario: 禁用用户
- **WHEN** 管理员将某用户状态设置为 DISABLED
- **THEN** 系统更新 user.status 为 DISABLED，该用户后续登录请求返回 401 并提示"账户已被禁用"

#### Scenario: 启用用户
- **WHEN** 管理员将 DISABLED 状态的用户设置为 ACTIVE
- **THEN** 系统更新 user.status 为 ACTIVE，该用户可正常登录

#### Scenario: 禁用 ADMIN 账户
- **WHEN** 管理员尝试禁用另一个 ADMIN 角色的账户
- **THEN** 系统返回 400 状态码并提示"不允许禁用管理员账户"

#### Scenario: 禁用自己的账户
- **WHEN** 当前登录的 ADMIN 尝试禁用自己的账户
- **THEN** 系统返回 400 状态码并提示"不允许禁用当前登录账户"
