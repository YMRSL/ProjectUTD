# 誓死坚守｜项目总控台

> 项目级唯一运营台账。最后更新：2026-07-11。
>
> 本页只记录进度、优先级、阻塞和验收；模块设计仍以各自的 `ARCHITECTURE.md`、`HANDOFF.md` 和修订日志为准。

## 1. 两周冲刺目标

暂按 2026-07-11 至 2026-07-24（14 天）管理。

本轮的产能假设是：**Codex 承担主要实现、自动测试、文档和修复；用户承担必须在 Minecraft 客户端完成的实机验证与结果反馈。** 因此关键路径不是纯编码工时，而是“交付测试包 → 用户验证 → 回传证据 → Codex 修复 → 复测”的周转速度。

下面两个结果是本轮必须先跨过的**最低交付线和第一道里程碑，不是两周产出的上限**：

1. **Social Will 最小真实闭环**：Chess 事实/fixture → MiroFish 生成带事实引用的 CNPC 内容与 `nar_* mark` 订单 → Picasso 安全执行 marker、写 Journal/receipt → 测试世界可见并可回滚。
2. **本体内容数据止血**：物品、配方、Loot 建立可追踪的权威源和 `validate → generate → diff → deploy` 流程，修掉当前会造成 Loot 表连锁加载失败的缺失引用。

冲刺结束的完成定义：

- 当前成果有可恢复的本地与远端 Git 快照。
- Picasso 现有测试保持全绿，并补完 DD marker 实机闸门。
- MiroFish Spike Zero 关闭；最小事实摄取、CNPC 包编译和订单输出可运行。
- Chess 至少可稳定产出规范事件日志；优先完成纯 Java 核心与最小 headless 路径，不承诺完整地图 UI。
- 三层薄切片在测试世界走通，含重复订单、失败回执、权限与回滚验收。
- P0 配方/Loot 校验不再包含已移除模组的未处理引用；生成结果可对账。

明确延后：完整 Picasso segmentation/Room/Rail、Chess P4–P9 全量系统、生产规模 OASIS/Graphiti、完整战略地图、资产管理器重 UI、全项目美术重做。

其中“延后”表示不允许它们抢在第一道里程碑之前；若测试闸门提前关闭且前置验收稳定，则按依赖顺序继续拉取 P1/P2，而不是停工等待。

### 交付与测试节奏

- Codex 可以并行推进多个互不写同一区域的实现任务，并持续跑自动测试。
- 用户侧同时只保留一个主测试包，避免来回切世界、切版本和混淆日志。
- 每个测试包必须包含：目标、前置备份、预计耗时、精确步骤、预期结果、失败时要保留的日志/截图/文件、回报模板。
- 优先把相关验证合并成 15–45 分钟的批次；不为每个小改动单独打断用户。
- 用户回传失败证据后，该测试包立即进入最高优先级修复；与它无关的 Codex 工作不中断。

## 2. 当前事实基线

| 工作流 | 当前状态 | 可运行性 | 下一闸门 |
| --- | --- | --- | --- |
| Picasso | 唯一已实现的 Social Will MCP；22 tools | `407 passed, 1 skipped` | DD marker 实机验证；随后实现 `mark` dispatcher/receipt |
| MiroFish | 架构与 CNPC 探针已成形，主体实现尚未开始 | 无 MCP Server、无正式运行入口 | 完成 Spike Zero 六项游戏内验证，再冻结内容包格式 |
| Chess | v0.1.4 规格较完整 | 明确无代码 | P0 外部能力探针；P1 纯 Java 核心；P2 event log |
| 物品/配方 | 有工作簿与运行时快照，但生成链断裂 | 运行时自定义配方约 960 条 | 建权威源、恢复生成器、清除旧模组引用 |
| Loot | 多份数据源和部署 JSON 并存 | 当前存在解析失败与连锁 Unknown loot table | 修复 tier1 引用并对账清单/生成 JSON |
| CNPCScripts | 资料很多，活跃版本很少 | 222 个 JS 中仅少量面向 1.21.1 | 版本分区、manifest、源码/部署单向同步 |
| 资产管理器 | 尚未创建 | 无 | 先做只读索引、校验、导出和 diff；编辑/UI 后置 |

