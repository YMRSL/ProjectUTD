# Picasso 项目状态与依赖路线图

> 状态快照：2026-07-10  
> 本文件记录“当前源码真实可运行的能力”和下一阶段交付闸门。规范、示例
> JSON、未来目录或工具名本身不算实现；只有源码、注册面和测试共同落地才
> 标为完成。

## 1. 状态口径

| 标记 | 含义 |
|---|---|
| ✅ | 已有源码实现，并有相称的自动测试或临时世界集成验证 |
| 🟡 | 部分完成；已有可用子集，但阶段的 Done 标准尚未满足 |
| 🚧 | 规格或设计已存在，主体实现尚未开始 |
| ⬜ | 尚未形成实现；可能连专项规格也未冻结 |
| ⛔ | 被外部验证、前置阶段或能力闸门阻塞 |

以下内容一律不能单独作为“已实现”证据：

- `docs/` 中的 API、类或目录草图；
- `data/brushes/`、`data/room_templates/`、
  `structure_fingerprints.json` 中没有运行时消费者的示例数据；
- 临时世界能保存某个非原版 ID，但尚未在真实 Minecraft + 对应模组中验证；
- Prompt 能描述一个工作流，但没有对应的读取、写入或注册工具；
- Fragment 能描述静态体素，并不等于能构造可运行的 Create 网络。

## 2. 当前基线

### 2.1 已确认基线

- 自动测试基线：**407 passed, 1 skipped, 14 subtests**。
- MCP 工具基线：**22 个已注册工具**，所有同步工具调用由
  `SingleFlightFastMCP` 串行化。
- 工具分组：
  - 世界：`set_world`、`close_world`、`read_region`；
  - 分析证据：`analyze_region`、`inspect_volume`；
  - Catalog：`query_catalog`；
  - Style：`list_passes`、`preview_pass`、`apply_pass`、`create_pass`；
  - Bundle：`list_bundles`、`apply_bundle`；
  - Journal：`list_journal_entries`、`inspect_journal_entry`、
    `revert_last_apply`；
  - 玩家活动：`query_player_activity`、`get_activity_site`；
  - 定义创作：`list_fragments`、`create_fragment`、`create_bundle`；
  - NPC：`place_npc_marker`；
  - 诊断：`describe_capabilities`。
- MCP 另提供服务器级 instructions 和 `interpret_world_structure` Prompt；
  Prompt 是 Agent 指导面，不是额外工具或安全边界。
- 已实现底座包括：Java 原生/稀疏 palette 读取、水平 halo 和垂直上下文、
  trusted write envelope、统一 WriteChoke、批写回滚、Journal/Revert、玩家活动
  保护、Fragment/Pattern 原子组、确定性噪声/位置哈希、局部楼梯与楼层候选、
  有界体素证据。

### 2.2 尚未实现的主要系统

- Phase 1.5 的真实游戏内 DD 渲染/交互验收；
- 正式 segmentation、Structure Registry、稳定结构身份与 structure-mode apply；
- Agent candidate/evidence run、审阅确认和规则晋升闭环；
- Brush/Room loader、engine、interior graph 和工具；
- `learn_style`、`extract_block_clusters`；
- Work-order 队列消费者、回执和 window clock；
- 静态机械 SR0 精确版本 no-BE/no-NBT 能力清单及 SR1 shipped ruins；
- 功能性 Create/机械模板、游戏原生 executor、commissioning 和实例 registry；
- Create 轨道的 JAR/target-state 证据与 vanilla source graph 纯内存核心已实现；
  BE/Bezier/SavedData 读取、真实地图 bounded 接线、RailTemplate preview、原生
  replacement 与验收仍未实现。独立线路拆除不在 R0-R3 首期；R2 只会原子移除
  已批准的 source rail component。

## 3. 总依赖图

