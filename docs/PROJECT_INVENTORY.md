# 誓死坚守｜项目文件与成熟度总清单

> 只读盘点基线：2026-07-11。  
> 本页回答“文件在哪里、扮演什么角色、做到哪一层、下一道闸门是什么”。项目优先级与任务状态仍由 PROJECT_STATUS.md 管理，模块技术设计仍以各自 ARCHITECTURE.md、HANDOFF.md 和修订日志为准。

## 0. 盘点后的发布增量

本页后续各节保留 2026-07-11 初次只读盘点的证据口径；当天稍后完成的资产/Loot 增量以 `docs/RELEASE_20260711_ASSET_LOOT.md` 和 `docs/PROJECT_STATUS.md` 为当前事实：

- runtime KubeJS 已有完整本地基线 ZIP，修改前恢复点已关闭；
- UTD Loot 的 798 registry、78 helper、280 容器表和 Java 保底已封装为 JAR，旧 365 个 KubeJS 文件已移入回滚目录；
- NeoForge 游戏内资产标注 Mod、851 身份规范项目、960 配方/798 Loot 桌面工作台、状态清单、确定性 KJS 输出和正式 Excel 已完成；
- 两只 JAR 与状态清单已安装到当前 NeoForge 21.1.233 客户端，完整客户端冷启动、开箱和标注持久化仍是待人工闸门；
- 当前内容债务是 316 条旧种子缺 translation key、2 条孤立根和 1 个包含 280 物品的回收/互转循环；这些是后续配方审查输入，不应再描述为“资产管理器未开始”。

## 1. 扫描口径

本次盘点覆盖：

- SocialWill 的 Picasso、Chess、MiroFish，以及参考源码 SocialWill/sw_src；
- NeoForge 1.21.1 移植源码、构建物与当前整合客户端部署状态；
- KubeJS 配方、Loot、CNPC 脚本、工作簿和运行日志；
- 正传、前传、测试世界、地图参考素材；
- Git 跟踪、忽略、LFS、未跟踪文件和远端恢复基线。

进度不使用单一百分比。每条工作流按五个彼此独立的维度判断：

| 维度 | 判断内容 | 不能替代它的证据 |
| --- | --- | --- |
| 规格 | 范围、契约、失败语义和完成标准是否明确 | 文件名、TODO 或口头设想 |
| 实现 | 是否存在可调用源码或可执行数据 | 只有 docs 中的类名/API 草图 |
| 自动测试 | 是否有可重复的离线或临时世界验证 | “曾经能编译” |
| 实机 | 是否在指定 MC、NeoForge、模组和世界中验证 | pytest、JAR 生成成功 |
| 集成 | 上下游是否按真实契约闭环并能恢复 | 单模块演示成功 |

状态词统一为：稳定、部分、待人工验证、未开始、阻塞。文档完成、自动测试通过、实机通过和跨层集成通过不得互相代替。

## 2. Git 与恢复基线

### 2.1 已有公开快照

- 分支：codex/sprint-baseline-20260711
- 远端：origin，仓库 YMRSL/ProjectUTD
- 扫描时远端基线：8e5ef182
- c79c01ba：项目治理、快照清单和仓库内 v1 工作簿
- 5111900c：Picasso 源码、测试、数据和文档
- 8e5ef182：Chess 与 MiroFish 规格文档
- 仓库内 v1 工作簿通过 Git LFS 保存。

这是一份“主动维护源码/规格”的恢复点，不是完整客户端、世界或素材备份。以下内容不在这份恢复点内：

- UtilWeDie-Neo-1.21.1/.minecraft 下的客户端、当前实际 KubeJS 配方/Loot 数据、日志和配置；
- UtilWeDie-Neo-1.21.1/建筑存档、地图管理和 live saves；
- 构建输出、JAR、压缩包、缓存；
- CNPC 参考脚本库及来源/许可未确认的贴图、音频和图片；
- 被忽略的本地秘密，例如 SocialWill/MiroFish/.env.local。

### 2.2 v2 交付纳入快照后的未跟踪边界

共有 26 个待审未跟踪文件：

