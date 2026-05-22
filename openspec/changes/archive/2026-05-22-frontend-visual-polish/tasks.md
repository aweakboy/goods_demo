## 1. 全局品牌主题

- [x] 1.1 新建 `frontend/src/assets/theme.css`：声明品牌 CSS 变量（`--brand-primary: #FF6B2B`、`--brand-dark: #1A2540`、`--brand-gradient: linear-gradient(135deg, #1A2540 0%, #243B55 100%)`），并覆盖 EP 主色变量（`--el-color-primary` 及其 light/dark 系列变为橙色系）
- [x] 1.2 修改 `frontend/index.html`：在 `<head>` 中添加 Google Fonts Inter 字体引入（weights 400/500/600/700），将 `<title>` 改为「优选商城」（或其他合适名称）
- [x] 1.3 修改 `frontend/src/main.js`：在 `import 'element-plus/dist/index.css'` 之后追加 `import '@/assets/theme.css'`
- [x] 1.4 修改 `frontend/src/App.vue`：将 body 背景色 `#f5f5f5` 改为 `#F8F7F5`（暖白），字体族改为 `'Inter', 'Helvetica Neue', Arial, sans-serif`

## 2. 头部导航重设计

- [x] 2.1 修改 `frontend/src/components/AppHeader.vue`：
  - 头部背景改为 `var(--brand-gradient)`（深色渐变）
  - Logo 文字改为白色，悬停保持白色
  - 所有导航链接文字改为 `rgba(255,255,255,0.85)`，悬停变为纯白
  - active 路由链接改为白色 + 底部品牌橙色下划线（`border-bottom: 2px solid var(--brand-primary)`）
  - 用户名下拉改为白色文字
  - box-shadow 改为 `0 2px 12px rgba(0,0,0,0.2)`

## 3. 商品卡片升级

- [x] 3.1 修改 `frontend/src/components/ProductCard.vue`：
  - 添加 `transition: transform 0.2s, box-shadow 0.2s`
  - hover 时 `transform: translateY(-4px)`，`box-shadow: 0 8px 24px rgba(0,0,0,0.12)`，左边框出现 `3px solid var(--brand-primary)`
  - 价格文字改为 `var(--brand-primary)`（橙色），字号 `17px`
  - 商品名称字重改为 `500`

## 4. 商品列表页优化

- [x] 4.1 修改 `frontend/src/views/ProductListView.vue`：在商品网格上方添加 Hero 横幅区块——渐变背景（`var(--brand-gradient)`）、白色主标题（「发现好物，尽在优选」）、副标题（「品质商家 · 安心购物」）、高度约 160px
- [x] 4.2 修改 `frontend/src/views/ProductListView.vue`：搜索/筛选栏改为白色卡片区块（`background: #fff; border-radius: 12px; padding: 16px 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.06)`），让搜索区与商品网格之间有视觉分层

## 5. 登录 / 注册页品牌化

- [x] 5.1 修改 `frontend/src/views/LoginView.vue`：
  - 页面背景改为品牌渐变（`var(--brand-gradient)`）
  - 卡片最大宽度改为 `420px`，加深阴影（`box-shadow: 0 20px 60px rgba(0,0,0,0.15)`），border-radius `12px`
  - 卡片顶部添加品牌 Logo 文字区域（品牌色 `var(--brand-primary)` 的大号字体）
- [x] 5.2 对 `frontend/src/views/RegisterView.vue` 应用同样的品牌化样式（背景渐变、卡片阴影、顶部品牌字）

## 6. 验收

- [ ] 6.1 手动验证：所有 Element Plus 主要按钮（primary）变为橙色，链接 active 态为橙色
- [ ] 6.2 手动验证：导航头部为深色渐变，Logo 和链接为白色
- [ ] 6.3 手动验证：商品列表页顶部显示 Hero 横幅，搜索栏有白色卡片背景
- [ ] 6.4 手动验证：商品卡片 hover 时上浮并显示左边框橙色高亮
- [ ] 6.5 手动验证：登录/注册页背景为深色渐变，卡片浮在其上
- [ ] 6.6 手动验证：管理员后台（`/admin`）视觉无明显异常
