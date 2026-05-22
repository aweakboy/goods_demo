## Context

卖家提交新商品表单后，`ProductFormView.vue` 调用 `productApi.create()` 成功后执行 `router.push('/seller/products')` 跳回列表页。`SellerProductsView.vue` 的数据加载逻辑只在 `onMounted` 中触发。

当前代码在普通前进导航中能正常重挂载，但存在两个问题：
1. `router.push()` 未被 `await`，navigation 与 `finally` 块并发，极端情况下可能造成竞态；
2. 若将来引入 `<keep-alive>`（路由优化常见手段），`onMounted` 将不再在返回时触发，`onActivated` 缺失导致数据永久陈旧。

用户反馈的"前端未同步"最可能是路由组件未完整重建（浏览器 bfcache、keep-alive 试验版本）或 `router.push` 未 await 导致组件已切换但 `load()` 时序偏移。

## Goals / Non-Goals

**Goals:**
- 保证从「上架新商品」表单提交成功返回列表后，列表立即展示新商品
- 修复方式对现有代码改动最小，无需引入新依赖或全局状态管理

**Non-Goals:**
- 不引入 Pinia product store（需求不复杂到需要全局缓存）
- 不改动后端接口
- 不处理编辑商品后的列表刷新（该路径已有独立 `load()` 触发，且本次 bug 仅涉及新建）

## Decisions

### Decision 1: 在 SellerProductsView 中同时使用 onMounted + onActivated

**选择**：在 `SellerProductsView.vue` 加入 `onActivated(load)`，与现有 `onMounted(load)` 共存。

**理由**：`onMounted` 处理首次挂载（无 keep-alive）；`onActivated` 处理 keep-alive 场景下的激活。两者共存对正常挂载无副作用，同时对 keep-alive 场景提供兜底。改动极小（1 行代码）。

**备选方案**：
- 使用 `watch(route, load)` 监听路由变化——过于宽泛，任何参数变化都会触发；
- 在 `ProductFormView` 提交后直接更新全局 store——增加复杂度，此处无需全局共享；
- `router.go(0)` 强制刷新页面——用户体验差，有闪烁。

### Decision 2: await router.push()

**选择**：将 `ProductFormView.vue` 中的 `router.push('/seller/products')` 改为 `await router.push('/seller/products')`。

**理由**：`router.push()` 返回 Promise，不 await 意味着导航与 `finally` 并发执行，虽然通常不影响结果，但 await 使执行顺序明确：先完成导航（目标组件已挂载并开始 `load()`）再执行 `finally`。语义更清晰，消除潜在竞态。

## Risks / Trade-offs

- `onMounted + onActivated` 双触发：若组件在 keep-alive 内首次挂载，两个钩子都会触发，导致 `load()` 被调用两次。两次 API 调用返回相同数据，后者覆盖前者，无功能影响，仅多一次网络请求。属可接受的轻微代价。→ 无需额外处理。

## Migration Plan

1. 修改 `frontend/src/views/seller/SellerProductsView.vue`：导入 `onActivated`，添加 `onActivated(load)`
2. 修改 `frontend/src/views/seller/ProductFormView.vue`：`router.push` 前加 `await`
3. 手动验证：新建商品 → 跳回列表 → 新商品可见

## Open Questions

无
