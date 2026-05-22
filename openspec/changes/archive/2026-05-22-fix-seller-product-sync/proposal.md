## Why

卖家在「上架新商品」表单提交成功后，后端数据库已写入新商品记录，但跳回「商品管理」列表页时前端不刷新数据，导致卖家看不到刚上架的商品。该问题直接影响卖家对操作结果的感知，属于功能性 bug，需立即修复。

## What Changes

- **ProductFormView.vue**：新建商品成功后，导航回列表前触发列表数据刷新信号
- **SellerProductsView.vue**：在 `onActivated` 生命周期钩子中补充调用 `load()`，确保从其他页面返回时（包括 keep-alive 场景）列表始终展示最新数据

## Capabilities

### New Capabilities

_无新增能力_

### Modified Capabilities

- `seller-dashboard`：「商品上架」场景的前端展示行为变更——上架成功后列表页须立即反映新商品，现有 Scenario「成功上架商品」在前端侧的验收标准需补充「卖家跳回列表后可见新商品」

## Impact

- `frontend/src/views/seller/ProductFormView.vue`：提交逻辑
- `frontend/src/views/seller/SellerProductsView.vue`：列表加载逻辑
- 无 API 变更，无后端改动，无破坏性变更