```text
P0 核心底座 ✅
├─ P1 Phase 1.5 DD 游戏内验证 🟡
│  └─ SR1 中使用 DD 的静态机械废墟内容
├─ P2 Agent evidence 闭环 🟡
│  ├─ A1 candidate/evidence run
│  └─ P3 Phase 0 / S1-S4 segmentation + registry
│     ├─ A2 审阅写回 ──→ A3 规则晋升
│     ├─ P4 R2-R5 Room/Brush
│     ├─ P6 style learning 完整闭环
│     ├─ P6 work orders 的 structure/room 类订单
│     └─ Later M0+ 的自动选址、楼层电梯和 Room 集成
├─ P4 R1 Brush（仅 region scope，可在 S1 前并行）
├─ P5 静态机械：SR0 精确版本 stateless gate ──→ SR1 Fragment/StaticPrefab
├─ Create 轨道高优先级独立线：
│  R0 state/graph core ✅ / BE+SavedData+real-fixture 🟡 ──→ R1 preview ──→ R2 executor ──→ R3 acceptance
└─ Later M0+ 功能机械（生产线/农场/电梯；不阻塞 SR 或 Rail）🚧

P3 + P4 + Phase 8 Journal ✅ ──→ P6 完整 work-order dispatcher
```

P0-P6 是本文件的集成交付闸门；它不重命名既有文档中的 base Phase 1-8、
Agent A0-A3、segmentation Phase 0/S1-S4、Room R1-R5、静态 SR0-SR1 或轨道
R0-R3。下文凡写 R1，都会标明是 `Room R1` 还是 `Rail R1`。

## 4. P0-P6 集成阶段

### P0 — 安全读写与现有工具基线 ✅

**依赖：** 无；这是所有后续工作的根。

**已完成**

- Base Phase 1、2、3、3.5、4、Phase 5 初版、7、8 的当前实现；
- 统一 WriteChoke、modded ID fail-closed、block-entity 保护、Journal 和冲突安全
  revert；
- 22 工具、407-test 基线、wheel smoke 和临时世界桥接测试；
- A0 的局部语义与有界体素读取底座。

**进行中**

- Phase 5 仍需视觉验收和少量回归加固；这是维护项，不阻止只读后续阶段。

**待办**

- 为 `preview == apply diff`、四向 shipped content、bundle 顺序继续补高风险回归；
- 每次升级 `amulet-core`、`mcp` 或 Python 版本时重跑临时世界集成套件。

**完成标准**

- 维持当前 22 工具契约及全套测试通过；
- 所有写路径都经过 trusted envelope、WriteChoke 和 Journal；
- 无世界句柄、无安全策略或未知 mod ID 时保持 fail-closed。

**阻塞项**

- 无硬阻塞；含 DD 的写入受 P1，SR/Rail/Later M 分别受自己的能力闸门；所有真实
  世界写入还受停服、dry-run、Journal 与玩家保护操作规程约束。

**建议下一动作**

- 将当前基线作为 CI 必过门槛，后续阶段不得通过放宽安全检查换取功能。

### P1 — Phase 1.5：DD 模组方块真实游戏验收 🟡 ⛔

**依赖：** P0；需要 Minecraft 1.21.1、目标 DD 模组版本和一次性物理世界副本。

**已完成**

- 当前 Phase 1.5 标记已写入一次性测试世界：
  - `(5, -60, 4)`：`doomsday_decoration:acrate`；
  - `(7, -60, 4)`：`doomsday_decoration:accessorybox_1`，`facing=east`；
  - `(9, -60, 4)`：`minecraft:diamond_block` 原版对照；
- 三个标记均已通过 Amulet save → close → reopen 核验，ID 和已检查的方向属性未在
  Amulet 往返中丢失；这仍不是 Minecraft + DD 模组内的渲染或交互验收；
- 已记录兼容四元组：Minecraft/Forge 1.21.1（NeoForge 21.1.233 profile）、
  DD `1.1.3-neoforge-1.21.1`、Amulet Core `1.9.41`、`DataVersion=3955`；
- 原版属性、叶片 `persistent` 注入和既有 block-entity 检测已有测试；
- `PICASSO_MODDED_WRITE_VERIFIED=false` 的 legacy 写入闸门已实现；即使本次通过，它也
  只代表本整合包固定 DD `1.1.3`/catalog 组合，不授权 Create 或未来任意 namespace；
- 失败时的 structure-block staging / external stamper contingency 已有设计。

**进行中**

- 代码侧 spike 已完成，等待真实游戏端人工验收。

**待办**

- 用真实客户端和目标 DD 模组打开已经写入三个 marker 的一次性世界，验证渲染、
  朝向、碰撞及必要交互；不再重复放置 marker；
- 保存客户端截图/目测结论及游戏日志；版本四元组已固化，不再凭路径猜测；
- 通过后才设置闸门；失败则按实际故障选择 contingency，而不是猜测。

