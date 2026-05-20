## Why

系统目前只有 BUYER 和 SELLER 两种角色，没有任何管理端能力——分类只能通过直接操作数据库维护，违规商品无法下架，用户纠纷无法干预，平台运营无据可查。引入管理员角色和后台是平台从 MVP 走向可运营状态的前提。

## What Changes

- 新增 `ADMIN` 角色，拥有独立于买卖双方的全局权限
- 新增管理员后台前端页面（Vue），独立路由 `/admin/*`，仅 ADMIN 可访问
- 新增后端管理员 API（`/api/v1/admin/*`），全部受 ADMIN 角色守卫
- 新增数据总览仪表盘（用户数、商品数、订单数、成交额概览）
- 新增用户管理：查看所有用户、禁用/启用账户
- 新增商品审核：查看所有商品、强制下架违规商品
- 新增分类管理：创建、编辑、启用/禁用商品分类（替代当前只能数据库操作的方式）
- 新增订单总览：跨买卖双方查看所有订单

## Capabilities

### New Capabilities

- `admin-auth`: ADMIN 角色的认证与权限守卫——登录入口、角色校验、后台路由守卫
- `admin-overview`: 数据总览仪表盘，展示平台核心指标（用户总数、商品总数、今日订单、总成交额）
- `admin-user-management`: 管理员查看全部用户列表、禁用/启用账户
- `admin-product-moderation`: 管理员查看全部商品、强制下架商品
- `admin-category-management`: 管理员对商品分类进行 CRUD 操作（创建、编辑、启用/禁用）
- `admin-order-oversight`: 管理员跨买卖双方查看所有订单详情

### Modified Capabilities

- `user-auth`: 权限控制扩展——原有 BUYER/SELLER 二值角色模型需增加 ADMIN 角色，注册接口不允许自注册为 ADMIN（ADMIN 账户由数据库初始化或超级管理员创建）

## Impact

- **后端**：`Role` 枚举新增 `ADMIN`；Spring Security 配置新增 `/api/v1/admin/**` 路由守卫；新增 `AdminController`、`AdminService`；User 实体可能需要新增 `status`（启用/禁用）字段
- **前端**：新增 `/admin` 路由组及对应 Vue 页面；`router/index.js` 增加 ADMIN 角色守卫；`stores/auth.js` 需支持 ADMIN 角色识别
- **数据库**：`user` 表新增 `status` 字段（ACTIVE/DISABLED）；需要至少一条初始 ADMIN 账户数据（data.sql 或 migration）
- **依赖**：无新增外部依赖
