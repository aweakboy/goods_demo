## Why

现有系统商品只显示卖家用户名，买家无法感知店铺品牌，也无法浏览某家店的全部商品。引入店铺（Shop）实体，让卖家注册专属店铺、商品归属店铺、买家可查看店铺主页，形成完整的多卖家市场体验。

## What Changes

- 新增 `Shop` 实体：每个卖家拥有唯一一家店铺（1:1），含店铺名称、简介、状态
- 卖家须先注册店铺，才能上架商品；商品归属卖家所属的店铺
- 商品列表和详情页展示店铺名称，并可点击进入店铺主页
- 店铺主页展示该店铺所有上架商品
- 订单详情和购物车展示商品所属店铺名称
- 管理后台新增店铺管理：查看所有店铺、禁用/启用店铺（店铺禁用后其商品对买家不可见）
- 管理后台商品审核新增按店铺名筛选

## Capabilities

### New Capabilities

- `seller-shop-management`: 卖家注册店铺、查看和编辑自己的店铺信息（名称、简介）
- `shop-storefront`: 公开的店铺主页，展示店铺信息及其全部上架商品，支持按店铺名搜索

### Modified Capabilities

- `seller-dashboard`: 卖家上架商品前须已注册店铺；卖家后台展示所属店铺信息
- `product-catalog`: 商品列表和详情需展示店铺名称，并附带店铺主页链接；商品搜索新增按店铺名筛选
- `order-management`: 订单详情中每条订单条目需显示所属店铺名称
- `admin-product-moderation`: 商品审核列表新增店铺名列，支持按店铺名筛选（替代原卖家用户名筛选）

## Impact

- **数据库**：新增 `shops` 表（id、seller_id UNIQUE、name、description、status、created_at）；`products` 表新增 `shop_id` 外键（可通过 seller_id 关联，无需强制迁移）
- **后端**：新增 `Shop` 实体、`ShopRepository`、`ShopService`；`ProductService` 上架时校验卖家有店铺；`ProductController` 返回商品时携带 shop 信息；`AdminService` 扩展店铺管理接口
- **前端**：新增店铺注册/编辑页、店铺主页；商品卡片和详情页展示店铺名；订单详情展示店铺名；管理后台新增店铺管理页
- **依赖**：无新增外部依赖