- 19 个 CNPC 活跃包文件：安装说明、6 个 clone、KubeJS bridge、模型/动画/声音定义和验证说明；
- 6 个 MiroFish Spike Zero 的 CNPC fixture/probe；
- 1 个世界观文档生成脚本；

不得对这些区域执行整目录暂存。每一批必须先审查来源、秘密、许可、文件大小和是否属于生成中间物。

## 3. 顶层路径与角色

| 路径 | 当前角色 | 数据类别 | Git/备份边界 |
| --- | --- | --- | --- |
| docs/ | 项目总控、恢复清单、仓库地图 | AUTHORITATIVE 管理文档 | 正常 Git |
| SocialWill/Picasso/ | 世界编辑 MCP、测试、规范、数据定义 | AUTHORITATIVE 源码；部分 JSON 为 shipped data | 已快照 |
| SocialWill/Chess/ | 兵棋/派系层规格 | AUTHORITATIVE 规格 | 已快照；图片素材另审 |
| SocialWill/MiroFish/ | 叙事层规格与 Spike Zero | AUTHORITATIVE 规格；fixture 为 REVIEW | 规格已快照，运行 fixture 未快照 |
| SocialWill/sw_src/ | SuperbWarfare 参考/上游源码 | VENDOR/REFERENCE | 已跟踪，但不是 Chess 实现 |
| UtilWeDie-Neo-1.21.1/ModsSourceCode/ | 活跃移植、补丁、上游副本和报告混合区 | AUTHORITATIVE、VENDOR、GENERATED 混合 | 源码子集已跟踪；build/JAR 忽略 |
| UtilWeDie-Neo-1.21.1/.minecraft/ | 当前整合客户端和实际部署 | DEPLOYED/RUNTIME | 整体忽略；不是权威源 |
| CNPCScripts/ | 多版本参考库与 Fungal 活跃包 | REFERENCE 与 AUTHORITATIVE 混合 | 未完整快照；需先做 manifest/许可审查 |
| Code/ItemsReFresh/ | 旧 Spigot/Mohist 区域容器刷新插件 | LEGACY SOURCE | 已跟踪；不是资产管理器 |
| Migration-1.21.1/ | 迁移清单与差异说明 | AUTHORITATIVE 迁移记录 | 已跟踪 |
| 策划案以及文档相关/ItemNameCatch分类汇总_合成设计方案_v1.xlsx | 仓库内物品/合成种子工作簿 | SOURCE CANDIDATE | 已用 LFS 快照；尚未冻结为唯一权威源 |
| outputs/projectutd-sprint-20260711/ | 可读 v2 工作簿 | GENERATED/REVIEW | v2 通过 LFS 跟踪；inspect 中间物忽略且不保留 |
| 合成表修改/ | 预留目录 | EMPTY | 暂无权威内容 |
| UtilWeDie-Neo-1.21.1/建筑存档/ | 正传、前传、Picasso 测试副本 | RUNTIME/TEST/ARCHIVE | 忽略；应进私有备份而非普通 Git |
| 地图管理/ | TARKOV、列车建筑和 schematic 参考 | REFERENCE/ARCHIVE | 忽略；需外部存储策略 |
| 待删除或整改升级的mod/ | 待审下载/整改区 | QUARANTINE | 忽略；未经复核不得删除 |

## 4. 五维成熟度矩阵