仓库安全基线：`main` 与 `origin/main` 仍停在 2026-07-06；当前有大量未提交和未跟踪成果，Chess、MiroFish、CNPCScripts 整体尚未入库。任何清理、移动、批量格式化前必须先做分模块快照。

## 3. P0 阻塞与任务队列

状态口径：`待办` / `进行中` / `待人工验证` / `阻塞` / `完成` / `延后`。

| ID | 任务 | 状态 | 负责人 | 验收 |
| --- | --- | --- | --- | --- |
| SAFE-01 | 只读复核后，按 Picasso / Chess / MiroFish / CNPCScripts / 本体脚本分批做安全快照并推送 | 阻塞 | 用户授权 + Codex | 能从远端或本地提交恢复；未把客户端、存档、构建物误纳入 |
| SAFE-02 | 更新忽略/备份策略，处理未忽略的建筑存档与疑似重复世界副本 | 待办 | Codex | `git status` 只显示主动维护的源码/数据；不删除任何原文件 |
| GOV-01 | 以本页为唯一项目级状态台账；修正文档版本冲突 | 进行中 | Codex | README 可直达；MiroFish/Chess 状态描述一致 |
| PIC-01 | Picasso DD 三个 marker 的渲染、朝向、碰撞、交互实机验证 | 待人工验证 | 用户 | 结果写回 Picasso PROJECT_STATUS；闸门明确 PASS/FAIL |
| MIR-01 | MiroFish Spike Zero 的 0–5 六项 CNPC 1.21.1 游戏内测试 | 待人工验证 | 用户 | 基准文件与结果表齐全；失败项有原始报错 |
| CHS-01 | 锁定精确 JAR、Java/NeoForge、测试世界；完成外部能力矩阵 | 待办 | Codex + 用户 | FTB、CNPC/TACZ、CNA、Create 每项有 PASS 或预定 fallback |
| CONTRACT-01 | 统一 Chess/Picasso/MiroFish work-order 契约，正式加入 narrative writer | 待办 | Codex | 写入者、ID、状态机、目录、幂等与失败回执只有一份规范 |
| PIC-02 | 实现只支持 `mark` 的最小 dispatcher、Journal 与 receipt | 待办 | Codex | fixture 重复消费幂等；失败不污染世界；可 revert |
| MIR-02 | 关闭 OASIS/Graphiti 最小 spike，冻结 CNPC content-pack 格式 | 待办 | Codex | 产生能力结论、版本边界与 fallback；不整体照搬旧工程 |
| MIR-03 | 实现最小事实摄取、内容验证器、CNPC pack compiler、`nar_* mark` 输出 | 待办 | Codex | 同一 fixture 可确定性重建；输出带 `fact_refs` |
| CHS-02 | 建最小工程并优先实现 P1/P2：纯 Java 核心、持久化/event log | 待办 | Codex | golden fixture 稳定；事件可被 MiroFish 消费 |
| INT-01 | 三层测试世界薄切片联调 | 待办 | Codex + 用户 | NPC 内容引用 Chess 事实；marker 可见；receipt、幂等、失败和回滚全过 |
| DATA-01 | 建立物品/配方/Loot 权威源并恢复最小生成器 | 待办 | Codex | `validate → generate → diff → deploy` 可重复；产物禁止手改 |
| LOOT-01 | 清理/条件化缺失模组引用，修复 tier1 Loot 连锁报错 | 待办 | Codex | 无缺失 `paraglider/locks/ropebridge/the_ravenous` 导致的解析失败 |
| LOOT-02 | 对账 DDF 清单、Loot registry/balance/family 与部署 JSON | 待办 | Codex | 数量差异逐项解释；影子 fallback 被删除或明确冻结 |

### 里程碑通过后的拉取队列

| 顺序 | 前置条件 | 下一批工作 |
| --- | --- | --- |
| P1-A | PIC-01、PIC-02 稳定 | Picasso segmentation/registry，再在 Room 与 Rail 中选择更接近当前试玩需求的一线 |
| P1-B | CHS-02 event log 稳定 | Chess P3 最小 NeoForge shell/headless 命令；随后按实机价值拉取经济、治理或镜像，而不是先做大地图 UI |
| P1-C | MIR-03 薄切片稳定 | MiroFish 最小 MCP 管理面、更多 thread/content 类型、连续性图查询与作者工作流 |
| P1-D | DATA-01、LOOT-01 稳定 | 资产管理器只读索引/校验/导出 MVP，再开放受控编辑和 UI 美术升级 |

