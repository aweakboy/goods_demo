## Context

当前平台已经有卖家店铺、购物车、订单、支付和发货流程，但地址信息仍是弱结构：

- `Shop` 只包含名称、简介、状态和创建时间，没有店铺发货地址。
- `Order` 只保存 `address` 自由文本，无法可靠用于地图展示、距离估算或后续物流模拟。
- 买家结算页使用单个 textarea 输入收货地址；卖家店铺页没有地址输入。
- 后续“发货模拟、地图、路线、物流轨迹”都需要稳定的起点和终点地址数据。

这个 change 建立地址数据基础，但不接入真实地图服务。

## Goals / Non-Goals

**Goals:**

- 店铺注册和编辑时保存结构化店铺地址。
- 创建订单时保存结构化收货地址快照。
- 前端表单收集省、市、区、详细地址等字段，并展示完整地址。
- 数据库保留完整地址文本，方便当前页面继续展示。
- 为后续地图/物流能力预留经纬度和地址校验状态字段。
- 按项目规则补充后端测试任务和明确验证命令。

**Non-Goals:**

- 不接入高德/腾讯/百度地图 SDK。
- 不做地址搜索、行政区联动数据源、地理编码或路线规划。
- 不做用户常用地址簿。
- 不拆分多卖家订单或新增物流包裹模型。
- 不改变支付、退款和订单状态机。

## Decisions

### Decision 1: 地址字段先内嵌到 `Shop` 和 `Order`

本期直接在 `Shop` 和 `Order` 上新增地址字段，而不是抽象独立 `Address` 表。

字段建议：

- `province`
- `city`
- `district`
- `detailAddress`
- `fullAddress`
- `longitude`
- `latitude`
- `addressValidationStatus`

对 `Order` 使用收货地址快照字段命名，例如：

- `receiverName`
- `receiverPhone`
- `receiverProvince`
- `receiverCity`
- `receiverDistrict`
- `receiverDetailAddress`
- `receiverFullAddress`
- `receiverLongitude`
- `receiverLatitude`
- `receiverAddressValidationStatus`

理由：

- 店铺当前一卖家一店，地址生命周期与店铺一致，独立地址表暂时过重。
- 订单必须保存快照，不能引用后续可能变化的店铺或用户地址。
- 字段直接落表更容易兼容当前 JPA 和前端表单。

备选方案是新增通用 `addresses` 表，但当前没有地址簿、多地址选择、地址复用等需求，复杂度暂不值得。

### Decision 2: 本期只做结构校验，不做真实地址校验

`addressValidationStatus` 本期使用 `UNVERIFIED` 作为默认值。前端和后端只校验必填字段、长度和基础格式；真实地理编码和经纬度校验留给后续地图 change。

理由：

- 结构化地址是后续地图接入的前置条件。
- 现在接入第三方地图会引入 key 管理、网络失败、配额、隐私和本地开发问题。
- 先让业务模型稳定，再接外部服务，风险更低。

### Decision 3: 保留兼容展示字段

订单保留现有 `address` 字段语义作为 `receiverFullAddress` 的兼容展示来源，或者在实现中继续填充 `address = receiverFullAddress`。店铺响应同样返回 `fullAddress`，前端优先展示完整地址。

理由：

- 当前多个页面直接展示 `order.address`。
- 分阶段迁移可以减少一次性修改范围。
- 后续归档主规格时可以再决定是否废弃旧字段名。

### Decision 4: API 请求体升级为结构化对象

`ShopRequest` 和 `OrderRequest` 新增结构化地址字段。前端提交结构化字段；后端组装 `fullAddress` 并写入实体。

为了兼容开发过程中的旧调用，后端可以在短期内接受旧 `address` 文本，但新的前端流程必须提交结构化字段。实现时应优先使用结构化字段，旧字段仅作为过渡。

### Decision 5: 测试覆盖服务层规则

本 change 涉及数据模型、表单和订单创建规则。自动化测试重点放在后端 service：

- 店铺注册缺少地址字段应失败。
- 店铺注册成功应保存完整结构化地址。
- 店铺编辑应更新地址字段。
- 创建订单缺少收货地址字段应失败。
- 创建订单成功应保存收货地址快照。

前端当前没有测试基线，因此前端以 `npm.cmd run build` 和明确手动验收为完成条件。

## Risks / Trade-offs

- **已有数据库缺少新增列** → 在 `schema.sql` 中补充建表字段，并添加面向已有库的迁移 SQL 注释；本地已有数据库需要手动执行 ALTER。
- **旧页面仍读取 `order.address`** → 实现时继续填充兼容字段，并逐步把页面迁移到 `receiverFullAddress`。
- **没有真实地址校验可能录入错误地址** → 本期只保证结构完整；后续地图 change 负责地址搜索和地理编码。
- **经纬度为空会影响后续地图** → 字段允许为空，后续接入地图服务后再设置为必填或自动填充。
- **多卖家订单仍只有订单级收货地址** → 本期只处理收货地址结构化；发货起点和多包裹模型留给物流模拟 change。

## Migration Plan

1. 新增 `shops` 地址字段和 `orders` 收货地址快照字段。
2. 对已有店铺和订单，新增字段允许为空；历史订单继续使用现有 `address` 展示。
3. 新提交的店铺注册/编辑和订单创建必须填写结构化地址。
4. 后续如接入真实地图服务，再迁移 `addressValidationStatus` 和经纬度字段。
