# UTD Asset Manager 0.1.6-test6｜图标、分类与免手抄晋升测试

## 本轮目标

关闭资产管理器最后三个操作缺口：游戏导出的已标注物品携带真实客户端图标；网页按旧工作簿 8 类浏览并自动使用最新游戏图标；候选校验后可点击填入完整 SHA-256 晋升命令。

本轮不修改 Loot Core、现有 KubeJS 配方、Loot 脚本或活动方块替换内容。活动和候选规则仍应保持 `rules: []`。

## 自动验证基线

- Mod：`55 / 55` 测试通过，`clean test build` 成功；JAR 为 `utd_asset_manager-0.1.6-test6.jar`，167,789 字节，SHA-256 `28AAB1E4A21B2147CEB35F28C76A43C7F2B95CD5660C1EB574C1E73B918A4771`。
- Workbench：`56 / 56` 测试通过，TypeScript 与 Vite 生产构建成功。
- 真实浏览器：890 条目录、960 配方、798 Loot 正常载入；分类组合筛选、枪械 48 条视图、检查器分类字段通过；控制台 `0 error / 0 warning`。
- 分类源：旧工作簿“汇总”表 8 类、432 条唯一映射；`tacz:ammo_box` 的重复等级冲突保留分类但等级置为未定。

## 部署记录

- 已安装：`utd_asset_manager-0.1.6-test6.jar`；运行目录内 UTD Asset Manager JAR 数量为 `1`。
- 已安装 JAR：167,789 字节；SHA-256 `28AAB1E4A21B2147CEB35F28C76A43C7F2B95CD5660C1EB574C1E73B918A4771`。
- 回滚快照：`_local_snapshots/utd_asset_manager-0.1.6-test6-deploy-20260712-214055/`。
- 活动配置与固定候选均保持 66 字节安全空配置，SHA-256 `D3DE974F0CEF51DA31A863BFBFD714AE54F16299906883CF2B4CBA50E8027EE0`。
- 部署时仍无 `block_transforms.json.bak`；它应在 T4 首次成功晋升时生成。

## 必测清单

### T1｜版本与原功能冒烟

1. 完全退出旧客户端后安装 test6，再启动游戏进入测试世界。
2. Mods 页面确认 `UTD Asset Manager 0.1.6-test6`。
3. 按 O 打开资产界面；确认列表、搜索、检查器和已有标注仍正常。

### T2｜游戏内导出真实图标

1. 本机白名单里至少保留一个普通物品；若方便，再保留一个 FPE 食品和一个 TaCZ 枪械变体。
2. 点击右下角“导出+图标”。导出时画面可能短暂闪过一帧图标采集网格，这是正常现象。

预期：

- 底部提示“已导出 N 个图标与物品数据”，且 N 大于 0；
- 不崩溃、不取消已有标注，名称/介绍草稿仍单独导出；
- 若某条历史数据无法恢复为游戏物品，只跳过该条图标，不能伪造或串到其它变体。

### T3｜网页分类与图标

1. 完成 T2 后，双击 `Tools/UTDAssetWorkbench/打开UTD资产工作台.cmd`。
2. 在左栏“表格分类”选择“枪械”，再点“纳管”。
3. 搜索一个完整的长 `food_id`，再清空搜索。

预期：

- 启动器自动读取最新游戏导出，不要求手工选择 JSON；
- T2 成功捕获的物品显示游戏图标，不再显示命名空间首字母占位；
- FPE/TaCZ 图标严格跟随 `food_id / GunId`，不能在同 registry 的变体之间串图；
- “枪械 + 纳管”显示 48 条；分类可与白名单/纳管/依赖/问题和搜索组合；
- 检查器显示分类与等级；表格未覆盖的新物品明确显示“未分类”。

### T4｜点击填入正确哈希并完成 test5 遗留晋升

1. 输入 `/utdasset transforms validate`。
2. 在返回消息末尾点击“点击填入晋升命令”。必要时先按 T 打开聊天栏，再点击上一条消息。
3. 确认输入框中自动出现完整 `/utdasset transforms promote <64位sha256>`，直接回车执行。

预期：

- 不需要手抄或手动复制 SHA；
- `promoted=true`、`usable=true`、`error=<none>`；
- generation 增加 1，规则仍为 `total=0 / enabled=0`；
- 配置目录存在或更新 `block_transforms.json.bak`；普通方块、箱子、门和工作台仍不发生替换、不误扣物品。

### T5｜回归

1. 标注/取消标注一个普通物品后恢复原状态。
2. 抽查名称/介绍编辑仍可保存。
3. `/utdasset transforms status` 仍只显示相对路径，不能泄露主机绝对路径。

## 回报模板

```text
T1 版本/界面：PASS / FAIL
T2 图标导出：PASS / FAIL（提示数量：___）
T3 网页分类/图标：PASS / FAIL
T4 点击晋升：PASS / FAIL（晋升后 generation：___）
T5 回归：PASS / FAIL
补充截图或 latest.log：___
```
