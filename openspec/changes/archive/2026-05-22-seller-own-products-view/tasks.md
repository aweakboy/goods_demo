## 1. 登录跳转

- [x] 1.1 修改 `frontend/src/views/LoginView.vue` 的 `handleLogin` 函数：将 `router.push(res.data.role === 'ADMIN' ? '/admin/overview' : '/')` 改为三路跳转——ADMIN → `/admin/overview`，SELLER → `/seller/products`，其他 → `/`

## 2. 导航栏

- [x] 2.1 修改 `frontend/src/components/AppHeader.vue`：为「商品」`<router-link to="/products">` 添加 `v-if="userStore.role !== 'SELLER'"`，使卖家登录后该链接不显示

## 3. 路由守卫

- [x] 3.1 修改 `frontend/src/router/index.js` 的 `beforeEach` 守卫：在现有逻辑末尾追加——若 `to.path === '/products'` 且已登录用户角色为 `SELLER`，返回 `/seller/products`

## 4. 验收

- [ ] 4.1 手动验证：卖家账号登录后直接跳转到 `/seller/products`（商品管理页）
- [ ] 4.2 手动验证：卖家登录状态下，导航栏不显示「商品」链接，只显示「我的店铺」「商品管理」「订单管理」
- [ ] 4.3 手动验证：卖家在浏览器地址栏直接输入 `/products`，自动跳转至 `/seller/products`
- [ ] 4.4 手动验证：买家账号登录后仍跳转到 `/`（公开商品列表），导航栏「商品」链接正常显示
