## MODIFIED Requirements

### Requirement: 商品上架
系统 SHALL 允许已登录且已注册店铺的卖家创建新商品，商品图片可通过本地文件上传或保留外部 URL 方式设置。

#### Scenario: 卖家上传本地图片
- **WHEN** 卖家在商品表单中点击图片上传区域并选取本地图片文件（JPEG/PNG/GIF/WEBP，≤5 MB）
- **THEN** 图片立即上传至服务器，上传区域展示图片预览，`imageUrl` 自动填入服务端返回的图片地址

#### Scenario: 上传文件类型不合法
- **WHEN** 卖家尝试上传非图片类型文件（如 .pdf、.txt）
- **THEN** 前端拒绝选取，提示"仅支持 JPEG、PNG、GIF、WEBP 格式"

#### Scenario: 上传文件超过大小限制
- **WHEN** 卖家上传超过 5 MB 的图片文件
- **THEN** 系统返回错误，前端展示"图片大小不能超过 5 MB"提示，表单图片字段保持空

#### Scenario: 编辑模式下展示已有图片
- **WHEN** 卖家打开已有图片 URL 的商品编辑页
- **THEN** 图片上传组件预览区展示现有图片，卖家可选择保留或替换
