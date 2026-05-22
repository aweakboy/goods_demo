## 1. 数据模型与数据库

- [x] 1.1 在 `Shop` 实体中新增店铺地址字段：省、市、区、详细地址、完整地址、经度、纬度、地址校验状态
- [x] 1.2 在 `Order` 实体中新增收货地址快照字段：收货人姓名、手机号、省、市、区、详细地址、完整地址、经度、纬度、地址校验状态
- [x] 1.3 更新 `backend/src/main/resources/schema.sql`，为 `shops` 和 `orders` 表增加结构化地址字段
- [x] 1.4 在 `schema.sql` 中补充面向已有本地数据库的 ALTER TABLE 迁移注释

## 2. 后端 DTO 与校验

- [x] 2.1 更新 `ShopRequest`，要求店铺注册/编辑提交省、市、区、详细地址
- [x] 2.2 更新 `ShopResponse`、`ShopStorefrontResponse`、`AdminShopResponse`，返回店铺结构化地址和完整地址
- [x] 2.3 更新 `OrderRequest`，要求下单提交收货人姓名、手机号、省、市、区、详细地址
- [x] 2.4 更新订单相关响应（含 `AdminOrderResponse`），返回收货地址快照字段和完整地址
- [x] 2.5 为地址字段添加后端基础校验：必填、长度限制、手机号格式

## 3. 后端业务逻辑

- [x] 3.1 更新 `ShopService.register()`，保存店铺结构化地址并组装完整地址
- [x] 3.2 更新 `ShopService.update()`，允许卖家修改店铺地址并保持名称重复校验
- [x] 3.3 更新 `OrderService.createOrder()`，保存收货地址快照并继续兼容现有 `address` 展示字段
- [x] 3.4 确保历史订单读取仍可展示旧 `address` 字段，不因新增字段为空而报错

## 4. 前端表单与展示

- [x] 4.1 更新 `SellerShopView.vue`，在注册/编辑店铺表单中添加省、市、区、详细地址输入
- [x] 4.2 更新 `SellerShopView.vue`，在店铺详情中展示完整店铺地址
- [x] 4.3 更新 `CheckoutView.vue`，将收货地址 textarea 改为收货人姓名、手机号、省、市、区、详细地址字段
- [x] 4.4 更新 `OrderDetailView.vue`，展示订单收货地址快照和兼容旧订单地址
- [x] 4.5 更新 `SellerOrdersView.vue` 和 `AdminOrders.vue`，展示结构化后的完整收货地址
- [x] 4.6 更新 `ShopStorefrontView.vue`，在店铺页展示店铺完整地址

## 5. 后端自动化测试

- [x] 5.1 为 `ShopService` 添加注册店铺缺少地址字段时返回 400 的单元测试
- [x] 5.2 为 `ShopService` 添加注册店铺成功保存结构化地址和完整地址的单元测试
- [x] 5.3 为 `ShopService` 添加编辑店铺地址成功更新字段的单元测试
- [x] 5.4 为 `OrderService` 添加创建订单缺少收货人或地址字段时返回 400 的单元测试
- [x] 5.5 为 `OrderService` 添加创建订单成功保存收货地址快照的单元测试

## 6. 验证命令与手动验收

- [x] 6.1 在 `backend/` 运行 `mvn.cmd test`
- [x] 6.2 在 `frontend/` 运行 `npm.cmd run build`
- [ ] 6.3 手动验证：卖家注册新店铺时地址字段必填，提交后店铺详情展示完整地址
- [ ] 6.4 手动验证：卖家编辑店铺地址后，店铺详情和店铺公开页展示新地址
- [ ] 6.5 手动验证：买家下单时必须填写收货人、手机号和结构化地址
- [ ] 6.6 手动验证：买家订单详情、卖家订单页和管理员订单详情均显示完整收货地址
