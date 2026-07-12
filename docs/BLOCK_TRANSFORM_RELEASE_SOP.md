# UTD 方块替换制造｜正式录入与发布 SOP

## 当前安全基线

- 运行配置：`UtilWeDie-Neo-1.21.1/.minecraft/versions/1.21.1-NeoForge_21.1.233/config/utd_asset_manager/block_transforms.json`
- schema：`utd-block-transforms/v1`
- 当前规则：`0`；当前启用：`0`
- 空配置 SHA-256：`D3DE974F0CEF51DA31A863BFBFD714AE54F16299906883CF2B4CBA50E8027EE0`
- 固定候选：`UtilWeDie-Neo-1.21.1/.minecraft/versions/1.21.1-NeoForge_21.1.233/config/utd_asset_manager/block_transforms.candidate.json`
- 自动备份：同目录 `block_transforms.json.bak`
- 规范项目：`outputs/projectutd-assets-20260711/workbench/workbench.json`
- 规范生成物：`outputs/projectutd-assets-20260711/workbench/utd_block_transforms.json`

资产管理器 test4 已实机通过三条运行器薄切片：主手持材替换普通方块、正确材料取消工作台原交互后替换、36 格玩家背包取材并要求潜行。测试用黏土球规则已全部清除，不是正式内容。

## 规则生命周期

```text
网页禁用草稿
  → 下载单一 candidate.zip
  → 保留 workbench.json + manifest
  → 只校验 block_transform
  → 与活动规则做严格语义差异
  → 人工确认
  → 快照规范源与运行配置
  → 复制为固定 candidate 文件
  → 游戏内 validate，取得原始字节 SHA-256
  → promote <sha256> 原子晋升
  → 测试世界验收
  → 正式部署
  → 提交规范项目、生成物、manifest 与发布回执
```

禁止从网页下载后直接覆盖活动 `block_transforms.json`；禁止只保存独立规则 JSON 而不保存同时修改后的规范项目、ZIP manifest 与差异报告。

## 正式录入字段

每条规则必须明确：

1. `id`：小写且唯一，格式建议为 `utd:block_transform/<用途>`。
2. `target`：被右键方块、可选状态约束、方块实体策略。
3. `catalyst`：物品 ID、精确变体、数量、来源与是否消耗。
4. `activation`：主手/副手/任意手、是否必须潜行、是否允许 FakePlayer。
5. `result`：结果方块、显式状态和要从目标复制的兼容属性。
6. `creative`：创造模式是否仍需材料、是否消耗。
7. `priority`：普通正式规则默认 `100`；`1000` 以上仅用于短期测试或紧急覆盖。相同目标状态不得留下同优先级冲突。

`source=inventory` 默认必须配合 `requireSneak=true`。其材料范围仅为玩家 36 格主背包和快捷栏，不包括副手、盔甲、合成格、鼠标光标或打开的容器。

## 安全边界

- v1 拒绝目标或结果方块实体，避免清空箱子等内容。
- v1 拒绝门、床、双格植物、活塞头等多方块敏感结构。
- 任意规则格式、注册名、状态、SNBT 或重复 ID 错误都会 fail-closed：整份规则停止运行，但原文件不被空文件覆盖。
- 活动规则仅在 Mod 启动、显式 `promote` 或管理员 `reload` 时读取；普通右键、文件时间变化和 `status` 都不会热加载磁盘候选。
- `validate` 固定读取 `block_transforms.candidate.json`，只解析和按当前游戏注册表编译，不改变活动 generation。
- `promote` 必须携带 `validate` 返回的 64 位小写 SHA-256；晋升时会重新读、重新校验，并先保留 `.bak`。平台不支持同目录原子移动时直接失败。
- 专用服务器暂时没有规则同步；客户端与服务端必须使用同一文件和同一 SHA-256。
- 保护 Mod、领地、冒险模式和 FakePlayer 必须在实际目标服务器单独验收。

## 候选差异必须包含

- 新增、删除、修改和启停的规则 ID。
- priority 与目标方块/状态。
- catalyst ID/变体/数量/来源/消耗。
- hand、潜行、FakePlayer。
- 结果方块/状态/复制属性。
- 创造模式材料与消耗策略。
- 候选文件 SHA-256、规则总数和启用数。

## 部署与回滚

1. 网页下载候选 ZIP，核对 ZIP 中 `manifest.json` 的三个文件 SHA-256；不要直接部署浏览器下载目录中的散文件。
2. 在独立 staging 目录解包，以 ZIP 内 `workbench.json` 重新导出，并执行：

   ```powershell
   npm.cmd run cli -- validate --project ".\workbench.json" --only block_transform
   npm.cmd run cli -- diff-block-transforms --baseline "<活动 block_transforms.json>" --candidate ".\utd_block_transforms.json" --out ".\utd_block_transform_diff.json"
   ```

3. 人工审查差异；快照旧 `workbench.json`、规范生成物、manifest、活动配置和已有 `.bak`，记录 SHA-256。
4. 将审核后的 `utd_block_transforms.json` 复制并重命名为固定 `config/utd_asset_manager/block_transforms.candidate.json`。这一步不会激活规则。
5. 在测试世界以管理员或单机主人执行 `/utdasset transforms validate`；保存返回的 candidate SHA-256，并确认活动 generation 未变化。
6. 执行 `/utdasset transforms promote <sha256>`。只有同一候选重新校验/编译成功、哈希匹配并完成原子备份与替换后，活动 generation 才会增加。
7. 执行 `/utdasset transforms status`，确认规则总数、启用数和 `usable=true`；再做正向转换、错料、错方块、不误扣和普通交互回归。
8. 多人环境先在服务端完成校验/晋升，再在客户端停机时分发服务端的活动文件并重启客户端；核对两端活动文件 SHA 一致。当前命令作用于服务端配置，没有自动规则同步或远程改写客户端文件。
9. 失败时优先把 `.bak` 作为新的 candidate 走同一套 validate/promote 回滚。`/utdasset transforms reload` 会绕过 candidate/hash staging，只用于已人工恢复活动文件后的应急重载。
10. 测试通过后，才把 ZIP 内规范项目提升为权威源并提交规范项目、生成物、manifest、差异和发布回执。

## 单条正式规则的实机验收

- 正确目标、材料和触发条件：只转换一次，数量精确扣除，无回弹和额外掉落。
- 错误材料、数量不足、错误方块、错误手别或缺少潜行：不转换、不扣料。
- 可交互目标：匹配时取消原交互；不匹配时保留原交互。
- 创造模式：遵守 `creative.requireInput/consume`。
- 规则停用或删除：下一次加载后立即停止生效。
- 方块实体和多方块结构：安全拒绝，不丢内容。
