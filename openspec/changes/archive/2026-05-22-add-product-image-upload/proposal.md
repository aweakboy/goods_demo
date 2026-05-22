## Why

卖家在「上架商品」表单中，图片字段目前要求手动填写外部图片 URL（如 CDN 链接）。这对非技术型卖家极不友好——他们通常没有现成的图床链接，导致该字段常被忽略，商品列表呈现占位图，影响买家浏览体验。本次改动允许卖家直接从本地选取图片文件并上传，系统自动处理存储与 URL 回填，消除手动填 URL 的摩擦。

## What Changes

- **后端**：新增图片上传端点 `POST /api/v1/seller/products/upload-image`，接收 `multipart/form-data` 格式文件，校验类型与大小后保存至服务器本地目录，返回可访问的图片 URL
- **后端**：配置 Spring Boot 将上传目录暴露为静态资源，使图片可直接通过 HTTP 访问
- **前端 ProductFormView.vue**：将图片 URL 文本输入替换为 `el-upload` 上传组件，支持选图即上传、预览、重新选择

## Capabilities

### New Capabilities

_无新增能力_

### Modified Capabilities

- `seller-dashboard`：「商品上架」中的图片设置交互方式变更——从手动输入 URL 改为本地文件上传
- `product-catalog`：商品展示能力不变，但商品图片来源由外部 URL 扩展为可包含本地上传图片的服务端 URL

## Impact

- `backend/src/main/java/com/trading/controller/SellerController.java`：新增文件上传端点
- `backend/src/main/java/com/trading/service/FileStorageService.java`：新建，封装文件存储逻辑
- `backend/src/main/resources/application.yml`：新增 multipart 大小限制与上传目录配置
- `frontend/src/views/seller/ProductFormView.vue`：替换图片输入组件
- `frontend/src/api/product.js`：新增上传图片 API 调用
- 无数据库结构变更（`image_url` 字段语义不变，仍存储 URL 字符串）
- 无破坏性变更，现有使用外部 URL 的商品不受影响
