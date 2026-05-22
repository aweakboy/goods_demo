## 1. 修复 SellerProductsView 列表刷新

- [x] 1.1 在 `frontend/src/views/seller/SellerProductsView.vue` 中从 `vue` 导入 `onActivated`
- [x] 1.2 在 `onMounted(load)` 之后添加 `onActivated(load)`，确保 keep-alive 场景下返回列表时触发数据刷新

## 2. 修复 ProductFormView 导航时序

- [x] 2.1 在 `frontend/src/views/seller/ProductFormView.vue` 的 `submit` 函数中，将 `router.push('/seller/products')` 改为 `await router.push('/seller/products')`，消除导航与 finally 块并发的潜在竞态

## 3. 验证

- [x] 3.1 启动前后端，以卖家身份登录，填写新商品表单并提交，确认跳回列表后新商品立即可见
- [x] 3.2 确认编辑已有商品保存后列表展示未受影响