**完成标准**

- 三个对照方块均在游戏内通过，或已有可复现失败分析及已验证 fallback；
- 结果写入实现顺序/运行问题记录；
- 仅对已验证 namespace/版本开放相应能力。

**阻塞项**

- 这是人工、真实游戏和模组环境验证，单靠 pytest 不能关闭；
- 现有全局布尔闸门只能代表上述固定 DD 版本，绝不能顺带授权 Create；后续仍需
  namespace+version scoped capability matrix。

**建议下一动作**

- 优先执行一次 15-30 分钟的受控游戏内检查；在完成前不要继续扩张 DD 写入内容。

### P2 — Agent Evidence 与审阅闭环 🟡

**依赖：** P0。A2 依赖 P3/S4 Registry；A3 还依赖已确认样本和规则测试。

**已完成**

- **A0**：`analyze_region`、`inspect_volume`、局部 stair/storey candidates、
  bounded RLE、MCP instructions、`interpret_world_structure` Prompt 及合成测试；
- 明确 `local_semantics.scope=candidate_only`，不会把局部平面称为权威楼层；
- 受控 fixture 已覆盖；Mosslorn 只读挑战的路径安全边界已有记录。

**进行中**

- A0 可实际使用；A1-A3 尚未实现。

**待办**

- **A1**：run ID、candidate/evidence ID、分页、关系、矛盾字段及缓存；
- 在独立物理副本之外，对 Mosslorn 只做有界只读实景验证；
- **A2**：Agent/human 接受、修正和 `authored` 写回；
- **A3**：多例确认、规则提案、反例测试和人工批准晋升。

**完成标准**

- 一个候选可从摘要追到有界证据、矛盾项和完整分页；
- 同一 run 的引用稳定且有资源上限；
- 人工确认写入 `authored`，重检测不会覆盖；
- 新规则只有在多例+反例测试通过后才进入确定性 core。

**阻塞项**

- A2/A3 需要 P3 Registry 与稳定结构身份；
- Mosslorn 现有 `picasso_mosslorn_test` 是 junction，不是可写测试副本。

**建议下一动作**

- 实现纯只读 A1 run/evidence 层，并用合成 fixture + 一个 Mosslorn 有界区域验证；
  不要直接跳到 Agent 自动写入结构真相。

### P3 — Segmentation、Structure Registry 与结构作用域 🚧

**依赖：** P0；S3 依赖 S1；S4 汇总 Phase 0/S1/S2/S3。开始前需加入 `scipy`。

| 子阶段 | 当前状态 | 交付内容 |
|---|---|---|
| Phase 0 | 🚧 | signal arrays、`StructureCandidate/Structure` 模型 |
| S1 | 🚧 | flood fill、封闭空间、tier-2 interior/exterior |
| S2 | 🚧 | flat/linear/elevated 检测，含原版 rail component |
| S3 | 🚧 | 地下、隧道、水体/船/港口检测 |
| S4 | 🚧 | assembler、fingerprint、Registry、稳定 ID、8 工具、bundle structure mode |

**已完成**

- 规范、实现阶段、fingerprint 数据、局部楼层候选和 A0 证据底座；
- 这些都不等于正式 segmentation。

**进行中**

- 无主体代码在运行；`src/picasso/core/segmentation/`、Structure 模型和工具仍缺失。

**待办**

- 按 Phase 0 → S1/S2 → S3 → S4 实现；
- Registry 使用 read-merge-write + atomic replace，保持 `detected/authored` 所有权；
- 实现 partial 边界、IoU/containment 身份、stale 规则和 override 保留；
- 接通 structure-scoped pass/bundle 与 player attribution。

**完成标准**

- fixture 中 3 building + road + rail 至少得到 5 个结构；
- 重检测保留 ID 与人工 override；
- `list/get/annotate/detect` 和 candidate/evidence 工具全部注册且测试通过；
- `apply_pass_to_structure` 和无坐标 bundle structure mode 严格裁剪边界。

**阻塞项**

- `scipy` 尚未进入依赖；
- 复杂地图 ground truth、边界 partial 和多层空间是主要质量风险；
- fingerprint JSON 目前没有运行时消费者。

**建议下一动作**