| 工作流 | 规格 | 实现 | 自动测试 | 实机 | 集成 |
| --- | --- | --- | --- | --- | --- |
| Picasso | 稳定 | 部分：22 个 MCP 工具与安全底座可用 | 稳定：本轮 407 passed、1 skipped、14 subtests | 待人工：DD 三 marker | 阻塞：无 work-order dispatcher/receipt |
| Chess | 稳定：v0.1.4、P0-P9 | 未开始：无 Chess 代码 | 无 | 无 | 仅契约 |
| MiroFish | 部分稳定：checkpoint-0 已关，子系统文档待补 | 探针：Spike Zero fixture；无 MCP/正式入口 | 无 | 待人工：测试 0-5 | 仅契约 |
| SocialWill 三层薄切片 | 部分：契约已设计但有跨文档漂移 | Picasso 单边底座 | 仅 Picasso | 未联调 | 未闭环 |
| NeoForge/Mod 移植 | 部分：各项目报告口径不统一 | 部分：22 个业务区有构建 JAR，20 个已同名或同哈希部署 | 弱：业务移植工程几乎无 tests | 部分：整合客户端可启动、进世界和正常退出 | 部分：仍有明确资源/兼容错误 |
| KubeJS 配方 | 部分：多份工作簿仍待字段裁决 | 960 配方已进入规范工作台与确定性生成链；运行版本未被候选覆盖 | 稳定：解析、关系、循环、变体和连续导出测试 | 最近运行版本已执行；新生成候选待后续实机 | 部分：280 物品回收循环与旧种子翻译键待审 |
| Loot | 稳定：798 registry、78 helper、280 容器与保底契约已冻结 | 单一 NeoForge JAR 已安装，旧 KubeJS 365 文件可回滚退休 | 稳定：4 Python + 6 JUnit + JAR/360 哈希校验 | 待人工：冷启动、三类箱子、T4/T5 保底 | 部分：代码与部署已闭环，实机回执未关 |
| CNPCScripts | 部分：有安装/验证说明，无统一 manifest | 部分：Fungal 包可部署；大量脚本仍是旧版本参考 | 无 | 部分 | 部分：存在源码/部署漂移 |
| 资产管理器 | 稳定：游戏内标注、状态清单、桌面工作台和一层图契约已冻结 | NeoForge Mod、网页工作台、KJS/status/Excel 输出与双击启动器已完成 | 稳定：Mod 12 项、工作台 12 项、浏览器与 Excel 检查通过 | 待人工：O 键、Tooltip、持久化、TaCZ/FPE 去重 | 部分：JAR/status 已部署，真实游戏导出待回流 |
| 世界/地图治理 | 无角色 manifest | 多份真实世界存在 | 无恢复演练 | 活跃使用 | 无私有备份闭环 |
| Git/项目治理 | 稳定起步 | 已有分模块远端快照与本地 runtime 基线 | Picasso、资产/Loot 自动验收已完成 | 不适用 | 部分：本轮发布待推送，CNPC/MiroFish 待审资产仍未纳入 |

## 5. 权威源、生成物与部署数据流

### 5.1 必须采用的角色

- AUTHORITATIVE：人可维护、可审查、唯一允许直接编辑的源。
- GENERATED：由固定工具从权威源产生，禁止手改。
- DEPLOYED：复制到客户端/世界供游戏读取的版本。
- RUNTIME EVIDENCE：日志、回执、截图、测试结果，只证明某次运行。
- REFERENCE/VENDOR：外部、旧版或上游材料，不直接成为生成输入。
- ARCHIVE：只用于恢复和溯源。

目标单向链：

权威源 → validate → generate → diff → deploy → 实机验证 → receipt/证据

任何 DEPLOYED 文件都必须能追溯到权威源版本和生成器版本；任何部署前都必须能查看 diff；任何失败都不得靠手改部署产物长期维持。

### 5.2 当前配方/物品数据现实

| 数据 | 当前事实 | 结论 |
| --- | --- | --- |
| 仓库内 v1 工作簿 | 汇总表为 439 条，但漏计枪械分类首行 M1911；实际分类总数为 440 | 数据很有价值，但统计口径需先修正 |
| 外部 v1 更新版 | 比仓库内 v1 更新，未纳入当前公开快照 | 属待对账来源，不能静默覆盖 |
| crafting_tables_v3_adjusted.xlsx | 外部的后续合成设计输入 | 与仓库 v1、运行时均有漂移 |
| outputs 下 v2 | 面向阅读与审查的新产物 | 当前是 GENERATED/REVIEW，不自动成为权威源 |
| runtime 的 utd_recipe_data.js | 2026-05-25 的 3 MB 生成快照；已进入完整本地 KubeJS 基线，并作为首份工作台输入 | 仍是当前运行配方；新生成候选必须先 diff，不能自动覆盖 |
| runtime Loot JS/JSON | 已进入完整本地基线；旧 7 脚本、78 helper、280 箱表另存为 365 文件回滚包 | 运行时已切换到 UTD Loot Core JAR，旧数据不再参与加载 |
| latest.log/KubeJS logs | 某次真实执行证据 | 不是源，也不能用于反向手工恢复全部设计 |

