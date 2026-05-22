## 1. 数据库与实体

- [x] 1.1 在 `schema.sql` 中新增 `operation_logs` 表（id、user_id、username、module、action、resource_id、detail、ip、created_at）
- [x] 1.2 新增 `OperationLog` JPA 实体，映射 `operation_logs` 表
- [x] 1.3 新增 `OperationLogRepository` 接口，继承 `JpaRepository`，支持按 module/username/时间段的动态查询

## 2. AOP 操作日志采集

- [x] 2.1 新增 `@OperationLog` 自定义注解（属性：module、action）
- [x] 2.2 新增 `OperationLogAspect` 切面，拦截带注解的 Service 方法，方法执行后从 SecurityContext 获取用户、从 RequestContextHolder 获取 IP，异步调用日志服务写入
- [x] 2.3 新增 `OperationLogService`，提供 `saveAsync(...)` 方法（`@Async`），写入失败仅 warn 不抛异常
- [x] 2.4 在 `TradingApplication` 主类上添加 `@EnableAsync`

## 3. 业务方法标注

- [x] 3.1 在 `AuthService.login()` 方法上添加 `@OperationLog(module="用户", action="登录")`
- [x] 3.2 在 `AuthService.register()` 方法上添加 `@OperationLog(module="用户", action="注册")`
- [x] 3.3 在 `OrderService.createOrder()` 方法上添加 `@OperationLog(module="订单", action="创建订单")`
- [x] 3.4 在 `PaymentService.handleNotify()` 支付成功分支添加 `@OperationLog(module="支付", action="支付成功")`
- [x] 3.5 在 `OrderService.requestRefund()` 方法上添加 `@OperationLog(module="退款", action="申请退款")`
- [x] 3.6 在 `OrderService.approveRefund()` 方法上添加 `@OperationLog(module="退款", action="批准退款")`
- [x] 3.7 在 `OrderService.rejectRefund()` 方法上添加 `@OperationLog(module="退款", action="拒绝退款")`
- [x] 3.8 在 `AdminService`（或 AdminController 对应 Service 方法）封禁/解封用户处添加 `@OperationLog(module="管理", action="修改用户状态")`

## 4. 管理员后端接口

- [x] 4.1 在 `AdminController` 中新增 `GET /api/v1/admin/logs` 接口，支持 module、username、startTime、endTime 查询参数，返回分页操作日志列表（按 created_at 倒序）

## 5. 系统日志（Logback）

- [x] 5.1 新增 `src/main/resources/logback-spring.xml`，配置：全量滚动文件 `logs/trading.log`（按天，保留30天）和 ERROR 独立文件 `logs/error.log`（保留60天）
- [x] 5.2 在 `application.yml` `logging.level` 下补充：`org.hibernate.SQL: WARN`（抑制 SQL 日志到控制台）

## 6. 前端——管理员操作日志页

- [x] 6.1 在 `src/api/admin.js` 中新增 `getLogs(params)` 方法，调用 `GET /api/v1/admin/logs`
- [x] 6.2 新增 `src/views/admin/AdminLogs.vue`，展示操作日志列表（模块、动作、用户名、资源ID、IP、时间），支持按模块/用户名/时间段筛选
- [x] 6.3 在 `router/index.js` 中注册 `/admin/logs` 子路由，对应 `AdminLogs.vue`
- [x] 6.4 在 `AdminLayout.vue` 导航菜单中添加"操作日志"入口