- 先交付 Phase 0 的数组/坐标不变量和模型测试；S1 与 S2 可随后并行，禁止先写 S4
  Registry 空壳来伪装进度。

### P4 — Brush、Room 与 Interior Graph 🚧

**依赖：** Room R1 仅依赖 P0；Room R2/R3 依赖 P3/S1；Room R4 依赖 Room R3；
Room R5 依赖 Room R4 和
已完成的 Journal，structure scope 还依赖 P3/S4。

| 子阶段 | 当前状态 | 真实可用范围/交付 |
|---|---|---|
| Room R1 | 🚧 | Brush model/compiler/tools；仅 region scope 可用 |
| Room R2 | 🚧 | S1 space kinds 接入 Brush |
| Room R3 | 🚧 | Room template、palette、interior graph、routes |
| Room R4 | 🚧 | carve-mode add/remove/connect/seal |
| Room R5 | 🚧 | annex、variants、telemetry、recondition/refurnish |

**已完成**

- 规范已评审；FragmentEngine 已有 `initial_placed_anchors`，WriteChoke/Journal 前置已完成；
- `debris_scatter.json`、`bunk_room.json`、palette compatibility 是参考数据。

**进行中**

- 无 Brush/Room model、loader、engine 或 MCP 工具；参考 JSON 不能执行。

**待办**

- 优先 Room R1 region-only 垂直切片；
- S1 后接入 space kinds，再实现模板、图、房间 envelope 和 layer-tagged journal；
- 为 pre-validation、可达性、双层墙、annex 与 selective revert 写反例测试。

**完成标准**

- Room R1：同 seed Brush preview/apply 一致，跨 kind min-spacing 正确；
- Room R3：能构建可复核的 interior graph 和稳定路线；
- Room R4/R5：房间操作 dry-run 默认开启，无半个 envelope，layer selective revert 可验证；
- 所有新工具注册、列入 diagnostics 并受同一安全链约束。

**阻塞项**

- Room R2-R5 的空间理解依赖 S1/S4；
- DD palette/placement 仍受 P1；
- 当前 BlockState/Journal 不支持 block-entity NBT，模板中的 chest/barrel 不能默认视为功能性容器。

**建议下一动作**

- 可立即实现 Room R1 的严格模型、loader 和 region-only dry-run/apply；与此同时推进 P3/S1，
  不要提前承诺 structure/room scope。

### P5 — 静态机械废墟：SR0 → SR1 🚧

**依赖：** P0。SR0 依赖目标整合包的精确模组版本/JAR 证据；SR1 依赖 SR0。
使用 DD props 还依赖 P1。静态能力不继承 DD、Rail 或 Later M 的任何授权，反向也一样。

#### SR0 — 精确版本 no-BE/no-NBT 能力清单 🚧

**已完成**

- 已冻结严格边界：只有精确版本 inventory 明确标记
  `block_entity=false` 且 `requires_nbt=false` 的 ID 才可进入静态内容；
- Fragment 已支持普通 blockstate、四向 offset、`facing/axis` 旋转、实例原子组、
  WriteChoke 和 Journal。

**进行中**

- Create JAR 的轨道专项审计已提供 R0 证据，但这不等于完整 SR0 stateless allowlist；
- 尚无可被 loader/preview/apply 强制执行的精确版本静态能力清单。

**待办**

- 从实际安装版本提取 block registry、block-entity 类和 NBT 需求；
- 对任一合法状态可能产生 BE 的 ID 整体拒绝，不做脆弱的 state-specific 例外；
- 在 authoring、load、preview、apply 四层 fail-closed，并为缺元数据、版本漂移、
  混入 BE/NBT 写拒绝测试。

**完成标准**

- 清单绑定精确 namespace、模组版本和证据来源；
- 缺失/未知/版本不匹配一律拒绝；
- 临时世界 save/reopen 与游戏内检查证明确认入选 ID 仍是无 BE、无 NBT、无运行语义。

**阻塞项**

- 完整精确版本 inventory 和静态候选的游戏内核验尚未完成；
- DD P1、Rail R0 或某个普通方块成功都不能替代 SR0。

**建议下一动作**

- 先生成机器可读的 SR0 capability inventory 和拒绝测试；在清单落地前不要把
  看起来“只是装饰”的 Create ID 放进 Fragment。

#### SR1 — Fragment 优先，可选 StaticPrefab 🚧