首份规范项目已保守使用 v2 中 316 条 READY 根，并把 runtime 960 配方作为现状证据；外部 v1 更新版、v3 adjusted 与剩余字段仍要逐项裁决。资产管理器不会把任意一个工作簿直接当数据库，游戏内真实标注将逐步替换旧种子。

## 6. SocialWill 三层

### 6.1 Picasso

已实现：

- 22 个 MCP 工具；
- Amulet 稀疏读取、halo、WriteChoke、定义库、Fragment/Bundle；
- Journal、冲突安全 revert、marker、玩家活动保护；
- 当前自动测试全绿。

下一闸门：

1. PIC-01：真实客户端验证 DD 三 marker 的渲染、朝向、碰撞与交互；
2. PIC-02：实现只处理 mark 的最小 dispatcher、幂等、Journal 与 receipt；
3. 通过后再拉 segmentation、Room、Rail 等后续能力。

### 6.2 Chess

当前是规格完成、实现未开始。SocialWill/sw_src 是 SuperbWarfare 参考源码，不能算 Chess 代码。

下一闸门：

1. CHS-01：FTB、CNPC/TACZ、CNA、Create 精确版本能力探针；
2. CHS-02：纯 Java core、确定性 fixture、持久化与 event log；
3. event log 稳定后再接最小 NeoForge shell/headless 命令。

### 6.3 MiroFish

当前有 checkpoint-0 架构和 Spike Zero 源码分析，但没有 MCP Server、正式入口、事实摄取器或内容编译器。

下一闸门：

1. MIR-01：完成 Spike Zero 测试 0-5；
2. MIR-02：关闭最小 OASIS/Graphiti spike，冻结版本与 fallback；
3. MIR-03：实现事实摄取、内容验证、CNPC pack compiler 与带 fact_refs 的 nar_* mark 输出。

### 6.4 跨层缺口

- Picasso 的 wargame_interface 仍写“恰好两个 writer”，MiroFish 已要求 narrative writer；
- work-order ID、目录、状态机、幂等、失败回执和 writer 权限必须统一成一份规范；
- 最小验收链固定为 Chess fixture/event → MiroFish CNPC 内容与 nar_* mark → Picasso marker/Journal/receipt → 重复、失败与回滚验证。

## 7. 配方与 Loot

### 7.1 配方运行事实

runtime 数据共有 960 个 recipeId 候选：

- 51 shaped；
- 26 shapeless；
- 883 custom。

最近一次处理结果：

- 30 个因缺失物品/模组被显式跳过；
- 12 个命中 BLOCKED_RESULTS，被策略性静默禁用；
- 918 个进入注册调用；
- failedRecipeIds 为 0。

日志中的 “registered from 51 / 26 / 883” 是输入数组长度，不是成功注册数量。后续生成报告必须直接输出 candidate、policy_disabled、missing、failed、submitted 五类计数，禁止继续使用含混文案。

DATA-01 的完成标准：

- M1911 等首行/跨表漏计被修正，分类与汇总可对账；
- 外部更新版、v3 adjusted、仓库 v1 和 runtime 的差异逐项有裁决；
- 权威源和生成器进入可恢复快照；
- validate → generate → diff → deploy 可重复；
- 缺失 ID、重复 ID、命名空间漂移、旧模组引用和策略禁用均有机器可读报告。

### 7.2 Loot 封盘状态

- 798 条 registry 保留，678 启用；14 条缺失模组引用及其它策略禁用项仍保留审计记录，但不会成为新投放候选。
- 4 个 `paraglider:paraglider` tier1 引用已移除，78 个 helper 与 280 个 `doomsday_decoration` 容器表通过闭包和哈希校验。
- TaCZ `GunId`、FPE `food_id` 与工作台 `BlockId` 已用于保底扫描的稳定逻辑身份；禁用旧条目只参与已有物品等级识别。
- Java 保底、命令、数据与元数据已封装为 `utd_loot_core-1.0.0-1.21.1.jar`；旧 365 个 KubeJS 文件已移入本地回滚目录。
- 当前闸门不再是离线修复，而是完整客户端冷启动、三类箱子、T4/T5、重复开启与重启持久化实机验收。

