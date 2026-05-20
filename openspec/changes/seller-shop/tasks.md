## 1. 后端基础设施

- [x] 1.1 新建 `Shop` 实体（id、sellerId UNIQUE、name、description、status、createdAt）
- [x] 1.2 新建 `ShopRepository`（findBySellerId、findByName、existsByName、existsByNameAndIdNot、按名称模糊查询、分页查询）
- [x] 1.3 `schema.sql` 新增 `shops` 表 DDL（seller_id UNIQUE 约束）
- [x] 1.4 新建 `ShopRequest` DTO（name、description）和 `ShopResponse` DTO

## 2. 后端 — 卖家店铺 API

- [x] 2.1 新建 `ShopService`：实现注册店铺、查看自己店铺、编辑店铺（含名称唯一校验）
- [x] 2.2 新建 `SellerShopController`（`/api/v1/seller/shop`）：GET、POST、PUT 三个端点
- [x] 2.3 `ProductService.createProduct` 增加校验：卖家必须有已注册且 ACTIVE 的店铺

## 3. 后端 — 公开店铺接口

- [x] 3.1 `ShopService` 新增：按 ID 查店铺（含商品列表）、按名称关键词搜索店铺
- [x] 3.2 新建 `ShopController`（`/api/v1/shops`）：`GET /shops/{id}`、`GET /shops?name=` 两个公开端点
- [x] 3.3 `ShopResponse` 扩展：包含商品分页列表（带 shopName 字段）

## 4. 后端 — 商品接口携带店铺信息

- [x] 4.1 新建 `ProductWithShopResponse` DTO（在商品字段基础上增加 shopId、shopName）
- [x] 4.2 `ProductRepository.searchActive` 增加 shop status=ACTIVE 的 join 过滤条件
- [x] 4.3 `ProductController` 的商品列表和详情接口返回 shopName 和 shopId 字段
- [x] 4.4 `OrderService` 或 `OrderController` 的订单详情接口，OrderItem 返回时附带所属 shopName

## 5. 后端 — 管理员店铺接口

- [x] 5.1 `AdminService` 新增：分页查所有店铺（含卖家用户名、商品数）、更新店铺状态
- [x] 5.2 `AdminController` 新增：`GET /admin/shops`、`PUT /admin/shops/{id}/status`
- [x] 5.3 `AdminService.getProducts` 筛选条件由 sellerName 改为支持 shopName（或同时支持两者）
- [x] 5.4 `AdminProductResponse` 新增 shopName 字段

## 6. 前端 — 卖家店铺管理

- [x] 6.1 新建 `src/api/shop.js`（sellerShop CRUD + 公开店铺接口）
- [x] 6.2 新建 `views/seller/SellerShopView.vue`：注册/编辑店铺表单
- [x] 6.3 `router/index.js` 新增 `/seller/shop` 路由（requiresAuth + SELLER）
- [x] 6.4 `AppHeader.vue` 或卖家导航新增"我的店铺"入口

## 7. 前端 — 公开店铺主页

- [x] 7.1 新建 `views/ShopStorefrontView.vue`：展示店铺信息 + 商品列表（复用 ProductCard）
- [x] 7.2 `router/index.js` 新增 `/shops/:id` 公开路由

## 8. 前端 — 商品展示更新

- [x] 8.1 `ProductCard.vue` 新增店铺名称显示，点击跳转 `/shops/:shopId`
- [x] 8.2 `ProductDetailView.vue` 新增店铺名称和"进入店铺"链接
- [x] 8.3 `ProductListView.vue` 搜索栏新增店铺名筛选输入框
- [x] 8.4 `OrderDetailView.vue` 订单条目新增店铺名称列

## 9. 前端 — 管理后台更新

- [x] 9.1 `admin.js` API 封装新增店铺管理方法（getShops、updateShopStatus）
- [x] 9.2 新建 `views/admin/AdminShops.vue`：店铺列表表格，含状态筛选、禁用/启用操作
- [x] 9.3 `router/index.js` 管理员路由新增 `/admin/shops`
- [x] 9.4 `AdminLayout.vue` 侧边导航新增"店铺管理"菜单项
- [x] 9.5 `AdminProducts.vue` 筛选条件由卖家用户名改为/新增店铺名

## 10. 收尾

- [ ] 10.1 手动验证：未注册店铺的卖家无法上架商品
- [ ] 10.2 手动验证：店铺禁用后商品从买家端消失
- [ ] 10.3 手动验证：商品详情页展示店铺名并可点击跳转