**已完成**

- 已冻结 “Fragment first” 原则：小型、有机、表面锚定的静态废墟继续使用现有 Fragment；
- `StaticPrefab` 只为必须精确锚点/yaw、较大固定布局的静态组合保留，仍编译成同一
  `RegionData` diff 和一个原子组，不获得 ports、commissioning 或实例 registry。

**进行中**

- 没有 shipped machinery/factory/elevator/farm ruin Fragment、StaticPrefab 或专项 bundle。

**待办**

- 用 SR0 允许的方块制作断裂输送线、停摆升降机构、废弃农机、工业控制台等 ruins；
- 每个模板明确 `functional=false`、允许 transform、clearance/support 和可破坏性；
- 补四向、边界、物理支撑、原子失败、preview/apply/revert、save/reopen 与视觉 fixture；
- 只有当 Fragment 无法表达精确大型布局时才实现薄 `StaticPrefab`，不建第二套安全链。

**完成标准**

- 至少三个不同工业叙事模板在允许 transform 下正确；
- 所有成员均来自 SR0，且没有 BE、NBT、库存、移动实体或动力网络；
- preview/apply 完全一致，整组拒绝不留半个模型，Journal revert 可恢复；
- 游戏内视觉确认邻居更新不会让废墟启动或自毁，并明确标注“装饰性、不可运行”。

**阻塞项**

- SR0 尚未完成；DD 内容另受 P1；
- 不能把静态 props 当成轨道或 Later M 功能机械支持。

**建议下一动作**

- SR0 关闭后先交付一个小型 Fragment 垂直切片；只有出现已证明的精确布局缺口时，
  再批准 `StaticPrefab`。

### P6 — Style Learning 与 Work Orders 运营闭环 🟡 / 🚧

**依赖：** Style Learning 的 cluster extractor 依赖 P3 的 segmentation 基础；完整
Work Orders 依赖 Journal（已完成）、P3/S4、P4，以及各 order kind 的实际 executor。

#### P6-L — Style Learning 🟡

**已完成**

- `list_fragments`、`create_fragment`、`create_bundle` 及安全的定义写入/冲突语义；
- `inspect_volume` 可提供有界参考证据。

**进行中**

- “创作输出端”可用；真正的学习/聚类输入端尚未实现。

**待办**

- `StyleProfile`、`style_learner.py`、`learn_style`；
- `cluster_extractor.py`、`extract_block_clusters`；
- matched fragments、统计诚信、资源上限和端到端人工 walkthrough；
- 不得复活已退休的 `apply_style_profile`。

**完成标准**

- styled fixture 返回非零统计和至少一个 matched fragment；
- 5+ 重复装饰能稳定聚类并给出正确 occurrence count；
- Agent 可从 reference save 生成、预览并人工接受一个工作 bundle。

**阻塞项**

- cluster extractor 与 P3 的 segmentation 包共享基础；
- 高质量规则晋升还依赖 P2/A3 的确认闭环。

**建议下一动作**

- P3 Phase 0 坐标/数组基础落地后，先实现纯只读 cluster extractor，再接 `learn_style`。

#### P6-W — Work Orders 🚧

**已完成**

- Work-order/receipt/window clock 规范；
- Journal、player protection、`on_behalf_of` 可承载的安全底座和玩家活动读取已存在。

**进行中**

- 没有 `picasso_workorders/` dispatcher、schema loader、two-phase consume、receipt 或工具。

**待办**

- `list_work_orders`、`process_work_orders(dry_run=true)`；
- pending → in_progress → applied/failed 的 crash-safe 状态机；
- dependency、expiry、dedup、partial/rejected 回执和 window log；
- 把 construct/restyle/demolish 等种类接到真实存在的 Room/Structure/Mechanical executor；
- 保持“无 raw `write_blocks` order”。

**完成标准**

- 同一 order ID 幂等消费，崩溃后的 in-progress 会隔离而不自动重放；
- 依赖、过期、失败、partial 回执全有测试；
- 所有世界写仍通过既有 choke/journal/protection；
- 至少一条 construct 或 restyle 端到端订单在一次性世界生成可审计回执。

**阻塞项**

- 大多数语义 order 依赖尚未实现的 P3/P4；
- wargame contract 仍是前向接口，不能当作 dispatcher 已存在；
- 功能机械 order 还必须等待 Later M0+；轨道 replacement 必须等待 Rail R2/R3，
  两者都不能降级成 raw block 写入。

