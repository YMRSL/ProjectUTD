# UTD Asset Manager 0.1.5-test5｜候选发布链测试

## 本轮目标

验证方块替换规则已经从“直接改活动文件”升级为可审计的候选发布链：候选校验不激活、SHA-256 钉扎晋升、原子备份、状态查询不热加载。同时确认网页中的配方、Loot、名称/介绍和方块规则编辑刷新后不会静默丢失。

本轮没有修改 Loot Core JAR、现有 KubeJS 配方或 Loot 脚本，也没有录入任何正式方块替换内容。活动配置和测试候选都应保持 `rules: []`。

## 自动验证基线

- Mod：`55 / 55` 测试通过，`clean test build` 成功。
- Workbench：`53 / 53` 测试通过，TypeScript 与 Vite 生产构建成功。
- 真实浏览器：配方/Loot 刷新恢复、规则字段、非法 JSON 回滚、阻断错误、单 ZIP 下载和清除草稿通过；控制台 `0 error / 0 warning`。
- 真实 890/960/798 项目：浏览器草稿约 316 KB；候选 ZIP 约 340 KB；ZIP 内三个核心文件的 SHA-256 与字节数复核一致。
- test5 构建 JAR SHA-256：`F75A1DC2FCF9607AFBE36FF0B9F9A6E7F94EFD32A6E23AFE9DE1A2C2323ADE50`。

## 部署记录

- 已安装：`utd_asset_manager-0.1.5-test5.jar`；运行目录内 UTD Asset Manager JAR 数量为 `1`。
- 已安装 JAR SHA-256：`F75A1DC2FCF9607AFBE36FF0B9F9A6E7F94EFD32A6E23AFE9DE1A2C2323ADE50`；JAR 内嵌版本为 `0.1.5-test5`。
- 回滚快照：`_local_snapshots/utd_asset_manager-0.1.5-test5-deploy-20260712-202805/`。
- 快照清单 SHA-256：`4DAFFE6AF0DEC2122872E2CDB6997941430B2F3DD9FBC292BE6F1BEB007030F2`。
- 活动配置与固定候选均为 66 字节安全空配置，SHA-256 均为 `D3DE974F0CEF51DA31A863BFBFD714AE54F16299906883CF2B4CBA50E8027EE0`。
- 初始没有 `block_transforms.json.bak`；它应只在 T4 的首次成功晋升时生成。

## 前置状态

1. test5 已安装。开始测试时请重新启动 Minecraft，避免复用旧 test4 进程。
2. 活动文件：`config/utd_asset_manager/block_transforms.json`。
3. 固定候选：`config/utd_asset_manager/block_transforms.candidate.json`。
4. 两份文件初始都应为合法空配置，活动文件 SHA-256 为 `D3DE974F0CEF51DA31A863BFBFD714AE54F16299906883CF2B4CBA50E8027EE0`。
5. 不要手工修改 `block_transforms.json`；本轮只晋升相同的空候选，不会产生方块替换。

## 必测清单

### T1｜版本与空规则状态

启动游戏并进入测试世界：

1. Mods 页面应显示 `UTD Asset Manager 0.1.5-test5`。
2. 输入 `/utdasset transforms status`。

预期：

- 只显示相对路径 `config/utd_asset_manager/block_transforms.json`，不能出现 `D:\...` 等主机绝对路径；
- `total=0`、`enabled=0`、`usable=true`、`error=<none>`；
- 记下当前 `generation`，供 T2/T3 对比。

### T2｜候选校验不激活

输入 `/utdasset transforms validate`，然后再次输入 `/utdasset transforms status`。

预期：

- validate 显示候选相对路径、64 位小写 `sha256`、`total=0`、`enabled=0`、`usable=true`；
- status 的活动 `generation` 与 T1 完全相同；
- 校验本身不会生成 `.bak`，也不会改变活动规则。

### T3｜错误哈希必须失败

输入：

```text
/utdasset transforms promote 0000000000000000000000000000000000000000000000000000000000000000
```

预期：

- 明确提示 SHA-256 mismatch，`promoted=false` 或命令失败；
- 再查 status，`generation`、`total=0` 和 `enabled=0` 均不变；
- 任何方块都没有获得替换规则。

### T4｜正确哈希原子晋升

复制 T2 返回的真实小写 SHA-256，输入：

```text
/utdasset transforms promote <T2 返回的 sha256>
```

预期：

- `promoted=true`、`usable=true`、`error=<none>`；
- 活动 `generation` 增加 1，仍为 `total=0`、`enabled=0`；
- 配置目录出现 `block_transforms.json.bak`，内容是晋升前的空活动文件；
- 随便右键普通方块、箱子、门和工作台，不发生替换、不扣物品，原交互保持正常。

### T5｜显式应急重载

输入 `/utdasset transforms reload`。

预期：`generation` 再增加 1，仍为 `total=0`、`enabled=0`、`usable=true`。这条命令会绕过候选/SHA 流程，只能由管理员或单机主人用于人工回滚和应急；正式规则发布必须使用 validate/promote。

## 可选网页抽查

双击 `Tools/UTDAssetWorkbench/打开UTD资产工作台.cmd`：

1. 修改一项配方工作站和一项 Loot 等级，刷新后两项都应恢复；点击“清除”后恢复载入基线。
2. 新建并启用一条未填目标/结果的规则，候选 ZIP 必须被中文错误阻止。
3. 修复或停用阻断项后，点击“候选包 ZIP”只下载一个 ZIP；内部应有 `workbench.json`、`utd_block_transforms.json`、`utd_item_presentations.json`、`manifest.json`。

网页抽查结束后点击“清除”，不要把测试草稿留在浏览器中。

## 失败时保留

- 聊天栏完整截图，必须包含执行的命令和整行返回值；
- `logs/latest.log`；
- 当时的 `block_transforms.json`、`block_transforms.candidate.json`、`block_transforms.json.bak`；
- 若网页失败，保留浏览器控制台截图和下载到的 ZIP，不要继续覆盖。

## 回报模板

```text
T1 版本/状态：PASS / FAIL
T2 校验不激活：PASS / FAIL（前后 generation：___ / ___）
T3 错误哈希拒绝：PASS / FAIL
T4 正确哈希晋升：PASS / FAIL（晋升后 generation：___）
T5 显式重载：PASS / FAIL
网页可选抽查：PASS / FAIL / 未测
补充截图或 latest.log：___
```
