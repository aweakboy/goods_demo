## 1. 基础设施 - CSS 变量与全局样式

- [x] 1.1 在 `frontend/src/style.css` 中定义 `:root` 浅色模式 CSS 变量（背景色、文字色、边框色、品牌色）
- [x] 1.2 在 `frontend/src/style.css` 中定义 `.dark` 深色模式 CSS 变量覆盖
- [x] 1.3 引入 Element Plus 暗色主题样式（`element-plus/theme-chalk/dark/css-vars.css`）

## 2. 主题状态管理

- [x] 2.1 创建 `frontend/src/composables/useTheme.js`，实现 `isDark` 响应式状态
- [x] 2.2 实现 `toggleTheme()` 函数：切换 `html` 元素的 `dark` class
- [x] 2.3 实现 localStorage 持久化：读取初始值、写入变更
- [x] 2.4 在应用入口（`App.vue` 或 `main.js`）初始化时加载保存的主题偏好

## 3. 切换按钮 UI

- [x] 3.1 在买家端顶部导航栏（`ProductListView.vue` 或全局布局）添加主题切换按钮（月亮/太阳图标）
- [x] 3.2 在管理后台 `AdminLayout.vue` 的 header 区域添加主题切换按钮
- [x] 3.3 在卖家后台布局中添加主题切换按钮
- [x] 3.4 按钮根据 `isDark` 状态动态显示 `Moon` / `Sunny` 图标

## 4. 布局组件深色适配

- [x] 4.1 `AdminLayout.vue`：将硬编码色值（`#001529`、`#fff`、`#f0f2f5`）替换为 CSS 变量
- [x] 4.2 卖家后台布局组件：同步替换硬编码色值
- [x] 4.3 全局导航/顶部栏：替换背景色、边框色为 CSS 变量

## 5. 验证与测试

- [x] 5.1 验证切换按钮在所有三种角色界面（买家/卖家/管理员）正常工作
- [x] 5.2 验证刷新后主题偏好正确恢复
- [x] 5.3 验证 Element Plus 组件（表格、弹窗、表单）在深色模式下显示正常
- [x] 5.4 检查橙色品牌色在深色背景下的对比度，必要时调整深色模式下的品牌色变量