**建议下一动作**

- 可以先实现纯文件、纯 dry-run 的 schema/queue 状态机测试；实际 dispatch 等 P3/P4 的
  一个垂直切片完成后再接入。

## 5. Later M0+：功能性生产线、农场与电梯 🚧

> 冻结优先级：功能性机械是 **Later**。它不阻塞 SR0/SR1 静态废墟，也不阻塞
> 高优先级 Rail R0-R3；三个 track 不相互继承能力。当前没有
> `MechanicalTemplate` loader、game-native executor、commissioning probe、
> `MechanicalInstance` registry 或 MCP 工具。

**依赖：** P0 和独立的精确 Create/附属模组版本能力矩阵。真正写入需要游戏原生
schematic/plugin executor；自动选址依赖 P3/S4，电梯还依赖 P4 的 storey、shaft、
opening 与 vertical circulation。

**已完成**

- `MechanicalTemplate/MechanicalInstance`、ports、artifact digest、commissioning、
  instance-scoped removal 的职责已在冻结规范中保留；
- 已明确 Amulet blockstate 写入、DD P1、SR 静态验证或 Rail executor 都不能授权任意
  kinetic machine。

**进行中**

- 无功能机械实现；生产线、自动农场、电梯全部保持 Later。

**待办**

- Later M0+ 首先定义独立 capability matrix、game-native request/receipt、模板/实例
  registry、commissioning 和 conflict-safe native disassembly；
- 之后才分别做生产线 item/kinetic probe、农场 moving-envelope/inventory-output probe、
  电梯 contacts/轿厢/楼层到达 probe；
- 自动 site search 和既有机械 adoption 等 P3/P4 具备后再做。

**完成标准**

- 每个 shipped 模板绑定精确模组版本、artifact digest 和已验证 executor capability；
- place → commission → save/reload → recommission → conflict-safe remove 全生命周期通过；
- 玩家库存、配置和第三方修改不会被静默吞掉；失败实例进入可诊断状态，不谎报成功/删除；
- 电梯只有在正式 storey/Room graph fixture 中完成多楼层到达和安全 disassembly 才算完成。

**阻塞项**

- 尚无 game-native functional-machine executor 与真实集成 fixture；
- BlockState/RegionData/Journal 不持有 Create block-entity NBT 或 contraption entity；
- 自动选址和电梯依赖尚未实现的 P3/P4。

**建议下一动作**

- 当前不启动功能机械写入。先完成 SR0/SR1 和 Rail R0/R1；Later M0+ 只保留规范与
  测试用例清单，避免抢占当前高价值只读/静态主线。

## 6. R0-R3：Create 轨道高优先级专项

> `RailTemplate` 与 `RailNetwork` 规范已经冻结在
> `docs/mechanical_structures.md`。轨道是独立高优先级例外：不等待 Later M0+ 的
> 功能生产线，也不继承 DD P1 或 SR0/SR1 静态能力。反过来，轨道通过也不授权
> 任意功能机械。P3/S2 的原版 rail detector 不能替代此专项。

### R0 — Vanilla source graph 与 Create target evidence 🟡

**依赖：** P0；source reader 只依赖原版 rail blockstate。target evidence 绑定精确
Create/Railways 版本。整个阶段只读，不依赖功能机械 executor。

**已完成**

- 首期 source 已冻结为一个经审阅的 vanilla-rail connected component；现有
  Create/Railways 网络接管属于后续范围；
- JAR 审计确认 `create:track` 的 `shape/turn/waterlogged`、`turn=true` 的
  TrackBlockEntity/Bezier `Connections`，以及全局 `create_tracks.dat`；
- `core/create_track_semantics.py` 已实现 target-side 状态分类：Create + Railways 52 个
  required ID、全部 shape 类、BE expected/present 一致性、非法属性诊断、halo 排除和
  确定排序；`tracks:track_mount` 与 required:false 兼容材质明确排除；
- target semantics 专测 38 项通过。它明确返回
  `curve_payload_status=unavailable`，不会把 BE 坐标当作已读取曲线 NBT；
- `core/vanilla_rail_graph.py` 已实现 source-side doubled-port 图：四种原版轨道、
  10 个 rail shapes、四向坡道高端、components/terminals、完整 source properties、
  halo/context 与 chunk/Y partial 证据；