## 4. 物品与资产治理结论

现有工作簿不是废稿，而是很有价值的种子数据；但它还不能直接充当运行时数据库：

- `汇总` 有 439 条物品记录、437 个唯一 ID；仍有跨表重复、等级 `153` 哨兵值、空等级和大小写/命名不一致。
- “所有可拾取的垃圾”有 628 条有效记录、606 个唯一 ID，存在 22 组重复 ID。
- “工艺总览”已经定义徒手/背包/T1/T2/T3/Create 的职责，但数量列为空，工作簿没有可执行配方关系表或公式。
- 当前工作簿被全局 `*.xlsx` 忽略；运行时 `utd_recipe_data.js` 与工作簿时间已漂移，仓库内也没有可重复生成链。

因此资产管理器 MVP 的顺序固定为：

1. **只读导入器**：扫描权威 JSON/CSV、工作簿、已安装 JAR、KubeJS/数据包产物。
2. **规范化与校验**：item identity、版本/来源、名称、13 类、标签、NBT/1.21 components、模型/纹理、配方图、Loot、回收、启用/缺失状态、hash。
3. **问题与差异视图**：缺失 ID、重复、命名空间漂移、源码与部署差异、生成预览。
4. **稳定后再开放编辑**；美术化页面与组件包选型属于第二阶段。

`Code/ItemsReFresh` 是旧 Spigot/Mohist 区域容器刷新插件，不是资产数据库；版本声明也有冲突，不能直接作为管理器基础。

## 5. 14 天节奏

| 日期 | 目标 | 退出条件 |
| --- | --- | --- |
| 7/11–7/12 | 安全快照、范围冻结、两项人工验证、依赖锁定 | SAFE-01 可恢复；PIC-01/MIR-01 有结果；P0 契约问题列表冻结 |
| 7/13–7/16 | Picasso 契约/dispatcher；MiroFish spikes/格式；Chess P0/P1；Loot 止血 | 各线都有可运行 fixture；当前 Loot 解析故障消失 |
| 7/17–7/20 | MiroFish 最小实现、Chess event log、Picasso receipt；数据生成链 | 三层可在离线 fixture 贯通；数据管线可生成并 diff |
| 7/21–7/22 | 测试世界联调与故障注入 | 事实引用、权限、幂等、失败、回滚、部署全验收 |
| 7/23–7/24 | 修复、文档、启动脚本、演示存档、发布快照 | 一键复现；状态页真实；剩余事项明确延后 |

## 6. 工作纪律

- 同时最多三条进行中主线：Social Will 薄切片、本体数据止血、人工验证；不再开第四条大功能。
- 上一条是用户可见的主线限制，不限制 Codex 在每条主线内部并行做代码、测试、审计和文档；用户侧一次只处理一个主测试包。
- 新想法先进入收件箱，未给验收标准不进入开发。
- 生成物不作为权威源；源码到部署必须单向、可重建、可 diff。
- “文档完成”“单元测试通过”“游戏内通过”是三种不同状态，不互相替代。
- 未经明确确认，不删除、移动、批量格式化存档、客户端目录或未跟踪成果。

## 7. 等待用户决定

1. 是否授权 Codex 先按模块制作并推送安全快照；这是继续开发前的最高优先级。
2. 两项必须由你在游戏客户端执行的验证，何时方便开始；Codex 会提供逐项操作和回报模板。
3. 若两周结束时存在不可妥协的完整功能清单，请补入本页；否则按“前置验收一过就继续拉取下一批”的规则推进。

## 8. 已确认决策

- **2026-07-11 / 冲刺产能模型**：主要实现与修复由 Codex 完成；用户时间集中用于 Minecraft 实机验证和失败反馈。三层薄切片是最低里程碑，不是两周产出上限。

## 9. 收件箱

- GitHub 上的网页/Mod 游戏页面组件包调研：待资产管理器技术栈与首屏范围冻结后开始。
- 全量 UI 美术升级：延后至核心数据模型和接口稳定后。
