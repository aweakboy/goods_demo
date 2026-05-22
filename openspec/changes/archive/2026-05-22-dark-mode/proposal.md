## Why

用户长时间使用平台时，强光白色界面造成视觉疲劳；夜间使用场景下深色模式可显著提升舒适度并节省 OLED 屏幕电量。

## What Changes

- 新增全局深色主题，用户可手动切换浅色/深色模式
- 主题偏好持久化到 localStorage，刷新后保留用户选择
- 顶部导航栏新增主题切换按钮（太阳/月亮图标）
- Element Plus 组件库切换至对应的暗色主题
- 自定义 CSS 变量覆盖以适配平台橙色品牌色在深色背景下的视觉呈现

## Capabilities

### New Capabilities

- `dark-mode`: 深色模式主题切换功能，包含主题状态管理、持久化、UI 切换控件及全局样式适配

### Modified Capabilities

<!-- 无现有能力规格需要更新，深色模式为纯前端新增功能 -->

## Impact

- **前端**：`frontend/src/` 下的所有视图组件、布局组件、全局样式文件
- **状态管理**：新增 theme store（Pinia）或 composable
- **依赖**：Element Plus 内置暗色模式支持（无需额外依赖）
- **无后端改动**：纯前端功能，不涉及 API 或数据库变更
