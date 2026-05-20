## Context

系统当前采用 Spring Boot + Vue 3 架构，通过 JWT 实现无状态认证，角色存储于 User 实体的 `role` 字段（枚举：BUYER/SELLER）。Spring Security 通过 `@PreAuthorize` 和 Security Filter Chain 实现接口级权限控制。前端通过 Pinia `authStore` 持有 token 和角色信息，Router Guard 根据角色做页面级保护。

当前没有管理员角色、没有管理员路由、没有 admin API，分类数据只能通过直接操作 MySQL 维护。

## Goals / Non-Goals

**Goals:**
- 在现有认证体系上最小侵入地增加 ADMIN 角色
- 提供管理员登录入口和独立后台前端页面
- 实现用户管理、商品审核、分类管理、订单总览四大模块
- 提供数据概览仪表盘（平台核心指标）
- 初始 ADMIN 账户通过数据库 seed 脚本创建，无自注册入口

**Non-Goals:**
- 多级管理员权限（本期只有单一 ADMIN 角色）
- 操作审计日志
- 图表可视化（仅数字指标）
- 卖家资质审核流程（本期管理员只能禁用用户，不做入驻审批流）

## Decisions

### 决策 1：复用现有登录接口，不为管理员建立独立登录端点

**选择**：ADMIN 使用与买卖家相同的 `/api/v1/auth/login` 接口，返回的 JWT payload 中 role=ADMIN，前端根据角色跳转至 `/admin` 路由。

**理由**：认证逻辑完全一致，无需维护两套 token 体系。ADMIN 账户不开放注册入口即可隔离。

**备选**：独立 `/api/v1/admin/login` 端点 → 增加维护成本，无额外安全收益。

---

### 决策 2：User 表新增 `status` 字段而非软删除

**选择**：`user.status` 枚举字段（ACTIVE / DISABLED），禁用用户时设为 DISABLED，Spring Security 登录校验时检查此字段。

**理由**：禁用账户是可逆操作，需要保留数据；UserDetails 接口的 `isEnabled()` 方法天然支持此语义，改动最小。

**备选**：独立 `user_ban` 表 → 查询复杂度增加，本期不需要禁用原因/时间等扩展字段。

---

### 决策 3：后端 Admin API 全部挂载在 `/api/v1/admin/**`，统一 Security 规则

**选择**：Security Filter Chain 中增加一条规则 `requestMatchers("/api/v1/admin/**").hasRole("ADMIN")`，无需在每个方法上重复 `@PreAuthorize`。

**理由**：集中管控，防止漏加注解导致越权。Controller 层仍可用 `@PreAuthorize` 做更细粒度控制（如后期扩展子权限）。

---

### 决策 4：前端管理后台独立路由组，共用现有 authStore

**选择**：Vue Router 增加 `/admin` 路由组，`beforeEach` guard 中增加对 `role === 'ADMIN'` 的判断。不为管理员创建独立 Pinia store，复用 `authStore`。

**理由**：认证状态（token/role/user）结构一致，无需重复实现；管理后台只是一组受保护路由，不是独立应用。

---

### 决策 5：分类管理 API 复用现有 Category 实体，不新建表

**选择**：在 `AdminController` 中增加分类 CRUD 接口，操作现有 `Category` 实体。现有 `GET /api/v1/products/categories`（公开接口）继续服务买家端。

**理由**：Category 表结构已满足需求（id、name、status），无需改动数据模型。

---

### 数据模型变更

```
User 表新增字段：
  status  VARCHAR(20)  NOT NULL  DEFAULT 'ACTIVE'   -- ACTIVE | DISABLED
```

无其他表结构变更，仪表盘数据通过聚合查询现有表获得。

---

### API 结构

```
/api/v1/admin/
  GET    /overview          # 仪表盘数据（用户数、商品数、订单数、成交额）
  GET    /users             # 用户列表（分页，支持角色/状态筛选）
  PUT    /users/{id}/status # 启用/禁用用户
  GET    /products          # 所有商品（分页，支持状态/卖家筛选）
  PUT    /products/{id}/status  # 强制下架商品
  GET    /categories        # 分类列表（含禁用）
  POST   /categories        # 创建分类
  PUT    /categories/{id}   # 编辑分类
  PUT    /categories/{id}/status  # 启用/禁用分类
  GET    /orders            # 所有订单（分页，支持状态筛选）
  GET    /orders/{id}       # 订单详情
```

---

### 前端路由结构

```
/admin                   → AdminLayout（侧边导航）
  /admin/overview        → 数据总览
  /admin/users           → 用户管理
  /admin/products        → 商品审核
  /admin/categories      → 分类管理
  /admin/orders          → 订单总览
```

## Risks / Trade-offs

- **ADMIN 账户泄露风险** → 初始密码通过 seed 脚本写入，部署文档需明确要求上线前修改默认密码；后期可增加操作日志降低风险
- **仪表盘聚合查询性能** → 直接 COUNT/SUM 查询，数据量小时无影响；数据增长后可加 Redis 缓存或定时统计表，本期不做
- **禁用用户不立即踢出** → 已颁发的 JWT 在过期前仍有效（Access Token 15 分钟）；可接受，无需本期实现主动吊销
- **前后端 ADMIN 角色字符串一致性** → 后端 `Role.ADMIN`，JWT claim 写 `"ADMIN"`，前端 `authStore` 比较字符串 `"ADMIN"`，需保持一致，代码 review 时注意
