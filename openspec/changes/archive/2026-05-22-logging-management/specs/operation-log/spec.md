## ADDED Requirements

### Requirement: 用户关键操作被记录到数据库
系统 SHALL 在用户执行关键操作时，自动将操作信息（操作人、模块、动作、关联资源ID、IP、时间）异步写入 `operation_logs` 表，不阻塞主业务流程。

#### Scenario: 操作日志自动记录
- **WHEN** 带有 `@OperationLog` 注解的 Service 方法被调用
- **THEN** 系统从 SecurityContext 获取当前用户信息，从 HttpServletRequest 获取客户端 IP，方法执行完成后异步写入一条操作日志记录

#### Scenario: 覆盖登录操作
- **WHEN** 用户成功登录
- **THEN** 系统记录一条模块为"用户"、动作为"登录"的操作日志，包含用户ID、用户名、IP

#### Scenario: 覆盖注册操作
- **WHEN** 用户成功注册
- **THEN** 系统记录一条模块为"用户"、动作为"注册"的操作日志，包含用户ID、用户名、IP

#### Scenario: 覆盖下单操作
- **WHEN** 买家创建订单成功
- **THEN** 系统记录一条模块为"订单"、动作为"创建订单"的操作日志，resource_id 为新订单ID

#### Scenario: 覆盖支付回调
- **WHEN** 支付宝回调通知处理成功，订单状态更新为 PAID
- **THEN** 系统记录一条模块为"支付"、动作为"支付成功"的操作日志，resource_id 为订单ID

#### Scenario: 覆盖退款申请
- **WHEN** 买家提交退款申请
- **THEN** 系统记录一条模块为"退款"、动作为"申请退款"的操作日志，resource_id 为订单ID

#### Scenario: 覆盖退款审批
- **WHEN** 管理员批准或拒绝退款
- **THEN** 系统记录一条模块为"退款"、动作为"批准退款"或"拒绝退款"的操作日志，resource_id 为订单ID

#### Scenario: 覆盖管理员封禁用户
- **WHEN** 管理员封禁或解封用户
- **THEN** 系统记录一条模块为"管理"、动作为"封禁用户"或"解封用户"的操作日志，resource_id 为被操作用户ID

#### Scenario: 日志写入失败不影响主链路
- **WHEN** 异步日志写入数据库失败
- **THEN** 系统仅打印 WARN 级别日志，主业务请求不受影响，不回滚

### Requirement: 管理员可分页查询操作日志
系统 SHALL 提供接口供管理员按模块、用户名、时间段分页检索操作日志。

#### Scenario: 查询全量操作日志
- **WHEN** 管理员访问 GET /api/v1/admin/logs（无筛选条件）
- **THEN** 系统返回按创建时间倒序排列的分页操作日志列表

#### Scenario: 按模块筛选
- **WHEN** 管理员传入 module 参数
- **THEN** 系统只返回该模块的操作日志

#### Scenario: 按用户名筛选
- **WHEN** 管理员传入 username 参数
- **THEN** 系统只返回该用户名的操作日志

#### Scenario: 按时间段筛选
- **WHEN** 管理员传入 startTime 和 endTime 参数
- **THEN** 系统只返回该时间段内的操作日志