## 8. CNPCScripts

盘点基线：

- 289 个文件、222 个 JS；
- 路径明确标为 1.21.1 的普通脚本只有 5 个；
- 大多数普通脚本位于 1.20.1 参考区，另含 1.12.2 和危险/实验示例；
- Fungal 活跃包含 6 个 clone、资源包和可选 KubeJS 开枪吸引 bridge。

部署对账：

- 资源包源与 runtime 部署文件一致；
- 6 个 clone 中 5 个与 runtime 同哈希；
- Fungal_Infected_Creep.json 的源码与部署已漂移；
- fungal_gunshot.js 当前未部署到 runtime KubeJS；
- MiroFish 的 6 个 fixture/probe 仍未跟踪并待实机测试。

整理前必须先建立 manifest，至少包含：文件 ID、目标 MC/CNPC 版本、来源、作者/许可、用途、危险等级、依赖模组、源哈希、部署哈希、最后实机结果。后续采用“源码 → compiler/install → diff → world/runtime”的单向同步，禁止从多个 clone 目录双向覆盖。

## 9. 地图与存档

| 区域 | 逻辑大小/事实 | 当前判断 |
| --- | --- | --- |
| 建筑存档/GreenField | 6481 文件，约 3.20 GB | 与 live saves 同名副本文件数、大小和 level.dat 哈希一致；抽样哈希一致，但不是 junction/hardlink |
| 建筑存档/Mosslorn | 约 433.7 MB | 与 live saves 版本已分叉，不可当重复项删除 |
| Picasso-Test1-pre-phase15 | 约 11.3 MB | Phase 1.5 前置恢复点，应保留 |
| live saves/新的世界 (1) | 与 Picasso 测试副本同规模但 level.dat 已变化 | 运行/测试本，不能覆盖前置恢复点 |
| 地图管理 | TARKOV、列车建筑等约 2.45 GB | 参考/归档素材，不进普通 Git |

建筑存档逻辑文件大小合计约 3.65 GB；`SNAPSHOT_MANIFEST.md` 已统一为“文件逻辑大小”口径，并分列 GreenField、Mosslorn 与 Picasso 测试副本。

世界治理必须先定义：

- production：正式运行本；
- test：允许重建或丢弃的测试本；
- archive：不可写恢复点；
- reference：只读地图/建筑素材。

每个世界登记路径、世界名、level.dat 哈希、最近打开时间、MC/模组版本、用途、写入者、备份位置和最近恢复演练。普通 Git 不承担世界备份。

## 10. Git 边界

### 可进入公开源码快照

- 自研源码、测试、规范、无秘密的小型结构化数据；
- 经确认许可的素材；
- 工作簿等必要二进制通过 Git LFS；
- 生成器可以入库，构建产物和大型 inspect 中间物不入库。

### 先保留本地或进入私有备份

- .minecraft、world saves、地图缓存、日志、JAR、压缩包；
- CNPC 混合参考库和来源未确认素材；
- 外部工作簿原件，直到来源与发布范围确认；
- .env.local、token、账号和机器配置。

### 当前最重要的快照缺口

真正被游戏使用的 `utd_recipe_data.js` 仍位于被忽略的 `.minecraft`，但已进入带哈希的本地 KubeJS 基线并进入规范工作台。Loot 已由可构建源码/JAR接管，旧运行数据另有 365 文件回滚包；未通过实机验收前不得清理这些本地恢复点。

## 11. 已知漂移与冲突

