# Trading Platform

## 快速启动

### 数据库初始化

1. 确保 MySQL 8 已启动
2. 执行 `backend/src/main/resources/schema.sql` 完成建表和初始数据导入

### 管理员账户

初始管理员账户由 `schema.sql` 的 seed 脚本自动创建：

| 字段 | 值 |
|------|-----|
| 邮箱 | admin@trading.com |
| 密码 | Admin@123456 |
| 角色 | ADMIN |

> **⚠️ 安全提示**：首次部署后请立即修改默认密码。目前系统无修改密码接口，可直接通过 MySQL 更新：
> ```sql
> -- 使用 BCrypt 工具生成新密码 hash 后执行：
> UPDATE users SET password = '<新密码BCrypt hash>' WHERE email = 'admin@trading.com';
> ```

### 后端启动

```bash
cd backend
./mvnw spring-boot:run
```

API 默认运行在 `http://localhost:8080`

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

## 角色说明

| 角色 | 访问路径 | 说明 |
|------|----------|------|
| BUYER | `/products`, `/cart`, `/orders` | 买家，注册时选择 |
| SELLER | `/seller/*` | 卖家，注册时选择 |
| ADMIN | `/admin/*` | 管理员，仅可通过数据库创建 |