- 非互惠邻轨、潜在叠层/T 形、多个 components、世界高度越界、waterlogged 与
  powered/detector/activator 语义均 fail-closed；source graph 专测 45 项通过；
- 轨道不是 Fragment/StaticPrefab；任何 target segment 都禁止原始 Amulet 写入。

**进行中**

- 两个 core 模块尚未接入 bounded MCP/read workflow，也未在正式地图 rail component 上
  做只读对照；
- target 侧仍只有 BE 坐标，没有 Bezier NBT 与 `create_tracks.dat` 证据；
- 尚未有 native executor，也尚未进行游戏内 replacement 验收。

**待办**

- 把 vanilla graph 与 Create target semantics 接入一个只读 bounded evidence/preview
  内部服务，保留 `complete/truncated/omissions`；
- 在正式地图物理副本上选择一个小 rail component 只读对照 terminals/坡道/拐角；
- 为 R1 增加来源 digest 与 RailTemplate plan 模型；
- 为 target post-check 增加只读 BE/Bezier 与 `create_tracks.dat` 证据通道；这不阻塞
  source graph/R1 纯规划，但会阻塞 R2/R3 target 完整性验收。

**完成标准**

- 同一 bounded vanilla route 可稳定重建 source component、terminal、直线/坡道/拐角
  分类及完整性诊断；
- 特殊 rail 或缺失边界证据不会被静默降级为普通 rail；
- Create/Railways target 状态被精确分类，缺 BE/payload 证据会返回 blocking diagnostic；
- 全阶段无世界写。

**阻塞项**

- 纯内存 source graph 无硬阻塞且已完成；真实地图对照需要选定小范围并保持只读；
- target 侧完整验证仍需新的 BE/NBT + SavedData 只读通道和一次性 modded fixture。

**建议下一动作**

- 将两个已测 core 接成内部 bounded evidence service，并在正式地图副本上做一次小范围
  只读 source graph 对照；同时可启动 R1 的纯 schema/digest 规划，不等待 Later M0+。

### R1 — RailTemplate 拟合与只读 preview 🚧

**依赖：** R0。`RailTemplate`/`RailNetwork` schema 已冻结；不依赖功能生产线 M 阶段。

**已完成**

- 规范已定义直线、ascending、curve segment、legal transforms、endpoint/tangent/grade、
  source digest、template version、preview digest 和网络 receipt 边界。

**进行中**

- 无 connection solver、support/clearance validator 或 preview 工具实现。

**待办**

- 实现 template registry/validation、source graph 拟合、terminal/connectivity 保持、
  foundation/support/clearance/protection/liquid/chunk-boundary 检查；
- 实现 `list_rail_templates` 与 `preview_rail_replacement`；
- 对 straight/slope/curve 及不可表示形状写 adversarial fixtures；
- preview 只生成稳定 digest 和 blocking diagnostics，不写世界。

**完成标准**

- 同一 source digest + options 得到相同 RailNetwork plan/digest；
- terminal 与连通性保持，任何 unsupported/ambiguous shape 阻断授权；
- preview 不暗示 native executor 已存在，且不能被 apply 到不同 source state。

**阻塞项**

- R0 source graph 尚未完成；
- 仍缺已验证的 straight/slope/curve RailTemplate artifact 集。

**建议下一动作**

- R0 graph contract 稳定后立即做纯内存 solver + preview digest，继续保持只读。

### R2 — Create-native rail executor 与 rollback artifact 🚧

**依赖：** R1；精确 Create/Railways 版本和停服维护窗口。它是轨道自己的 executor，
不等待任何 Later M0+ 功能生产线里程碑。

**已完成**

- 写入边界已冻结：必须通过 Create/Minecraft 原生 API，先捕获 native rollback artifact，
  再移除经批准 source rails、放置并触发邻居更新；原始 Amulet 写入禁止。
- lifecycle 已冻结：preview plan 完全 ephemeral；executor 捕获 rollback 后、第一次世界
  mutation 前，RailNetwork 必须以 `pending` 耐久写入，随后只能进入 `applying`、
  `awaiting_acceptance`、`accepted`、`rolled_back` 或 `failed_needs_recovery`。

**进行中**

