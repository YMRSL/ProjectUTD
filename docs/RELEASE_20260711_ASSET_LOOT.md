# 誓死坚守｜资产、配方与 Loot 发布记录（2026-07-11）

## 结论

本轮已完成并部署“游戏内人工标注 → 桌面审查/配方图 → KJS/Excel/status → 游戏内反馈”的 MVP，以及 Loot 从散落 KubeJS/数据包到单一 JAR 的封盘。

当前状态分为两层：

- **自动验收完成**：源码、构建、单元测试、数据校验、确定性导出、Excel 公式/视觉检查、网页真实数据与交互检查均通过。
- **待用户实机验收**：当前完整整合客户端的 O 键界面、Tooltip、标注持久化、三类箱子和 T4/T5 保底仍需进游戏验证。自动测试通过不替代这一闸门。

## 正式产物

| 产物 | 位置 | 大小 | SHA-256 |
| --- | --- | ---: | --- |
| UTD Asset Manager | `outputs/projectutd-assets-20260711/mods/utd_asset_manager-0.1.0-mvp.jar` | 51,160 | `E8C64C9F6BE51DE7B0D225C67EFF17A98F92F9A6E7940465F31ECA9DD12EC371` |
| UTD Loot Core | `outputs/projectutd-assets-20260711/mods/utd_loot_core-1.0.0-1.21.1.jar` | 294,570 | `42DF9ABC835B9B206E49970005627EF16D8F7E658EEC972C1E1D0788EAB33823` |
| 状态清单 | `outputs/projectutd-assets-20260711/workbench/status_manifest.json` | 875,687 | `386026BAB042147AD94AD646D66D09C1AE30D362F5636922606DE79736FE0735` |
| 规范工作台项目 | `outputs/projectutd-assets-20260711/workbench/workbench.json` | 7,528,227 | `58BA4493A75DCBA63AD7171D6CD413810DB8F3D2EC38360B61667D4644D67672` |
| Excel 审阅工作簿 | `outputs/projectutd-assets-20260711/excel/誓死坚守_资产配方与Loot管理_20260711.xlsx` | 480,363 | `269B0EE42ABAC54DEA455B53770861FB16A189C9F6BE8EB387467A6CE6B2EEAC` |

工作台发布时刻为 `2026-07-11T19:53:57+08:00`，catalog hash 为 `797b002a05c3dbbb`。配方源自己的旧生成时间独立保存在 source 字段，不再冒充本次发布日期。

## 数据口径

- 状态身份：851。
- 旧工作簿保守导入的人工根：316；进入游戏重新标注后，将逐步替换为真实中文名、translation key 和稳定 variant 身份。
- 配方：960；Loot registry：798，其中 678 启用、120 禁用。
- 可视一层图节点：1,255。
- 当前问题：319，其中 316 条是旧工作簿缺 translation key，2 条是无配方产出/启用 Loot 的孤立根，1 条是包含 280 个物品的回收/互转强连通循环。
- 当前 status 不伪造“已同步”：571 项为 `local_only`，循环涉及的 280 项为 `error`。这表示需要内容审查，不表示 Mod 或清单损坏。

同一份规范项目连续导出两次，八个产物逐文件 SHA-256 完全一致。KJS 生成链已具备可重复性；本轮没有用生成候选覆盖现有运行配方，后续修改配方必须先审查循环和差异。

## 本地部署回执

目标客户端：`UtilWeDie-Neo-1.21.1/.minecraft/versions/1.21.1-NeoForge_21.1.233`

已完成：

1. 确认无 Java/Minecraft 进程。
2. 安装两只正式 JAR，运行目录哈希与上表完全一致。
3. 安装 `config/utd_asset_manager/status_manifest.json`，哈希与上表完全一致。
4. 将旧 Loot 的 7 个脚本、78 个 helper 表、280 个箱表整体移出 KubeJS；原 9 个路径均已不存在，避免 KubeJS 资源包覆盖 JAR。
5. 365 个旧文件全部保存在 `_local_snapshots/utd_loot_kubejs-retired-20260711/`，没有删除。
6. 更早的完整 KubeJS 基线仍保存在 `_local_snapshots/runtime-kubejs-baseline-20260711.zip`，SHA-256 为 `DED3D9882C5EA821E7FFBD602BB137AE9F0BF4784331951CA548569A1874042A`。

## 用户实机验收包

### A. 资产管理器（约 15 分钟）

1. 确认客户端语言为简体中文，启动当前 `1.21.1-NeoForge_21.1.233`，进入测试世界。
2. 按 `O`；应打开 UTD 资产档案台。若快捷键冲突，输入 `/utdasset open`。
3. 检查玩家背包物品；再打开一个容器、关闭容器并按 `O`，确认刚才容器内的可见物品也成为候选。
4. 选择一个普通物品和一把 TaCZ 枪分别标注；Tooltip 应分别显示人工标注、已纳管、配方、Loot、同步和问题状态。
5. 对 TaCZ 枪射击或切换模式后重新查看；它应仍是同一个 GunId 资产，不应新增重复记录。若方便，再用一个 FPE 食物验证耐久/食用变化不会制造第二个 food_id 资产。
6. 取消其中一个标注，再重新标注；退出客户端并重启，确认选择仍保留。
7. 输入 `/utdasset export`；保留聊天提示和 `config/utd_asset_manager/exports/` 中最新 JSON。

### B. Loot（约 20–30 分钟）

1. 冷启动后输入 `/utdloot status`；应显示 798 条 registry、280 个容器。
2. 检查日志中没有新的 `kubejs:utd`、UTD helper 缺失或重复 Loot 表错误。
3. 分别打开民用 `blackvan_1`、医疗 `ambulance_1`、军事 `ammunitionbox` 或 `airdrop` 容器。
4. 用 `/utdloot set civilian 10 0`、`/utdloot set medical 10 0`、`/utdloot set military 10 20` 做保底测试；下一只符合条件的新容器应分别触发 T4/T5，并重置相应计数。
5. 重开同一容器，计数不应再次变化；重启世界后，玩家计数和容器已处理标记应保留。

### C. 失败回传

若任一步失败，请保留：

- 失败步骤编号和实际画面；
- `latest.log`；
- 资产问题附最新导出 JSON；
- Loot 问题附 `/utdloot status`、`/utdloot pity` 的聊天截图；
- 出错容器名称以及是否首次打开。

回传格式：`资产 A1–A7：PASS/FAIL；Loot B1–B5：PASS/FAIL；失败步骤：…；附件：…`。

## 下一批内容工作

1. 用游戏内真实标注替换 316 条旧工作簿种子，补齐中文名、translation key 和稳定变体身份。
2. 审查 280 物品回收/互转循环，区分合理回收闭环、重复方案和可套利路径。
3. 在工作台中逐项微调 960 条配方，生成 diff 后再部署；Excel 只负责审阅，不反向成为权威源。
4. 实机闸门通过后再将状态推进为 synced，并开始 Social Will 主线。
