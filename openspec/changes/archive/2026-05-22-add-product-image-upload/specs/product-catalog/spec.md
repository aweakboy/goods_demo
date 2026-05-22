## MODIFIED Requirements

### Requirement: 商品详情页
商品详情页 SHALL 展示商品图片，图片来源可为外部 URL 或本次新增的服务端上传路径，两者均通过 `imageUrl` 字段统一渲染，展示逻辑不变。

#### Scenario: 展示本地上传图片
- **WHEN** 买家访问图片为服务端上传路径（`/uploads/<uuid>.<ext>`）的商品详情页
- **THEN** 图片正常加载展示，与外部 URL 图片显示效果一致
