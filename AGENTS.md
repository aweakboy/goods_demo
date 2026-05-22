# Codex 项目指令

## OpenSpec 测试策略

对每个 OpenSpec 变更，测试是完成定义的一部分，不是事后补充。

创建或更新变更时：

- 在 OpenSpec artifacts 中包含测试策略，通常写在 `design.md` 和 `tasks.md` 中。
- 将重要 spec 场景映射到明确的测试或验证任务。
- 对业务逻辑、状态流转、权限、持久化和回归问题，优先使用自动化测试。
- 仅在自动化不现实的情况下使用手动验证，例如第三方支付沙箱流程、视觉检查、本地隧道回调或依赖环境的行为。使用手动验证时，必须准确说明要检查什么。
- 相关测试或验证命令通过前，不要将变更标记为完成。

基于风险的测试期望：

- 后端业务逻辑变更通常应包含 JUnit/Mockito service 测试。
- 后端 API、认证、权限、支付、订单、退款、库存、超时和幂等性变更，在可行时应包含 controller 或集成覆盖。
- 当前端测试基线建立后，前端 composables、stores、router guards、API wrappers 和非平凡 UI 状态都应包含自动化测试。
- 前端纯视觉变更可以依赖 `npm.cmd run build` 加重点手动验证，除非交互复杂或容易回归。
- 关键用户流程最终应具备 E2E 覆盖。

本项目验证命令：

- 后端验证命令：在 `backend/` 目录运行 `mvn.cmd test`。
- 前端基线命令：在 `frontend/` 目录运行 `npm.cmd run build`。
- 如果后续加入前端测试 runner，每个相关前端变更也应运行对应测试命令，例如 `npm.cmd run test`。

OpenSpec task 文件应避免模糊的最终步骤，例如：

```text
- [ ] 手动验证
```

应使用明确任务：

```text
- [ ] 添加退款批准和拒绝路径的 service 测试
- [ ] 添加非管理员访问退款接口返回 403 的 API 测试
- [ ] 运行 `mvn.cmd test`
- [ ] 运行 `npm.cmd run build`
- [ ] 手动验证支付宝沙箱回调会将订单更新为 PAID
```

执行 `openspec-apply-change` 时，在可行情况下应将测试和相关功能任务一起实现。如果现有变更缺少测试任务，应暂停并建议先补充测试任务，再声明变更完成。

## GitHub 推送策略

每次执行 `openspec validate` 且校验通过后，应将当前变更代码提交并推送到 GitHub 对应分支。对应分支优先使用当前工作分支；如果当前分支不匹配 OpenSpec change 名称，应按项目约定切换或创建对应分支后推送。

如果推送需要网络、凭据或权限授权，应先请求授权；如果校验失败，不得推送。

## 编码策略

包含中文文本的项目文件使用 UTF-8 编码。在 Windows PowerShell 5.1 上，`Get-Content` 可能会将无 BOM 的 UTF-8 文件按本地 ANSI 代码页解码并显示乱码。

从 PowerShell 读取中文项目文件时，使用明确的 UTF-8 解码：

```powershell
Get-Content -Path "CLAUDE.md" -Encoding UTF8
Get-Content -Path "openspec\changes\<change>\proposal.md" -Encoding UTF8
```

搜索文本时优先使用 `rg`，因为它能正确处理本仓库现有 UTF-8 文件。不要仅因为默认 PowerShell 输出看起来乱码就重写文件；应先用 `-Encoding UTF8` 确认。
