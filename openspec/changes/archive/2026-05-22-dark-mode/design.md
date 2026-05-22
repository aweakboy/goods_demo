## Context

前端基于 Vue 3 + Element Plus + Vite 构建，当前使用橙色品牌主题（`#FF6B35`）。Element Plus 内置支持暗色模式（通过 `dark` class 切换）。现有布局使用硬编码颜色值（`#001529`、`#fff`、`#f0f2f5` 等），需迁移为 CSS 变量以支持主题切换。

## Goals / Non-Goals

**Goals:**
- 实现浅色/深色模式切换，主题偏好持久化至 localStorage
- Element Plus 暗色主题自动切换
- 自定义 CSS 变量覆盖，确保橙色品牌色在深色背景下视觉一致
- 导航栏新增切换图标按钮

**Non-Goals:**
- 跟随系统 `prefers-color-scheme` 自动切换（用户手动控制即可）
- 后端存储主题偏好
- 为每个组件单独设计暗色稿

## Decisions

### 决策 1：使用 Element Plus 内置暗色模式

Element Plus 通过在 `html` 元素添加 `class="dark"` 启用暗色主题，内置 CSS 变量自动切换。

**方案对比**：
- 方案 A（选用）：`html.dark` + CSS 变量覆盖 — 零额外依赖，维护成本低
- 方案 B：独立 CSS 主题文件手动管理 — 维护成本高，与 Element Plus 变量脱节

### 决策 2：使用 Composable 管理主题状态

创建 `useTheme` composable（`frontend/src/composables/useTheme.js`），封装主题切换逻辑和 localStorage 持久化，而非引入新的 Pinia store。

**理由**：主题状态是纯 UI 关注点，无需跨模块共享复杂状态，composable 足够且更轻量。

### 决策 3：全局 CSS 变量定义在 `style.css`

在现有 `frontend/src/style.css`（或等效全局样式文件）中定义 `:root` 和 `.dark` 两套 CSS 变量，组件通过变量引用替换硬编码色值。

## Risks / Trade-offs

- **硬编码色值迁移工作量**：现有视图大量使用 `style` 内联硬编码色值，迁移需逐一替换。→ 优先处理布局组件（AdminLayout、SellerLayout、导航栏），页面级组件次之
- **Element Plus 部分组件深色适配不完整**：少数自定义弹窗/覆盖层可能需要额外 CSS。→ 实现后人工验证关键页面
- **橙色在深色背景下的对比度**：需确保 `#FF6B35` 在深色背景上满足 WCAG AA 标准。→ 深色模式下可适当调整橙色亮度（如 `#FF8C5A`）
