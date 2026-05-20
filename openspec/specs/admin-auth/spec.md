## Purpose

管理员身份认证：支持 ADMIN 账户通过统一登录接口进行身份验证，JWT 中携带 role=ADMIN，并保护管理接口不被非 ADMIN 角色访问。ADMIN 账户只能通过数据库初始化脚本创建，不可自注册。

## Requirements

### Requirement: ADMIN 角色登录
系统 SHALL 允许 ADMIN 账户使用与买卖家相同的登录接口完成身份认证，JWT 中携带 role=ADMIN。

#### Scenario: ADMIN 登录成功
- **WHEN** 持有 ADMIN 角色的账户提交正确的邮箱和密码
- **THEN** 系统返回包含 role=ADMIN 的 Access Token 和 Refresh Token，前端跳转至 `/admin/overview`

#### Scenario: ADMIN 访问管理后台路由
- **WHEN** 携带 role=ADMIN JWT 的请求访问 `/api/v1/admin/**` 下的接口
- **THEN** 系统正常处理请求

#### Scenario: 非 ADMIN 角色访问管理接口
- **WHEN** BUYER 或 SELLER 角色的用户请求 `/api/v1/admin/**` 下的接口
- **THEN** 系统返回 403 状态码

#### Scenario: 前端 ADMIN 路由守卫
- **WHEN** 非 ADMIN 角色用户直接访问 `/admin/*` 前端路由
- **THEN** 前端路由守卫重定向至首页或登录页

### Requirement: ADMIN 账户不可自注册
系统 SHALL 禁止通过注册接口创建 ADMIN 角色账户，ADMIN 账户只能通过数据库初始化脚本创建。

#### Scenario: 尝试注册为 ADMIN
- **WHEN** 注册请求中 role 字段为 ADMIN
- **THEN** 系统返回 400 状态码并提示"不支持该角色注册"
