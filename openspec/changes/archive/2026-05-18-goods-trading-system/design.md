## Context

商品交易系统是一个全栈电商平台，前端基于 Vue 3，后端基于 Spring Boot 3。系统支持买家浏览商品、加入购物车、下单支付，以及卖家管理商品和处理订单。前后端通过 RESTful API 通信，采用 JWT 进行无状态身份认证。

## Goals / Non-Goals

**Goals:**
- 提供完整的买卖双方交易闭环（浏览→购物车→下单→发货→完成）
- 基于角色的访问控制（买家 / 卖家 / 管理员）
- 前后端分离架构，API 可独立测试
- 核心模块单元测试覆盖

**Non-Goals:**
- 第三方支付网关集成（支付宝/微信支付），本期模拟支付状态
- 实时消息推送（WebSocket/SSE）
- 多语言国际化
- 移动端原生 App

## Decisions

### 1. 前端框架：Vue 3 + Vite + Pinia
**选择**: Vue 3 Composition API，Vite 构建，Pinia 状态管理，Element Plus UI。  
**理由**: Vue 3 响应式系统更完善，Vite 开发构建速度快，Pinia 比 Vuex 更轻量且类型友好。  
**备选**: React + Redux — 生态更大，但团队熟悉 Vue，切换成本高。

### 2. 后端框架：Spring Boot 3 + Spring Security + JPA
**选择**: Spring Boot 3，Spring Security 配合 JWT，Spring Data JPA + MySQL。  
**理由**: Spring Boot 生态成熟，JPA 简化 CRUD，Spring Security 提供完整的认证授权框架。  
**备选**: Node.js/Express — 轻量但缺乏企业级特性，与团队 Java 技术栈不符。

### 3. 认证方案：JWT（无状态）
**选择**: 登录后颁发 Access Token（15分钟）+ Refresh Token（7天），前端存储于 localStorage。  
**理由**: 无状态设计，后端不需维护 Session，水平扩展友好。  
**备选**: Session + Redis — 需要额外 Redis 基础设施，单点会话风险。  
**风险**: localStorage 存在 XSS 风险 → 前端做 CSP 配置和输入过滤缓解。

### 4. 数据库：MySQL 8 + 单数据库
**选择**: 单一 MySQL 数据库，核心表：users、products、categories、cart_items、orders、order_items。  
**理由**: 数据关系紧密，单库事务简单，初期流量不需要分库分表。  
**备选**: PostgreSQL — 功能更强，但 MySQL 更普及，运维成本低。

### 5. API 设计：RESTful v1
**选择**: `/api/v1/` 前缀，标准 HTTP 动词，统一响应格式 `{code, message, data}`。  
**理由**: 规范清晰，前后端契约明确，便于后续版本迭代。

## Risks / Trade-offs

- **无真实支付** → 订单状态由后端接口手动驱动，演示用模拟支付；上线前需接入支付网关
- **JWT 无法主动吊销** → 登出仅清除前端 Token，短 Access Token 生命周期（15分钟）降低风险
- **单体架构扩展性** → 初期单体 Spring Boot，业务增长后可按模块拆分微服务
- **库存并发超卖** → 下单时使用数据库乐观锁（version字段）控制库存扣减

## Migration Plan

1. 初始化 MySQL 数据库，执行 DDL 脚本创建表结构
2. 部署 Spring Boot 后端（默认端口 8080）
3. 配置前端 `.env` 指向后端 API 地址
4. 构建并部署 Vue 前端静态文件（Nginx）
5. 回滚：前后端均为无状态部署，直接回滚镜像/包即可

## Open Questions

- 商品图片存储方案：本地文件系统 vs 对象存储（OSS/S3）？当前设计存本地，生产需评估
- 订单支付超时自动取消：是否需要定时任务？初期可手动处理
