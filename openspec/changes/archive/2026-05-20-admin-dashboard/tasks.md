## 1. 后端基础设施

- [x] 1.1 `Role` 枚举新增 `ADMIN` 值
- [x] 1.2 `User` 实体新增 `status` 字段（枚举 `UserStatus`: ACTIVE / DISABLED），实现 `UserDetails.isEnabled()` 检查 status
- [x] 1.3 生成并执行数据库 migration：`user` 表新增 `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
- [x] 1.4 `data.sql`（或独立 seed 脚本）中插入初始 ADMIN 账户（邮箱+BCrypt 密码+role=ADMIN+status=ACTIVE）
- [x] 1.5 Spring Security Filter Chain 新增规则：`/api/v1/admin/**` 仅允许 ADMIN 角色访问

## 2. 后端 — 注册接口防护

- [x] 2.1 `AuthController.register` 校验：若请求 role 为 ADMIN，返回 400 "不支持该角色注册"

## 3. 后端 — Admin API

- [x] 3.1 创建 `AdminController`（`/api/v1/admin`）、`AdminService`、对应 DTO
- [x] 3.2 实现 `GET /admin/overview`：查询用户总数、上架商品数、今日订单数、总成交额
- [x] 3.3 实现 `GET /admin/users`：分页查询所有用户，支持 role / status 筛选参数
- [x] 3.4 实现 `PUT /admin/users/{id}/status`：更新用户状态；禁止操作 ADMIN 账户和当前登录账户
- [x] 3.5 实现 `GET /admin/products`：分页查询所有商品（含下架），支持 status / sellerName 筛选
- [x] 3.6 实现 `PUT /admin/products/{id}/status`：强制修改商品状态
- [x] 3.7 实现 `GET /admin/categories`：查询所有分类（含禁用），附带各分类关联商品数
- [x] 3.8 实现 `POST /admin/categories`：创建分类，校验名称唯一
- [x] 3.9 实现 `PUT /admin/categories/{id}`：修改分类名称，校验唯一性
- [x] 3.10 实现 `PUT /admin/categories/{id}/status`：切换分类启用/禁用状态
- [x] 3.11 实现 `GET /admin/orders`：分页查询所有订单，支持 status 筛选，含买家用户名
- [x] 3.12 实现 `GET /admin/orders/{id}`：查询订单完整详情

## 4. 前端 — 路由与布局

- [x] 4.1 `router/index.js` 新增 `/admin` 路由组，路由守卫检查 role === 'ADMIN'
- [x] 4.2 创建 `AdminLayout.vue`（含侧边导航：总览 / 用户管理 / 商品审核 / 分类管理 / 订单总览）
- [x] 4.3 `authStore` 确认支持识别 `ADMIN` 角色（登录后跳转 `/admin/overview`）
- [x] 4.4 封装 `src/api/admin.js`：定义全部 Admin API 调用方法

## 5. 前端 — 各功能页面

- [x] 5.1 创建 `views/admin/AdminOverview.vue`：展示四项核心指标卡片
- [x] 5.2 创建 `views/admin/AdminUsers.vue`：用户列表表格，含角色/状态筛选、禁用/启用操作按钮
- [x] 5.3 创建 `views/admin/AdminProducts.vue`：商品列表表格，含状态/卖家筛选、强制下架/上架操作
- [x] 5.4 创建 `views/admin/AdminCategories.vue`：分类列表 + 新建分类表单 + 编辑/启用/禁用操作
- [x] 5.5 创建 `views/admin/AdminOrders.vue`：订单列表表格，含状态筛选、点击查看详情弹窗

## 6. 测试与收尾

- [x] 6.1 后端：为 `AdminService` 的用户禁用逻辑编写单元测试（禁用 ADMIN 账户应抛异常）
- [x] 6.2 手动验证：BUYER/SELLER 访问 `/api/v1/admin/**` 返回 403
- [x] 6.3 手动验证：禁用用户后其登录请求返回 401
- [x] 6.4 手动验证：前端非 ADMIN 角色访问 `/admin/*` 被重定向
- [x] 6.5 更新 README 或部署文档，说明初始 ADMIN 账户和修改默认密码的步骤
