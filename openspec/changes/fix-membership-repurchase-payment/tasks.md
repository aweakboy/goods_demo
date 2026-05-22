## 1. 后端会员购买恢复能力

- [x] 1.1 在 `MembershipPurchaseRepository` 增加按买家、套餐和状态查询最新待支付购买记录的方法
- [x] 1.2 更新 `MembershipService.createPurchase()`，同买家同套餐已有 `PENDING_PAYMENT` 记录时复用该记录
- [x] 1.3 确保复用的待支付记录始终具备可用的 `outTradeNo`
- [x] 1.4 增加会员购买继续支付服务方法，校验购买记录存在、归属当前买家且状态为 `PENDING_PAYMENT`
- [x] 1.5 增加买家会员购买记录继续支付 API，返回该购买记录对应的支付表单
- [x] 1.6 对会员购买相关的数据冲突返回明确 400 响应，避免展示“服务器内部错误”

## 2. 前端会员中心体验

- [x] 2.1 在会员购买记录表格增加操作列
- [x] 2.2 仅对 `PENDING_PAYMENT` 购买记录展示“继续支付”按钮
- [x] 2.3 点击“继续支付”时调用继续支付 API 并打开支付页面
- [x] 2.4 重复点击同一套餐购买时仍应打开支付页面，并刷新购买记录
- [x] 2.5 继续支付失败时展示后端业务错误，不清空购买记录

## 3. 后端自动化测试

- [x] 3.1 添加 `MembershipServiceTest`：重复购买同一套餐时复用已有待支付记录
- [x] 3.2 添加 `MembershipServiceTest`：待支付记录缺少 `outTradeNo` 时修复后复用
- [x] 3.3 添加 `MembershipServiceTest`：继续支付自己的待支付记录成功
- [x] 3.4 添加 `MembershipServiceTest`：继续支付非待支付记录返回 400
- [x] 3.5 添加 controller/API 测试：继续支付他人购买记录返回 403
- [x] 3.6 添加 controller/API 测试：继续支付待支付记录返回支付表单

## 4. 验证

- [x] 4.1 运行 `mvn.cmd test`
- [x] 4.2 运行 `npm.cmd run build`
- [x] 4.3 运行 `openspec validate fix-membership-repurchase-payment`
- [ ] 4.4 手动验证：同一套餐重复点击购买会弹出支付页面且不新增多条待支付记录
- [ ] 4.5 手动验证：购买记录中的待支付记录点击“继续支付”会弹出支付页面
- [ ] 4.6 手动验证：已支付购买记录不展示继续支付入口