- 无 executor、signed/digested receipt、native rollback 或公共 apply 工具。

**待办**

- 实现 exact-preview-digest request、executor 身份/版本校验、straight/slope/curve placement、
  rollback artifact、耐久状态机、失败恢复和 reload verification；
- R2/R3 只允许内部 acceptance harness 在一次性世界调用 executor；
- 公共 `apply_rail_replacement` 保持不注册/不可用，R3 通过后才开放；开放后仍默认
  dry-run，并只能执行仍匹配当前 source digest 的 R1 plan。

**完成标准**

- 一次性世界中 straight/slope/curve 均能按计划原生放置；
- 任一步失败都恢复 source route，并产生完整诊断；
- mutation 前已有可恢复的 `pending` 记录；不完整恢复进入
  `failed_needs_recovery` 并阻断后续 rail write；
- receipt 绑定 source、templates、Create/Railways 版本、变换、artifact 和结果 digest。

**阻塞项**

- game-native rail API bridge 与可靠 rollback artifact 尚未实现；
- 当前 block-state Journal 不能声称完整恢复 Create track BE/NBT。

**建议下一动作**

- R1 完成前只设计 executor contract/fake tests；不要用 Amulet 直写先“临时跑通”。

### R3 — Reload、图复核与双向通行验收 🚧

**依赖：** R2 和一次性真实 modded 世界。

**已完成**

- acceptance 条件已冻结；首期明确排除未规划的 stations、signals、schedules、trains、
  switches/crossings 和自动全图路线设计。

**进行中**

- 无游戏内 replacement acceptance；只有内部 R2/R3 harness 被设计为可执行，公共 apply
  尚未启用。

**待办**

- 保存/重载后重新读取 RailNetwork 图；
- 进行双向列车 traversal、terminal/curve/slope 连续性检查、source 清理和无额外 BE 检查；
- 验证失败的 native rollback 与 receipt/registry 状态；通过后才注册/启用公共 apply。

**完成标准**

- straight/slope/curve templates 分别通过 schema、图、重载和双向通行验收；
- replacement 后图与 preview digest/receipt 一致，没有断轨、悬挂连接或计划外数据；
- 失败可恢复原路线；只有 R3 全部通过后公共 MCP
  `apply_rail_replacement` 才可用。

**阻塞项**

- R2 尚未实现；需要真实 Create + Railways 环境和可安全回滚的一次性世界。

**建议下一动作**

- 预先准备最小直线、坡道、曲线三类 source fixtures 和验收脚本；不扩张到站点/信号或
  自动路线规划。

**后续而非 R0-R3 首期：** 自动路线、桥梁/隧道、junction、station、signal、schedule 和
train assembly 等 P3/P4 之后另立阶段，不能塞进 R0-R3 来虚增首期范围。

## 7. 建议执行顺序

1. **立即关闭 P1 人工闸门**：真实客户端验证 DD；这是时间短、信息价值最高的阻塞解除动作。
2. **Rail R0/R1 进入早期只读主线**：把已完成的 state/source graph core 接入 bounded
   evidence，补 BE/SavedData target evidence，并实现 RailTemplate solver/preview digest；
   明确不等待功能生产线。
3. **并行做 P2/A1 + P3/Phase 0**：完成通用证据引用和 segmentation 数组/坐标基础。
4. **静态内容线先做 SR0**：精确版本 no-BE/no-NBT inventory 和 fail-closed tests；
   SR0 关闭后才进入 SR1 Fragment，确有必要才加 StaticPrefab。
5. **并行做 Room R1**：只承诺 region scope；复用稳定的 Fragment/WriteChoke。
6. Phase 0 稳定后并行 S1/S2；S4 Registry 之后再推进 A2、Room structure scope、
   style-learning 闭环和 work-order 真实 dispatch。
7. Rail R1 preview 通过后推进 R2 native executor，再以 R3 reload + graph + 双向通行
   验收决定是否启用 apply；全程不借用 DD/SR/Later M 授权。
8. **Later M0+ 保持后置**：功能生产线、农场、电梯不阻塞 SR 或 Rail；电梯始终等待
   storey + Room graph，自动 site search 等 P3/P4。

任何阶段若新增工具，都必须同步更新：工具注册测试、`describe_capabilities`、工具文档、
错误码、dry-run/Journal/保护测试以及本文件的 22-tool 基线说明。
