## Context

前端目前零自定义样式，所有视觉均来自 Element Plus 2.4.4 默认主题。EP 主色为 `#409eff`，全局背景 `#f5f5f5`，头部纯白。所有 Vue 组件使用 `<style scoped>` 写少量布局 CSS，无全局 CSS 文件，无字体引入，无品牌变量。

EP 支持通过覆盖 `:root` 层的 CSS 自定义属性（如 `--el-color-primary`）来全局替换主色，无需修改 node_modules 或使用 SASS 编译，是最轻量的主题定制方案。

## Goals / Non-Goals

**Goals:**
- 建立清晰可辨识的品牌色系，主色替换为橙色，与行业主流电商平台色调一致（活力、可信）
- 头部导航视觉升级为深色品牌风格，强化平台辨识度
- 商品卡片和列表页视觉细节优化，提升浏览体验
- 登录/注册页面品牌化，减少"裸 EP"感
- 所有改动纯 CSS / template 层，不影响任何业务逻辑

**Non-Goals:**
- 不重构页面布局和组件结构
- 不修改 Element Plus 组件 API 使用方式
- 不引入动画库或图标库
- 不处理移动端响应式适配（现有适配不变）

## Decisions

### Decision 1: 品牌色选择 — 橙色系

**选择**：主品牌色 `#FF6B2B`（活力橙），深色强调 `#1A2540`（深海军蓝）。

**理由**：橙色在电商领域有强烈的转化联想（淘宝、Amazon 均用橙/黄色作为 CTA 色）。与默认蓝色形成明显对比，具有辨识度。深海军蓝作为头部背景，与橙色形成高端对比，避免整体过于"热闹"。

**EP 主色覆盖变量**：
```css
--el-color-primary: #FF6B2B
--el-color-primary-light-3: #ff9266
--el-color-primary-light-5: #ffb591
--el-color-primary-light-7: #ffd4ba
--el-color-primary-light-8: #ffe3d3
--el-color-primary-light-9: #fff1eb
--el-color-primary-dark-2: #d45420
```

### Decision 2: 深色渐变头部

**选择**：AppHeader 背景改为 `linear-gradient(135deg, #1A2540 0%, #243B55 100%)`，Logo 和导航链接改为白色，active 状态改为品牌橙色下划线。

**理由**：深色头部是现代电商平台（京东、拼多多深色导航）的常见选择，视觉重量感强，与浅色内容区形成明显分层。渐变比纯色更有质感，避免沉闷。

### Decision 3: CSS 变量集中管理

**选择**：新建 `frontend/src/assets/theme.css`，在 `:root` 中声明所有品牌变量，并在此文件中覆盖 EP CSS 变量。`main.js` 中在 EP 样式之后引入此文件，确保覆盖顺序正确。

**理由**：集中管理便于后续调整品牌色，不污染各组件的 scoped styles。引入顺序（EP 之后）确保 CSS specificity 正确覆盖。

### Decision 4: 商品卡片悬停效果

**选择**：hover 时 `transform: translateY(-4px)`，`box-shadow` 增强，左边框出现品牌橙色 `3px solid var(--brand-primary)`。

**理由**：轻微上浮是电商卡片最常见的交互反馈，成本低（纯 CSS transition），让用户感知到可交互性。边框颜色呼应品牌色，强化视觉一致性。

### Decision 5: Inter 字体

**选择**：通过 Google Fonts CDN 引入 Inter 字体（weights: 400, 500, 600, 700），在 theme.css 中设置为 `font-family`。

**理由**：Inter 是目前最广泛使用的现代界面字体，在中文+英文混排场景下表现优异，数字和拉丁字符清晰美观，不影响中文字体回退（仍使用系统中文字体）。

## Risks / Trade-offs

- **Google Fonts CDN**：需要网络访问，国内环境可能加载慢。备用方案：字体文件本地化，或直接删除 Inter 引入，保持系统字体。风险低——字体加载失败只影响西文字符渲染，不影响功能。
- **EP 主色覆盖**：CSS 变量覆盖影响全局所有 EP 组件（按钮、链接、选中态等）。需测试所有角色（买家/卖家/管理员）的关键页面，确认无明显视觉 bug。
- **管理员后台**：AdminLayout 有独立的深色侧边栏（`#001529`），与新品牌色可能产生轻微冲突，但因管理员侧边栏已是深色，视觉影响极小，本次不调整。

## Migration Plan

1. 新建 `theme.css`，定义品牌变量和 EP 覆盖
2. `index.html` 引入 Inter 字体，更新 title
3. `main.js` 引入 `theme.css`
4. `App.vue` 更新背景色使用 CSS 变量
5. `AppHeader.vue` 深色头部重设计
6. `ProductCard.vue` 悬停效果升级
7. `ProductListView.vue` Hero 横幅 + 搜索栏优化
8. `LoginView.vue` / `RegisterView.vue` 品牌化

## Open Questions

无
