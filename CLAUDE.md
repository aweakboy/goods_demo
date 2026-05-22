# Claude Code 工作指引

## 项目概述

Spring Boot 3.2 + Vue 3 的商品交易平台，含买家/卖家/管理员三种角色。

- 后端：`backend/`（Java 17, Spring Boot, MySQL, JWT）
- 前端：`frontend/`（Vue 3, Vite, Element Plus）
- 规格文档：`openspec/specs/`

## OpenSpec 开发规范

本项目使用 OpenSpec 管理功能变更。

### 标准完整流程

```
/opsx:propose  → 生成变更提案（proposal + design + specs + tasks）
/opsx:apply    → 实现任务
/opsx:verify   → 验证实现与变更产物匹配
/opsx:sync     → 将 delta specs 合并到主 openspec/specs/
git commit     → 提交代码
/opsx:archive  → 归档变更
```

### 当前工作约定（学习阶段，节省 token）

用户目前处于学习 OpenSpec 阶段，**跳过 verify 和 sync**，简化为：

```
/opsx:propose → /opsx:apply → git commit → /opsx:archive
```

verify 和 sync 不是强制步骤，主要用于团队协作时保证文档与实现一致。

### 变更文件位置

- 进行中的变更：`openspec/changes/<name>/`
- 主规格文档：`openspec/specs/<capability>/spec.md`

## 支付宝沙箱配置

- 沙箱网关：`https://openapi-sandbox.dl.alipaydev.com/gateway.do`
- 本地开发需用 ngrok 暴露回调地址：`ngrok http 8080`
- 配置文件：`backend/src/main/resources/application.yml` 中的 `alipay.*`
- notify-url 每次重启 ngrok 后需更新
