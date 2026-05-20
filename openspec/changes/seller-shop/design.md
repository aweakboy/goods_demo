## Context

系统当前商品（Product）只通过 `sellerId` 关联卖家用户，买家看到的是卖家用户名。没有店铺概念，无法浏览某家店的商品，商品列表也无法按店铺筛选。

现有关键表：`users`（含 role=SELLER）、`products`（sellerId 外键）。需要在两者之间插入 `shops` 层。

## Goals / Non-Goals

**Goals:**
- 每个卖家注册唯一一家店铺，商品归属该店铺
- 商品列表/详情/订单展示店铺名
- 买家可访问店铺主页浏览该店铺所有上架商品
- 管理员可查看/禁用店铺，店铺禁用后其商品买家不可见

**Non-Goals:**
- 一个卖家开多家店铺（1:1 关系，本期不扩展）
- 店铺评分/评价
- 店铺自定义装修/主题
- 店铺关注/收藏

## Decisions

### 决策 1：Shop 与 Seller 1:1，通过 sellerId UNIQUE 约束保证

**选择**：`shops.seller_id` 加 UNIQUE 约束，系统层面每个卖家只能有一家店铺。

**理由**：当前系统没有多店铺需求；1:1 关系让"查商品所属店铺"直接通过 `seller_id` join，无需额外外键；后续如需多店铺，可去掉 UNIQUE 并在 `products` 加 `shop_id`。

---

### 决策 2：products 表不新增 shop_id 外键，通过 seller_id → shops 间接关联

**选择**：`Product.shopId` 在应用层通过 `sellerId` 查 `shops` 获得，DB 层 products 表不新增列。

**理由**：避免 products 表迁移（存量数据需回填）；1:1 关系下 seller_id 即可唯一确定 shop，join 成本极低；如后期支持多店铺再做 schema 迁移。

**风险**：查商品时多一次 join/查询 → 通过 Service 层批量加载（Map<sellerId, Shop>）避免 N+1。

---

### 决策 3：店铺禁用通过 shop.status=INACTIVE 实现，买家端查询过滤

**选择**：`GET /api/v1/products` 公开接口加 join 条件，只返回 `shop.status=ACTIVE` 卖家的商品。

**理由**：不影响卖家自己查看，不影响管理员查看，只对买家端生效；逻辑集中在 `ProductRepository.searchActive`。

---

### 决策 4：店铺主页 URL 使用 `/shops/{shopId}`，不用 `/shops/{slug}`

**选择**：按 shopId（数字）路由，如 `/shops/3`。

**理由**：无需 slug 唯一性管理；当前系统其他实体均用数字 ID；简洁实现优先。

---

### 数据模型

```
shops 表：
  id          BIGINT PK
  seller_id   BIGINT UNIQUE NOT NULL FK→users.id
  name        VARCHAR(100) NOT NULL UNIQUE
  description TEXT
  status      ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE'
  created_at  TIMESTAMP
```

### API 结构

```
公开接口：
  GET  /api/v1/shops/{id}              # 店铺信息 + 商品列表
  GET  /api/v1/shops?name=             # 按店铺名搜索（用于买家搜索）

卖家接口：
  GET  /api/v1/seller/shop             # 查看自己的店铺
  POST /api/v1/seller/shop             # 注册店铺
  PUT  /api/v1/seller/shop             # 编辑店铺信息

管理员接口：
  GET  /api/v1/admin/shops             # 所有店铺列表（分页）
  PUT  /api/v1/admin/shops/{id}/status # 禁用/启用店铺
```

### 前端路由

```
/shops/:id                → ShopStorefront.vue（公开店铺主页）
/seller/shop              → SellerShopView.vue（注册/编辑店铺）
/admin/shops              → AdminShops.vue（管理后台新页面）
```

## Risks / Trade-offs

- **卖家未注册店铺时上架商品** → 上架接口校验卖家是否有店铺，无则返回 400 提示先注册店铺
- **存量商品无 shop 关联** → 因为通过 seller_id 关联，只要卖家注册了店铺，存量商品自动归属该店铺；未注册店铺的卖家存量商品在买家端过滤掉（shop 不存在视为 INACTIVE）
- **店铺名唯一性冲突** → DB 层 UNIQUE 约束 + Service 层友好报错
- **searchActive 查询增加 shop join** → 对现有查询有轻微改动，需同步更新 ProductRepository

## Migration Plan

1. 执行 `shops` 表 DDL
2. 现有卖家可选择性注册店铺（不强制回填，存量商品自动通过 seller_id 关联）
3. 更新 `searchActive` 查询加 shop status 过滤（已有商品若卖家未注册店铺则不展示，需告知卖家）
