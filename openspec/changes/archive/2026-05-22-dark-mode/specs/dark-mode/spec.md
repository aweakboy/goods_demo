## ADDED Requirements

### Requirement: 用户可切换深色/浅色模式
系统 SHALL 在导航栏提供主题切换按钮，允许用户在深色模式和浅色模式之间切换。

#### Scenario: 切换至深色模式
- **WHEN** 用户点击主题切换按钮且当前为浅色模式
- **THEN** 界面切换为深色主题，按钮图标变为太阳图标

#### Scenario: 切换至浅色模式
- **WHEN** 用户点击主题切换按钮且当前为深色模式
- **THEN** 界面切换为浅色主题，按钮图标变为月亮图标

### Requirement: 主题偏好持久化
系统 SHALL 将用户的主题偏好保存至 localStorage，确保页面刷新或重新访问后主题保持一致。

#### Scenario: 刷新后恢复深色模式
- **WHEN** 用户已选择深色模式并刷新页面
- **THEN** 页面加载后自动应用深色主题，无需重新切换

#### Scenario: 首次访问默认浅色模式
- **WHEN** 用户首次访问且 localStorage 中无主题偏好记录
- **THEN** 系统默认显示浅色主题

### Requirement: Element Plus 组件适配深色主题
系统 SHALL 确保 Element Plus 组件（表格、表单、弹窗、菜单等）在深色模式下正确显示暗色样式。

#### Scenario: 深色模式下表格正常显示
- **WHEN** 用户切换至深色模式
- **THEN** 所有 Element Plus 表格、卡片、对话框组件均显示深色背景和浅色文字

### Requirement: 品牌色在深色背景下的视觉适配
系统 SHALL 确保橙色品牌色（`#FF6B35`）在深色背景下具有足够的对比度，满足可读性要求。

#### Scenario: 深色模式下主按钮可辨识
- **WHEN** 用户切换至深色模式
- **THEN** 主操作按钮（橙色）在深色背景上清晰可见，文字对比度满足 WCAG AA 标准（≥4.5:1）
