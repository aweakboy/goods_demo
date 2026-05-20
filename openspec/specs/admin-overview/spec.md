## Purpose

平台数据总览：为 ADMIN 提供平台核心指标的聚合视图，包括用户数、商品数、订单数及总成交额。

## Requirements

### Requirement: 平台数据总览
系统 SHALL 为 ADMIN 提供平台核心指标的聚合视图，数据实时从数据库查询。

#### Scenario: 查看仪表盘数据
- **WHEN** 管理员访问数据总览页面
- **THEN** 系统返回：注册用户总数、商品总数（上架中）、今日新增订单数、平台历史总成交额（COMPLETED 状态订单的 totalAmount 之和）

#### Scenario: 数据总览各项无数据时
- **WHEN** 对应数据为零（如平台刚初始化）
- **THEN** 系统返回各项指标值为 0，不报错
