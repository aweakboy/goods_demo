## Context

登录成功后，`LoginView.vue` 将所有非管理员用户一律跳转至 `/`，而 `/` 通过 `router/index.js` 的 `redirect: '/products'` 重定向到公开商品列表。卖家因此每次登录都先看到全平台商品。

`AppHeader.vue` 中的「商品」链接（`<router-link to="/products">`）无角色区分，卖家也会看到它，但该链接对卖家无实际价值。卖家的商品管理入口（`/seller/products`）与公开商品链接并存，产生语义混淆。

`router/index.js` 的 `beforeEach` 守卫目前只处理「未登录跳登录页」和「角色不符跳首页」两个场景，未针对卖家访问公开商品列表做处理。

## Goals / Non-Goals

**Goals:**
- 卖家登录后直接落地到自己的商品管理页
- 卖家导航栏不再展示无意义的全平台「商品」入口
- 卖家直接访问 `/products` 时自动跳转（防止通过书签或分享链接误入）

**Non-Goals:**
- 不禁止卖家通过商品详情跳转等间接方式访问公开商品（不强行封锁，只优化主动入口）
- 不修改公开商品列表页本身
- 不修改后端

## Decisions

### Decision 1: 登录跳转分三路

**选择**：`LoginView.vue` 中将跳转逻辑改为：
- ADMIN → `/admin/overview`（已有）
- SELLER → `/seller/products`（新增）
- 其他（BUYER）→ `/`（保持不变）

**理由**：最直接的入口控制，一处改动即可解决登录落地问题。无需引入全局状态或中间件。

### Decision 2: AppHeader 隐藏卖家的「商品」链接

**选择**：为「商品」`router-link` 添加 `v-if="userStore.role !== 'SELLER'"`，卖家登录后该链接不渲染。

**理由**：卖家已有「商品管理」入口，全平台「商品」对卖家无用且产生混淆。直接隐藏比修改链接目标更干净——卖家的商品视图入口就是「商品管理」，语义明确。

**备选方案**：将「商品」链接对卖家指向 `/seller/products`——与「商品管理」重复，多余。

### Decision 3: router 守卫拦截卖家访问 /products

**选择**：在 `beforeEach` 守卫末尾补充：若已登录卖家访问 `/products`，返回 `/seller/products`。

**理由**：防止通过书签、分享链接或手动输入 URL 绕过主动入口控制。与现有守卫结构一致，加一行即可。

## Risks / Trade-offs

- 卖家无法通过常规导航浏览全平台商品（如作竞品参考）。属于有意为之的设计选择，符合「卖家=店铺经营者」定位。若将来有需求，可在卖家视图中独立提供「浏览全平台」入口。
- 三处文件改动相互独立，任一单独生效都有意义，无强依赖。

## Migration Plan

1. 修改 `LoginView.vue` 的登录跳转逻辑
2. 修改 `AppHeader.vue` 的「商品」链接显示条件
3. 修改 `router/index.js` 的 `beforeEach` 守卫

## Open Questions

无
