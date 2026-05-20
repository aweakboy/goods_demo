## ADDED Requirements

### Requirement: 用户注册
系统 SHALL 允许新用户通过用户名、邮箱和密码注册账号，并指定角色（买家或卖家）。

#### Scenario: 注册成功
- **WHEN** 用户提交合法的用户名、邮箱、密码和角色
- **THEN** 系统创建账号并返回 201 状态码及用户基本信息

#### Scenario: 邮箱重复注册
- **WHEN** 用户提交的邮箱已被注册
- **THEN** 系统返回 400 状态码并提示"邮箱已存在"

#### Scenario: 密码强度不足
- **WHEN** 用户提交的密码少于 8 位或不包含字母和数字
- **THEN** 系统返回 400 状态码并提示密码格式要求

### Requirement: 用户登录
系统 SHALL 通过邮箱和密码验证用户身份，成功后颁发 JWT Access Token 和 Refresh Token。

#### Scenario: 登录成功
- **WHEN** 用户提交正确的邮箱和密码
- **THEN** 系统返回 200 状态码、Access Token（15分钟有效）和 Refresh Token（7天有效）

#### Scenario: 密码错误
- **WHEN** 用户提交错误的密码
- **THEN** 系统返回 401 状态码并提示"邮箱或密码错误"

#### Scenario: 账号不存在
- **WHEN** 用户提交未注册的邮箱
- **THEN** 系统返回 401 状态码并提示"邮箱或密码错误"（不暴露账号是否存在）

### Requirement: Token 刷新
系统 SHALL 允许持有有效 Refresh Token 的用户换取新的 Access Token，无需重新登录。

#### Scenario: 刷新成功
- **WHEN** 用户提交有效的 Refresh Token
- **THEN** 系统返回新的 Access Token

#### Scenario: Refresh Token 过期
- **WHEN** 用户提交已过期的 Refresh Token
- **THEN** 系统返回 401 状态码，前端跳转登录页

### Requirement: 用户登出
系统 SHALL 允许用户登出，前端清除本地存储的 Token。

#### Scenario: 登出操作
- **WHEN** 已登录用户点击登出
- **THEN** 前端清除 Access Token 和 Refresh Token，跳转至登录页

### Requirement: 基于角色的权限控制
系统 SHALL 根据用户角色（BUYER / SELLER / ADMIN）控制 API 访问权限。注册接口不允许将角色设置为 ADMIN。

#### Scenario: 卖家访问卖家接口
- **WHEN** 角色为 SELLER 的用户访问卖家管理 API
- **THEN** 系统正常处理请求

#### Scenario: 买家尝试访问卖家接口
- **WHEN** 角色为 BUYER 的用户访问卖家管理 API
- **THEN** 系统返回 403 状态码

#### Scenario: 未登录访问受保护接口
- **WHEN** 未携带有效 Token 的请求访问受保护接口
- **THEN** 系统返回 401 状态码

#### Scenario: ADMIN 访问管理员接口
- **WHEN** 角色为 ADMIN 的用户访问 `/api/v1/admin/**` 接口
- **THEN** 系统正常处理请求

#### Scenario: 非 ADMIN 访问管理员接口
- **WHEN** BUYER 或 SELLER 角色的用户访问 `/api/v1/admin/**` 接口
- **THEN** 系统返回 403 状态码

#### Scenario: 注册时指定 ADMIN 角色
- **WHEN** 注册请求中 role 字段为 ADMIN
- **THEN** 系统返回 400 状态码并提示"不支持该角色注册"
