## MODIFIED Requirements

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
