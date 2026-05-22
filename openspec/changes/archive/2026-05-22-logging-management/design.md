## Context

当前后端使用 SLF4J + Spring Boot 默认日志配置（控制台输出）。没有文件日志，没有业务操作记录表。Spring Boot 3.2 内置 Logback，只需提供 `logback-spring.xml` 即可覆盖默认配置，无需额外依赖。

## Goals / Non-Goals

**Goals:**
- 后端日志持久化到文件，支持按天滚动和自动清理
- 用户关键操作被记录到数据库，管理员可查询
- 操作日志采集对业务代码侵入最小

**Non-Goals:**
- 不集成 ELK/Loki 等日志平台
- 不记录读操作（查询列表、查看详情）
- 不做实时告警
- 不记录前端埋点

## Decisions

### D1：用 AOP + 自定义注解采集操作日志，不在业务代码中手动调用

```java
@OperationLog(module = "订单", action = "创建订单")
public Order createOrder(...) { ... }
```

切面拦截，从 SecurityContext 获取当前用户，从请求上下文获取 IP，方法执行后写入日志。
优点：业务代码零侵入，统一管理。

### D2：操作日志异步写入，不阻塞主业务流程

使用 `@Async` + Spring 线程池异步持久化日志。若日志写入失败只打印 warn，不影响主链路。需在主类启用 `@EnableAsync`。

### D3：操作日志表结构

```sql
operation_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT,           -- 可为空（匿名操作）
  username   VARCHAR(50),
  module     VARCHAR(50),      -- 模块：用户、订单、支付、退款、管理
  action     VARCHAR(100),     -- 具体动作：创建订单、申请退款...
  resource_id VARCHAR(50),     -- 关联资源 ID（订单ID、用户ID等）
  detail     VARCHAR(500),     -- 补充说明
  ip         VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
```

### D4：系统日志使用 Logback 滚动策略

```
logs/
  trading.log          ← 当前日志（追加写）
  trading.2025-01-01.log  ← 归档日志
  error.log            ← 仅 ERROR，独立文件
```

使用 `TimeBasedRollingPolicy`，`fileNamePattern` 含日期，`maxHistory=30`。

### D5：IP 从 HttpServletRequest 获取，支持代理场景

优先读 `X-Forwarded-For` 头，再读 `getRemoteAddr()`。

## Risks / Trade-offs

- **AOP 无法捕获 this 调用** → 切面只拦截 Spring 代理调用，内部 this.method() 不触发。当前代码架构（Controller→Service）均通过代理调用，无影响。
- **异步日志丢失** → 极端情况下（JVM crash）最后几条日志可能未写入。可接受，操作日志非强一致性需求。
- **日志文件磁盘占用** → 单日日志量估计 <10MB，30天保留约 300MB，正常开发机可接受。
