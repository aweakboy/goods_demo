## 1. 后端数据模型与数据库

- [x] 1.1 新增 `PriceAlertStatus` 枚举，包含 `ACTIVE`、`TRIGGERED`、`CANCELLED`
- [x] 1.2 新增 `BuyerNotificationType` 枚举，至少包含 `PRICE_DROP`
- [x] 1.3 新增 `ShopFavorite` 实体，包含买家 ID、店铺 ID、创建时间和买家店铺唯一约束
- [x] 1.4 新增 `PriceAlert` 实体，包含买家 ID、商品 ID、目标价、状态、触发价、触发时间和更新时间
- [x] 1.5 新增 `BuyerNotification` 实体，包含买家 ID、通知类型、标题、内容、关联商品/店铺、已读时间和创建时间
- [x] 1.6 新增 `ShopFavoriteRepository`，支持按买家/店铺查询、收藏计数和分页列表
- [x] 1.7 新增 `PriceAlertRepository`，支持买家商品唯一查询、有效提醒查询和买家分页列表
- [x] 1.8 新增 `BuyerNotificationRepository`，支持买家通知分页、未读计数和批量已读
- [x] 1.9 更新 `schema.sql`，新增三张表、唯一约束、外键或等价索引
- [x] 1.10 为收藏、提醒和通知响应新增 DTO，避免直接暴露实体

## 2. 店铺收藏后端能力

- [x] 2.1 新增 `ShopFavoriteService.favoriteShop()`，校验买家角色、店铺存在且 ACTIVE
- [x] 2.2 实现收藏店铺幂等逻辑，重复收藏不创建重复记录
- [x] 2.3 新增 `ShopFavoriteService.unfavoriteShop()`，重复取消返回未收藏状态且不影响其他买家
- [x] 2.4 新增 `ShopFavoriteService.listFavorites()`，分页返回当前买家的收藏店铺
- [x] 2.5 扩展店铺主页响应 DTO，包含 `favorited` 和 `favoriteCount`
- [x] 2.6 扩展店铺主页查询逻辑，登录买家返回真实收藏状态，访客返回未收藏状态
- [x] 2.7 新增买家店铺收藏 controller，覆盖收藏、取消收藏和我的收藏列表接口
- [x] 2.8 确保 SELLER、ADMIN 和未登录用户访问买家收藏接口返回 401 或 403

## 3. 降价提醒与站内通知后端能力

- [x] 3.1 新增 `PriceAlertRequest`，校验目标价不能为空且大于 0
- [x] 3.2 新增 `PriceAlertService.createOrUpdateAlert()`，校验商品存在、ACTIVE 且目标价低于当前价
- [x] 3.3 实现同一买家同一商品提醒唯一逻辑，再次设置时更新原提醒并重置为 `ACTIVE`
- [x] 3.4 新增 `PriceAlertService.cancelAlert()`，只允许买家取消自己的提醒
- [x] 3.5 新增 `PriceAlertService.listAlerts()`，分页返回当前买家的提醒列表及商品当前价格
- [x] 3.6 新增 `PriceAlertService.processPriceChange()`，仅在价格下降且达到目标价时触发有效提醒
- [x] 3.7 触发提醒时创建 `PRICE_DROP` 站内通知，并将提醒标记为 `TRIGGERED`
- [x] 3.8 确保 `TRIGGERED` 和 `CANCELLED` 提醒不会重复生成通知
- [x] 3.9 新增 `BuyerNotificationService.listNotifications()`，分页返回当前买家通知
- [x] 3.10 新增 `BuyerNotificationService.countUnread()`，返回当前买家未读通知数
- [x] 3.11 新增 `BuyerNotificationService.markRead()` 和 `markAllRead()`，只修改当前买家的通知
- [x] 3.12 新增买家降价提醒 controller，覆盖设置/更新、取消和列表接口
- [x] 3.13 新增买家通知 controller，覆盖通知列表、未读数、单条已读和全部已读接口

## 4. 商品价格更新链路集成