| 漂移 | 影响 | 处理 |
| --- | --- | --- |
| MiroFish README 写 checkpoint PARTIAL，ARCH 写 CLOSED | 项目状态误判 | 以 ARCH/REVISION_LOG 为准并修 README |
| Picasso writer list 只有 wargame/Picasso，MiroFish 要 narrative | 跨层订单未获正式授权 | CONTRACT-01 统一 |
| 安全快照状态曾落后于远端事实 | 容易误判项目仍不可整理 | 已把 SAFE-01 更新为完成；后续以 PROJECT_STATUS 为准 |
| v1 汇总 439，但漏 M1911，实际分类 440 | 资产统计错误 | 修正导入与对账测试 |
| 外部 v1 更新版、v3 adjusted、仓库 v1、runtime 相互漂移 | 无法确定哪份是设计真值 | 逐字段裁决后冻结权威源 |
| runtime 配方/Loot 被 .minecraft ignore | 普通 Git 不直接跟踪客户端 | 已完成本地基线 ZIP；Loot 另有 365 文件回滚包，配方进入规范项目但仍待内容裁决 |
| 配方日志把输入数写成 registered from | 容易误报 960 已注册 | 改为五类精确计数 |
| Fungal_Infected_Creep 源/部署哈希不同 | 双向同步风险 | 决定胜出版本并单向部署 |
| GreenField 两个物理副本高度相似 | 占空间但误删风险高 | 外部备份与恢复演练后再判定 |
| 建筑存档旧“1.1 GB”与逻辑大小约 3.65 GB 不一致 | 备份容量估算失真 | 已统一为文件逻辑大小约 3.65 GB |
| v2 是审计/策划视图，不是权威源 | 若直接手改会再次产生多真源 | 后续由资产管理器从规范 JSON/CSV 生成；inspect 不入库 |

## 12. P0 与 P1 闸门

### P0：恢复、止血与最小闭环

1. 更新项目台账，使快照、分支、提交和文档状态与事实一致。
2. 审查剩余未跟踪文件；CNPC、MiroFish fixture、v2 和中间物分批决定。
3. 已在改数据前私有保存当前 runtime KubeJS，并记录 ZIP 与 SHA-256。
4. 对账仓库 v1、外部 v1 更新版、crafting_tables_v3_adjusted.xlsx、v2 与 runtime；修正 M1911 和所有计数差异。
5. 已修复 4 个 tier1 与连锁引用，建立 registry/namespace/hash validator 并封装 JAR；待实机回执。
6. 完成 Picasso DD marker 与 MiroFish Spike Zero 两个人工闸门。
7. 冻结统一 work-order 契约，实现 Picasso 最小 mark dispatcher/receipt。
8. 启动 Chess 纯 Java core/event log 与 MiroFish 最小事实摄取/CNPC compiler，完成三层 fixture 薄切片。

### P1：稳定数据面与可用工具

1. 资产管理器 MVP 已完成：游戏内标注、导入、规范化、校验、搜索、问题视图、一层图、导出与 diff；待实机回流真实名称/变体。
2. 配方/Loot 单向生成链和部署回执已完成第一轮；配方内容审查通过后再切换生成候选。
3. CNPC active/archive/experimental 分区、manifest 和 source/deploy hash 检查。
4. Picasso 在 mark 稳定后拉取 segmentation/registry，再选择 Room 或 Rail 的最有价值切片。
5. Chess event log 稳定后接最小 NeoForge shell/headless 路径。
6. MiroFish compiler 稳定后增加 MCP 管理面、连续性图和更多内容类型。
7. 世界角色登记、私有备份和恢复演练；之后才评估 GreenField 去重。

## 13. 安全整理顺序

1. 保持现有路径不动，先完成本索引、manifest、哈希和角色标记。
2. 对未跟踪/忽略内容逐批审查；不执行整目录 add、move 或格式化。
3. 将当前实际 runtime 配方/Loot复制到受控审查区或私有备份，保留原件。
4. 冻结权威源与字段胜出规则，再实现 validator 和 generator。
5. 生成到临时输出，检查 diff 后部署到测试客户端；部署物仍不作为权威源。
6. 保存日志、截图和 receipt，验收后再更新项目台账。
7. CNPC 整理采用“复制到新分区 → 校验哈希 → 切换引用 → 实机验证”的方式；旧路径暂不删除。
8. 地图/世界先做外部备份和恢复演练；只有两个独立恢复点均有效后才讨论重复副本。
9. 上游/vendor 拆分、历史压缩包归档和缓存清理最后进行。
10. 任何删除、覆盖、批量移动仍需单独授权；本页不是删除授权。
