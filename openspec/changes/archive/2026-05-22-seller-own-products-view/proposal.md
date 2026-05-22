## Why

卖家登录后会跳转到 `/products`（全平台商品列表），该页面展示所有卖家的商品。顶部导航中的「商品」链接也指向这个公开列表。这与卖家的身份定位不符——卖家账号本质上就是自己店铺的经营者，不需要浏览其他店铺的商品，首要关注点是管理自己店铺的商品。

当前体验存在两个问题：
1. 卖家登录后落地页是全平台商品列表，第一眼看到的是竞争对手的商品，而非自己的商品；
2. 导航栏的「商品」入口对卖家而言毫无意义（不能购买、只是浏览全平台），反而与「商品管理」入口形成混淆。

## What Changes

- **`LoginView.vue`**：卖家登录后直接跳转到 `/seller/products`（自己的商品管理页），而非公开商品列表
- **`AppHeader.vue`**：卖家角色不再显示指向全平台的「商品」导航链接
- **`router/index.js`**：新增路由守卫，卖家若直接访问 `/products`（如通过书签或手动输入），自动重定向至 `/seller/products`

## Capabilities

### New Capabilities

_无新增能力_

### Modified Capabilities

- `seller-dashboard`：卖家登录入口与导航体验变更——登录后直接进入自己的商品管理，而非全平台商品列表

## Impact

- `frontend/src/views/LoginView.vue`：登录跳转逻辑
- `frontend/src/components/AppHeader.vue`：导航链接显示规则
- `frontend/src/router/index.js`：路由守卫
- 无后端改动，无数据库变更，无破坏性变更
- 买家和未登录用户访问 `/products` 行为完全不变