- [x] 4.1 在卖家商品编辑逻辑中记录旧价格和新价格
- [x] 4.2 卖家成功更新商品价格后，价格下降时调用 `PriceAlertService.processPriceChange()`
- [x] 4.3 卖家更新商品但价格不变或上涨时，不调用降价提醒触发逻辑
- [x] 4.4 如管理员已有商品价格修改入口，同步接入降价提醒触发逻辑
- [x] 4.5 确保商品更新失败、权限失败或校验失败时不触发任何降价通知

## 5. 前端店铺收藏与降价通知

- [x] 5.1 新增店铺收藏 API wrapper，覆盖收藏、取消收藏和我的收藏列表
- [x] 5.2 新增降价提醒 API wrapper，覆盖设置/更新、取消和我的提醒列表
- [x] 5.3 新增买家通知 API wrapper，覆盖通知列表、未读数、单条已读和全部已读
- [x] 5.4 在店铺主页展示收藏按钮、收藏数量和当前买家收藏状态
- [x] 5.5 在店铺主页实现收藏/取消收藏交互，并按服务端返回状态更新 UI
- [x] 5.6 新增买家收藏店铺页面，展示收藏店铺列表和不可访问店铺状态
- [x] 5.7 在商品详情页新增目标价提醒入口，支持创建或更新目标价
- [x] 5.8 在商品详情页展示当前买家对该商品的提醒状态或目标价摘要
- [x] 5.9 新增买家降价提醒管理页面，支持查看和取消提醒
- [x] 5.10 新增买家通知中心页面，展示通知列表、未读状态和关联商品/店铺入口
- [x] 5.11 在买家导航中加入收藏店铺和通知中心入口，并展示未读通知数量
- [x] 5.12 前端错误提示展示服务端返回原因，例如目标价无效、店铺已关闭、商品已下架

## 6. 后端自动化测试

- [x] 6.1 添加 `ShopFavoriteServiceTest`：收藏有效店铺成功
- [x] 6.2 添加 `ShopFavoriteServiceTest`：重复收藏不创建重复记录
- [x] 6.3 添加 `ShopFavoriteServiceTest`：重复取消收藏保持幂等
- [x] 6.4 添加 `ShopFavoriteServiceTest`：收藏不存在或 INACTIVE 店铺返回错误
- [x] 6.5 添加 `ShopFavoriteServiceTest`：收藏列表只返回当前买家的记录
- [x] 6.6 添加店铺收藏 controller/API 测试：非买家访问返回 403
- [x] 6.7 添加 `PriceAlertServiceTest`：有效目标价创建 ACTIVE 提醒
- [x] 6.8 添加 `PriceAlertServiceTest`：同一商品再次设置会更新原提醒
- [x] 6.9 添加 `PriceAlertServiceTest`：目标价大于等于当前价格返回 400
- [x] 6.10 添加 `PriceAlertServiceTest`：价格下降达到目标价生成通知并标记 TRIGGERED
- [x] 6.11 添加 `PriceAlertServiceTest`：价格下降但未达到目标价不生成通知
- [x] 6.12 添加 `PriceAlertServiceTest`：已触发或已取消提醒不重复生成通知
- [x] 6.13 添加 `BuyerNotificationServiceTest`：未读数量、单条已读和全部已读状态正确
- [x] 6.14 添加降价提醒和通知 controller/API 测试：非买家访问返回 403
- [x] 6.15 添加商品更新 service 测试：价格下降后调用降价提醒处理
- [x] 6.16 添加商品更新 service 测试：价格未下降或更新失败时不触发通知

## 7. 验证

- [x] 7.1 运行 `mvn.cmd test`
- [x] 7.2 运行 `npm.cmd run build`
- [x] 7.3 运行 `openspec validate add-shop-favorites-price-alerts`
- [ ] 7.4 手动验证：买家在店铺主页收藏和取消收藏后，按钮状态和收藏数量正确更新
- [ ] 7.5 手动验证：买家收藏店铺列表只展示自己的收藏，关闭店铺显示不可访问状态
- [ ] 7.6 手动验证：买家在商品详情页设置目标价后，可在降价提醒管理页看到该提醒
- [ ] 7.7 手动验证：卖家把商品价格降到目标价后，买家通知中心出现未读降价通知
- [ ] 7.8 手动验证：通知单条已读和全部已读后，未读数量正确变化
