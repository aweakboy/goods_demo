## 1. 后端 — 文件存储服务

- [x] 1.1 新建 `backend/src/main/java/com/trading/service/FileStorageService.java`：注入 `app.upload-dir` 配置，提供 `store(MultipartFile file): String` 方法——校验文件类型（JPEG/PNG/GIF/WEBP）、大小（≤5 MB），以 `<UUID>.<ext>` 命名保存至上传目录，返回相对路径文件名
- [x] 1.2 `application.yml` 新增配置：`spring.servlet.multipart.max-file-size: 5MB`、`spring.servlet.multipart.max-request-size: 5MB`、`app.upload-dir: ./uploads`

## 2. 后端 — 图片上传端点

- [x] 2.1 在 `SellerController.java` 新增端点 `POST /api/v1/seller/products/upload-image`：接收 `@RequestParam("file") MultipartFile`，调用 `FileStorageService.store()`，返回 `{ "url": "/uploads/<filename>" }`（`Map<String, String>`）
- [x] 2.2 新建 `backend/src/main/java/com/trading/config/WebMvcConfig.java`（实现 `WebMvcConfigurer`）：`addResourceHandlers` 将 `app.upload-dir` 目录映射为 `/uploads/**`，并在 `@PostConstruct` 中确保目录存在

## 3. 前端 — API 封装

- [x] 3.1 在 `frontend/src/api/product.js` 新增 `uploadProductImage(file)` 函数：使用 `FormData` 构造请求体，调用 `POST /api/v1/seller/products/upload-image`，返回响应中的 `url` 字段

## 4. 前端 — 表单组件改造

- [x] 4.1 在 `frontend/src/views/seller/ProductFormView.vue` 中，将「图片URL」`el-input` 替换为 `el-upload` 组件：
  - `:http-request` 绑定自定义上传函数，调用 `uploadProductImage`
  - `list-type="picture-card"` 展示图片预览
  - `:limit="1"` 只允许一张图片
  - `accept="image/jpeg,image/png,image/gif,image/webp"` 限制文件类型
  - 上传成功后将返回的 `url` 写入 `form.imageUrl`，上传失败时用 `ElMessage.error` 提示
- [x] 4.2 保留兼容：`form.imageUrl` 初始值若为非空字符串（编辑模式加载现有图片），在 `el-upload` 的 `:file-list` 中展示已有图片

## 5. 验收

- [ ] 5.1 手动验证：卖家在新建商品页选取本地图片，图片立即出现在预览区
- [ ] 5.2 手动验证：提交表单后，商品列表与详情页展示上传的图片（非占位图）
- [ ] 5.3 手动验证：编辑已有外部 URL 图片的商品，原图片正常显示，可替换为本地上传
- [ ] 5.4 手动验证：上传超过 5 MB 的文件时，前端或后端返回明确的错误提示
