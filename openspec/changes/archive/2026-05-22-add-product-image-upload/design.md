## Context

商品实体 `Product.imageUrl` 已有 `VARCHAR(500)` 字段，前端 `ProductFormView.vue` 通过 `el-input` 接收卖家手动输入的图片 URL，提交时与其他字段一起以 JSON 格式发送至 `POST /api/v1/seller/products`。

当前架构中后端无文件上传逻辑，`application.yml` 无 multipart 配置，服务器亦无静态资源目录。本次改动在不变更现有商品保存接口的前提下，通过引入独立的图片上传端点，将上传与表单提交解耦。

## Goals / Non-Goals

**Goals:**
- 卖家可在商品表单中选取本地图片，上传后预览，提交时图片 URL 自动填入
- 后端安全存储上传图片，限制文件类型（JPEG/PNG/GIF/WEBP）与大小（最大 5 MB）
- 上传图片通过固定 URL 路径可访问，与现有 `imageUrl` 字段完全兼容

**Non-Goals:**
- 不实现图片管理（删除、替换孤立文件的清理机制）
- 不集成云存储（OSS/S3），仅本地磁盘存储
- 不修改商品保存接口，保持 JSON 请求体不变
- 不为买家或管理员增加上传入口

## Decisions

### Decision 1: 先上传后提交（pre-upload）模式

**选择**：图片上传与商品保存分为两步——卖家选文件时立即调用上传接口，得到 URL 后回填到 `form.imageUrl`，再随其他字段以 JSON 提交。

**理由**：现有商品保存接口使用 `@RequestBody ProductRequest`（JSON），无需改为 multipart。两步解耦使图片上传可独立重试，且支持提交前预览。与 Element Plus `el-upload` 的 `action` + `on-success` 钩子天然契合，实现最简单。

**备选方案**：
- 表单提交时合并为单次 multipart 请求——需同时修改后端接收方式与前端 axios 调用，改动面大，且无法在提交前预览；
- Base64 内嵌到 JSON——图片体积膨胀约 33%，占用 imageUrl 字段限制（500 chars），不适用。

### Decision 2: 服务端本地存储 + Spring 静态资源映射

**选择**：上传文件保存至 `{app.upload-dir}/`（默认 `./uploads/`，通过 `application.yml` 配置），Spring Boot 通过 `WebMvcConfigurer.addResourceHandlers` 将该目录映射为 `/uploads/**`，图片 URL 格式为 `http://localhost:8080/uploads/<uuid>.<ext>`。

**理由**：开发环境最简方案，无需额外依赖。`imageUrl` 字段语义不变（仍是 URL 字符串），现有展示逻辑完全兼容。

**备选方案**：
- 存入 `resources/static/uploads/`——打包后路径固定在 JAR 内，无法在运行时写入；
- 数据库 BLOB——图片占用 DB 空间，影响查询性能，读取需额外端点。

### Decision 3: 文件命名使用 UUID

**选择**：保存时重命名为 `<UUID>.<ext>`，不保留原始文件名。

**理由**：避免文件名冲突；消除路径遍历风险（不信任客户端文件名）；UUID 不暴露业务语义。

### Decision 4: 前端使用 el-upload 自定义上传

**选择**：使用 `el-upload` 组件，设置 `:http-request` 自定义上传函数（而非 `action` 属性），由前端手动携带 JWT 请求头调用上传接口。

**理由**：现有 axios 实例已配置 JWT 拦截器，复用更安全；`action` 属性方式绕过 axios 拦截器，需手动处理认证。

## Risks / Trade-offs

- **磁盘空间**：无清理机制，废弃图片（表单未提交、商品已删除）长期积累占用磁盘。属已知 Non-Goal，可在后续迭代中添加定时清理任务。
- **开发/生产路径差异**：默认使用相对路径 `./uploads/`，生产部署时需在 `application.yml` 中配置绝对路径。在 `application.yml` 中添加注释说明。
- **文件大小默认限制**：Spring Boot 默认 multipart 上限为 1 MB，需在配置中显式提升至 5 MB，否则上传失败。

## Migration Plan

1. 后端：新建 `FileStorageService`，封装目录初始化、UUID 命名、文件写入逻辑
2. 后端：在 `SellerController` 新增 `POST /api/v1/seller/products/upload-image` 端点
3. 后端：新建 `WebMvcConfig`（或在现有配置类中）添加 `/uploads/**` 静态资源映射
4. 后端：`application.yml` 新增 multipart 大小限制与 `app.upload-dir` 配置项
5. 前端：`product.js` 新增 `uploadProductImage(file)` 函数
6. 前端：`ProductFormView.vue` 将 `el-input` 替换为 `el-upload`，实现预览与上传逻辑
7. 手动验证：选图→预览→提交→商品列表展示上传图片

## Open Questions

无
