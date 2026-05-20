## 1. 项目初始化

- [x] 1.1 创建 Spring Boot 3 项目，添加依赖：spring-boot-starter-web、spring-boot-starter-security、spring-boot-starter-data-jpa、mysql-connector-j、jjwt、lombok
- [x] 1.2 创建 Vue 3 + Vite 项目，安装依赖：vue-router、pinia、element-plus、axios
- [x] 1.3 创建 MySQL 数据库，执行 DDL 脚本建表：users、categories、products、cart_items、orders、order_items
- [x] 1.4 配置 Spring Boot application.yml：数据库连接、JPA、JWT 密钥和过期时间
- [x] 1.5 配置 Vue 前端 .env 文件，设置 VITE_API_BASE_URL

## 2. 后端 - 用户认证模块

- [x] 2.1 创建 User 实体类，字段：id、username、email、password、role（BUYER/SELLER）、createdAt
- [x] 2.2 实现 UserRepository（JPA）
- [x] 2.3 实现 JWT 工具类：生成 Access Token、Refresh Token，验证 Token
- [x] 2.4 实现 Spring Security 配置：密码加密（BCrypt）、无状态 Session、白名单路径
- [x] 2.5 实现 JWT 过滤器（OncePerRequestFilter），从请求头提取并校验 Token
- [x] 2.6 实现 AuthController：POST /api/v1/auth/register、POST /api/v1/auth/login、POST /api/v1/auth/refresh
- [x] 2.7 统一响应格式封装 ApiResponse<T>（code、message、data）

## 3. 后端 - 商品目录模块

- [x] 3.1 创建 Category 实体，字段：id、name、status
- [x] 3.2 创建 Product 实体，字段：id、name、description、price、stock、status（ACTIVE/INACTIVE）、sellerId、categoryId、createdAt
- [x] 3.3 实现 ProductRepository，支持分页查询、按分类过滤、关键词模糊搜索
- [x] 3.4 实现 ProductController（公开接口）：GET /api/v1/products（分页+筛选+搜索）、GET /api/v1/products/{id}、GET /api/v1/categories

## 4. 后端 - 购物车模块

- [x] 4.1 创建 CartItem 实体，字段：id、buyerId、productId、quantity
- [x] 4.2 实现 CartItemRepository
- [x] 4.3 实现 CartController（需 BUYER 角色）：POST /api/v1/cart/items、GET /api/v1/cart/items、PUT /api/v1/cart/items/{id}、DELETE /api/v1/cart/items/{id}、DELETE /api/v1/cart
- [x] 4.4 购物车 Service 层：库存校验逻辑（添加/更新时检查 product.stock）

## 5. 后端 - 订单模块

- [x] 5.1 创建 Order 实体，字段：id、buyerId、status（枚举）、totalAmount、address、createdAt
- [x] 5.2 创建 OrderItem 实体，字段：id、orderId、productId、productName、price、quantity
- [x] 5.3 实现 OrderRepository、OrderItemRepository
- [x] 5.4 实现下单 Service：开启事务、校验库存、用乐观锁扣减 product.stock（version 字段）、创建 Order + OrderItems、清空购物车已购条目
- [x] 5.5 实现 OrderController（买家接口）：POST /api/v1/orders、GET /api/v1/orders（支持 status 筛选）、GET /api/v1/orders/{id}、POST /api/v1/orders/{id}/pay、POST /api/v1/orders/{id}/confirm、POST /api/v1/orders/{id}/cancel
- [x] 5.6 实现卖家订单接口：GET /api/v1/seller/orders（支持 status 筛选）、POST /api/v1/seller/orders/{id}/ship

## 6. 后端 - 卖家管理模块

- [x] 6.1 实现 SellerProductController（需 SELLER 角色）：POST /api/v1/seller/products、PUT /api/v1/seller/products/{id}、GET /api/v1/seller/products
- [x] 6.2 商品 Service：校验 sellerId 一致性（防止越权修改），价格/库存非负校验
- [x] 6.3 商品下架逻辑：status 设为 INACTIVE，买家端查询自动过滤 INACTIVE 商品

## 7. 前端 - 项目结构与路由

- [x] 7.1 配置 Vue Router：定义路由（首页、商品列表、商品详情、购物车、订单列表、订单详情、登录/注册、卖家后台）
- [x] 7.2 实现路由守卫：未登录跳转登录页；卖家路由验证 SELLER 角色
- [x] 7.3 创建 Pinia store：userStore（Token 持久化到 localStorage、用户信息）
- [x] 7.4 封装 axios 实例：统一 baseURL、请求头注入 Authorization Token、响应拦截处理 401（清除 Token 跳转登录）

## 8. 前端 - 用户认证页面

- [x] 8.1 实现登录页（LoginView.vue）：邮箱/密码表单，调用登录 API，存储 Token
- [x] 8.2 实现注册页（RegisterView.vue）：用户名/邮箱/密码/角色选择表单，调用注册 API
- [x] 8.3 实现顶部导航栏（AppHeader.vue）：登录状态展示用户名和登出按钮，未登录展示登录/注册链接

## 9. 前端 - 商品展示页面

- [x] 9.1 实现商品列表页（ProductListView.vue）：分类筛选栏 + 搜索框 + 商品卡片网格 + 分页组件
- [x] 9.2 实现商品卡片组件（ProductCard.vue）：封面图、名称、价格、库存状态
- [x] 9.3 实现商品详情页（ProductDetailView.vue）：图片展示、完整描述、价格、库存、加入购物车按钮（含数量选择）

## 10. 前端 - 购物车页面

- [x] 10.1 实现购物车页面（CartView.vue）：商品列表、数量增减、删除、清空、总价计算、去结算按钮
- [x] 10.2 调用购物车 API 实现增删改查，更新 pinia 购物车状态

## 11. 前端 - 订单页面

- [x] 11.1 实现结算/下单页面（CheckoutView.vue）：显示选中商品、填写收货地址、确认下单
- [x] 11.2 实现订单列表页（OrderListView.vue）：状态筛选 Tab + 订单卡片列表
- [x] 11.3 实现订单详情页（OrderDetailView.vue）：商品明细、订单状态、操作按钮（支付/确认收货/取消）
- [x] 11.4 模拟支付弹窗：点击支付后弹出确认框，调用支付接口

## 12. 前端 - 卖家后台

- [x] 12.1 实现卖家商品管理页（SellerProductsView.vue）：商品列表含低库存标识、上架/下架切换、编辑入口
- [x] 12.2 实现商品编辑/新增表单（ProductFormView.vue）：名称、描述、价格、库存、分类、状态字段
- [x] 12.3 实现卖家订单管理页（SellerOrdersView.vue）：订单列表含状态筛选、发货操作（填写物流信息）

## 13. 测试与验收

- [x] 13.1 后端单元测试：AuthService、OrderService（下单逻辑、库存扣减）
- [x] 13.2 后端接口测试：使用 Postman/MockMvc 验证各模块主流程
- [x] 13.3 前端 E2E 验证：注册→登录→浏览商品→加购物车→下单→模拟支付→卖家发货→买家确认收货完整流程
- [x] 13.4 验证权限控制：买家无法访问卖家接口，未登录无法访问受保护页面
