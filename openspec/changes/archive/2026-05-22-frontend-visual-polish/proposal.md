## Why

当前前端完全使用 Element Plus 默认样式（主色 `#409eff`、背景 `#f5f5f5`），没有任何品牌定制，视觉上与所有开箱即用的 EP 项目无法区分。一个有吸引力的交易平台需要清晰的品牌色系、更有层次感的页面结构和更精致的组件细节，才能让用户产生信任感并留下印象。

## What Changes

- **全局品牌色系**：引入橙色（`#FF6B2B`）为主品牌色 + 深海军蓝（`#1A2540`）为强调色，通过覆盖 Element Plus CSS 变量全局生效，替换默认蓝
- **导航头部重设计**：从纯白改为深色渐变品牌头部，Logo 和导航链接使用白色，整体形成强对比、有质感的顶栏
- **商品卡片升级**：添加悬停上浮动效、橙色左边框高亮、价格标签视觉强化
- **商品列表页**：顶部加入品牌 Hero 横幅区块，搜索栏改为圆角白底聚焦样式
- **登录/注册页**：卡片加宽加深阴影、顶部品牌色条带，视觉更有仪式感
- **全局字体与排版**：引入 Inter 字体，优化行高与间距

## Capabilities

### New Capabilities

_无新增能力_

### Modified Capabilities

_无功能变更，纯视觉层改动_

## Impact

- `frontend/index.html`：引入 Inter 字体、更新页面标题
- `frontend/src/assets/theme.css`（新建）：品牌 CSS 变量、EP 主色覆盖、全局基础样式
- `frontend/src/main.js`：引入 theme.css
- `frontend/src/App.vue`：更新全局背景色
- `frontend/src/components/AppHeader.vue`：深色品牌头部重设计
- `frontend/src/components/ProductCard.vue`：悬停效果、价格标签升级
- `frontend/src/views/ProductListView.vue`：Hero 横幅、搜索栏优化
- `frontend/src/views/LoginView.vue`、`RegisterView.vue`：品牌化登录/注册页
- 无后端改动，无 API 变更，无破坏性变更
