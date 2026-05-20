# Goods Demo — 项目说明

## 项目结构

```
myproject/
├── backend/      # Spring Boot 3.2 后端
├── frontend/     # Vue 3 + Vite 前端
└── openspec/     # 需求/设计文档
```

## 技术栈

- **后端**：Spring Boot 3.2、Spring Security (JWT)、JPA/Hibernate、MySQL 8
- **前端**：Vue 3 Composition API、Element Plus、Pinia、Vue Router、Vite

---

## Git 分支管理

### 分支结构

| 分支 | 用途 |
|------|------|
| `main` | 稳定主干，只接受来自开发分支的 PR 合并 |
| `dev/frontend` | 前端开发分支 |
| `dev/backend` | 后端开发分支 |

### 远程仓库

```
https://github.com/aweakboy/goods_demo.git
```

### 常用操作

**切换到前端分支开发**
```bash
git checkout dev/frontend
# ... 修改代码 ...
git add <files>
git commit -m "feat: 描述改动"
git push
```

**切换到后端分支开发**
```bash
git checkout dev/backend
# ... 修改代码 ...
git add <files>
git commit -m "feat: 描述改动"
git push
```

**将开发分支合并到 main**
```bash
# 方式一：在 GitHub 上创建 Pull Request（推荐）
# 方式二：本地合并
git checkout main
git merge dev/frontend   # 或 dev/backend
git push origin main
```

**同步 main 最新代码到开发分支**
```bash
git checkout dev/frontend
git merge main
git push
```

**查看所有分支状态**
```bash
git branch -a
git log --oneline --graph --all
```

---

## 启动方式

### 后端
```bash
cd backend
mvn spring-boot:run
# 默认端口 8080，需要 MySQL 已启动并执行 schema.sql
```

### 前端
```bash
cd frontend
npm install
npm run dev
# 默认端口 5173，代理后端 /api → http://localhost:8080
```

---

## 数据库

使用 `backend/src/main/resources/schema.sql` 初始化表结构。
连接配置在 `backend/src/main/resources/application.yml`。

如已有旧数据库，需手动执行：
```sql
ALTER TABLE order_items ADD COLUMN shop_name VARCHAR(100);
ALTER TABLE users MODIFY COLUMN role ENUM('BUYER','SELLER','ADMIN') NOT NULL;
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
```
