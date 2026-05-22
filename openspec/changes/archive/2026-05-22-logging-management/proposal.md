## Why

系统目前缺乏两类日志能力：一是用户关键行为无记录，出现纠纷或异常时无法追溯；二是后端运行日志仅输出到控制台，服务重启后日志丢失，排查问题困难。本次同时引入业务操作日志和系统日志文件管理。

## What Changes

### 操作日志（业务行为记录）
- **新增**：`operation_logs` 数据库表，记录用户关键操作（模块、动作、资源ID、IP、时间）
- **新增**：自定义注解 `@OperationLog`，标注需要记录的 Service 方法
- **新增**：AOP 切面 `OperationLogAspect`，拦截带注解方法，异步写入日志
- **新增**：管理员接口 `GET /api/v1/admin/logs`，支持按模块/用户名/时间段分页查询
- **新增**：管理员后台"操作日志"页，可筛选和浏览全量操作记录
- **覆盖的关键操作**：用户登录/注册、下单、支付回调成功、退款申请、退款审批、管理员封禁用户

### 系统日志（后端运行日志）
- **新增**：`logback-spring.xml`，配置滚动文件输出：
  - `logs/trading.log`：全量日志，按天滚动，保留 30 天
  - `logs/error.log`：仅 ERROR 级别，保留 60 天
- **修改**：`application.yml` 日志级别细化（com.trading=INFO，SQL=WARN）

## Capabilities

### New Capabilities

- `operation-log`: 操作日志记录与查询——AOP 采集、数据库存储、管理员检索

### Modified Capabilities

- `admin-order-oversight`: 管理员后台新增操作日志入口

## Impact

- **后端**：新增 `OperationLog` 实体、`@OperationLog` 注解、`OperationLogAspect`、`OperationLogService`、`OperationLogRepository`；修改 `AdminController`、`AuthService`、`OrderService`
- **前端**：新增 `AdminLogs.vue`；修改 `AdminLayout.vue`、`router/index.js`、`admin.js`
- **数据库**：新增 `operation_logs` 表（schema.sql 补充）
- **配置**：新增 `logback-spring.xml`，微调 `application.yml`
