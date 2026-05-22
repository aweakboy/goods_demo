# shop-favorites Specification

## Purpose
TBD - created by archiving change add-shop-favorites-price-alerts. Update Purpose after archive.
## Requirements
### Requirement: 买家收藏店铺
系统 SHALL 允许已登录买家收藏 ACTIVE 店铺，并保证同一买家对同一店铺最多只有一条收藏记录。

#### Scenario: 收藏有效店铺
- **WHEN** 已登录买家收藏一个 ACTIVE 店铺
- **THEN** 系统创建收藏记录并返回收藏状态为已收藏

#### Scenario: 重复收藏同一店铺
- **WHEN** 已登录买家重复收藏同一店铺
- **THEN** 系统保持单条收藏记录并返回收藏状态为已收藏

#### Scenario: 收藏不存在或关闭的店铺
- **WHEN** 已登录买家收藏不存在或 INACTIVE 店铺
- **THEN** 系统返回 404 或明确提示店铺不可收藏，且不创建收藏记录

#### Scenario: 非买家收藏店铺
- **WHEN** SELLER、ADMIN 或未登录用户调用收藏店铺接口
- **THEN** 系统拒绝请求，返回 401 或 403

### Requirement: 买家取消店铺收藏
系统 SHALL 允许已登录买家取消自己已收藏的店铺，且取消操作 MUST 具备幂等性。

#### Scenario: 取消已收藏店铺
- **WHEN** 已登录买家取消收藏已收藏店铺
- **THEN** 系统删除或停用对应收藏记录，并返回收藏状态为未收藏

#### Scenario: 重复取消收藏
- **WHEN** 已登录买家重复取消同一店铺收藏
- **THEN** 系统返回收藏状态为未收藏，且不影响其他买家的收藏记录

### Requirement: 查看收藏店铺列表
系统 SHALL 允许已登录买家分页查看自己的收藏店铺列表，并只返回该买家有权查看的店铺信息。

#### Scenario: 查看我的收藏店铺
- **WHEN** 已登录买家访问收藏店铺列表
- **THEN** 系统返回该买家收藏的店铺，包含店铺 ID、名称、简介、状态、收藏时间和可访问状态

#### Scenario: 收藏店铺已关闭
- **WHEN** 买家收藏的店铺后来被管理员设置为 INACTIVE
- **THEN** 系统不得展示该店铺为可进入状态，且不得将该店铺的商品作为 ACTIVE 店铺商品展示

#### Scenario: 收藏列表隔离
- **WHEN** 买家查看收藏店铺列表
- **THEN** 系统只返回当前买家的收藏，不返回其他买家的收藏记录

### Requirement: 店铺页面展示收藏状态
系统 SHALL 在店铺主页返回店铺收藏摘要，使前端能够展示收藏按钮状态和收藏数量。

#### Scenario: 买家查看已收藏店铺主页
- **WHEN** 已登录买家访问已收藏的 ACTIVE 店铺主页
- **THEN** 系统返回 `favorited=true` 和该店铺收藏数量

#### Scenario: 买家查看未收藏店铺主页
- **WHEN** 已登录买家访问未收藏的 ACTIVE 店铺主页
- **THEN** 系统返回 `favorited=false` 和该店铺收藏数量

#### Scenario: 访客查看店铺主页
- **WHEN** 未登录访客访问 ACTIVE 店铺主页
- **THEN** 系统返回店铺信息和收藏数量，并将当前用户收藏状态视为未收藏

