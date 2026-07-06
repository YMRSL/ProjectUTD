# Sable × Flashback 桥接 (SableFlashbackBridge)

让 Flashback 回放能够完整重现 Sable 物理结构(含 Create Aeronautics 飞艇),
包括**录制中途才物理化**的结构。2026-07-03 完成,实测有效。

> 宿主:代码目前编译在 `utd_doomsday_patch` 内(本目录为源码归档副本)。
> 若要抽成独立 mod,把 `src/` 六个文件挪入新工程,补上 mixins.json 注册与
> 载荷注册(见下),依赖照抄 utd_doomsday_patch 的 libs(sable 1.2.2 主 jar +
> sable-companion + veil,均 compileOnly)。

## 问题背景

- Flashback 只录 vanilla TCP 包流;sable 默认用**自有 UDP 通道**传结构运动 → 录不到
- 关掉 UDP(`sable-common.toml`: `disable_udp_pipeline = true`,官方注释明示
  为 Replay 类 mod 兼容而设)后运动包可录,但结构的**初始全量同步**
  (StartTracking + plot 区块 + Finalize,见 `SubLevelTrackingSystem.sendFullSync`)
  通常发生在开录之前 → 回放里只有运动包,结构本体不存在(sable#705 的
  "Received a sub-level movement packet for a non-existent sub-level")

## 回放管线的关键事实(Flashback 0.39.5 源码实证)

1. 回放 ≠ 转发:Flashback 把录像里的 vanilla 包**解释成回放服务器的世界状态**,
   再按 vanilla 视距规则流式发给观众 —— plot 区块坐标在数百万格外,**永远不会
   被下发**(活体游戏里这步由 sable 服务端追踪系统绕过,回放服务器上没有它)
2. 唯一例外:`ClientboundCustomPayloadPacket` 被包成 `FlashbackRawCustomPayload`
   **原始字节直发观众客户端**(`ReplayServer` L626 起,避免 mod 包重编码问题)
3. Flashback 提供官方集成钩子 `Recorder.writeCustomSnapshot`
   ("Mods can mixin here ... or packets"),每次写快照(开录/暂停恢复)时调用

## 架构(三件套 + 两辅助)

结论:**一切要送达观众客户端的数据都必须走自定义载荷通道**,于是:

| 组件 | 文件 | 职责 |
|---|---|---|
| 载波载荷 | `SableReplayPayload` | `utd_doomsday_patch:sable_replay`,携带一个"游戏协议 codec 序列化后的完整 clientbound 包"的字节 |
| 快照注入 | `FlashbackSableSnapshotMixin` → `SableFlashbackBridge` | 挂 `writeCustomSnapshot`:对每个已追踪 `ClientSubLevel`,按 `sendFullSync` 原序列合成 StartTracking + 逐 plot 区块(`ClientboundLevelChunkWithLightPacket`,用客户端区块+plot 光照引擎构造)+ Finalize,逐个裹进载波塞入快照 |
| 活录改道 | `FlashbackSableLivePacketMixin` | 挂 `Recorder.writePacketAsync`(全部被录包的必经之路,Bundle 已被它拆散):plot 网格内的区块包 + sable 状态类载荷(start/finalize/split/bounds/stop)改写为载波(同通道保序);运动包保持 raw 路径(量大、时序敏感、本就可用) |
| 客户端重放器 | `SableReplayClientHandler` | 收载波→排队(回放世界 level/connection 就绪前缓冲)→游戏协议 codec 解码→`packet.handle(connection)`,等效原服务器亲发 |
| BE 守卫 | `AeronauticsBurnerClientWriteGuardMixin` | aeronautics 热气球燃烧器 BE 的 `write` 无条件把 balloon 转 ServerBalloon(上游 bug);客户端建区块包会 CCE → 客户端空转。另有逐区块 try/catch 兜底 |
| 载荷注册 | `UtdDoomsdayPatch#registerPayloads` | NeoForge `RegisterPayloadHandlersEvent`,`optional().playToClient` |

### 为什么状态载荷也要改道(而不是留在 raw 路径)

raw 路径立即送达,载波路径经客户端队列下一 tick 冲刷——若 StartTracking 走 raw
而区块走载波,顺序会颠倒(Finalize 先于区块到达)。全部同通道 = 严格保序。

## 调试仪表(默认开启,grep `[SABLE-REPLAY]`)

- 录制:钩子跳过原因 / 每结构一行(UUID、plot、区块数、跳过数)/ 总计行 /
  `live: rerouted ...`(改道计数)
- 回放:`carrier received`(前5+每100)/ `flushed N carriers` /
  `dispatched #N: <包类名>`(前10+每100)/ 失败带完整异常 / 登出清队列

## 已知边界

- 结构上的 Create 动态部件(转动的螺旋桨等)是另一条同步链,不在本桥范围
- 编译面向 sable 1.2.2 内部类;sable 2.x 包结构已变,升级需适配

## 已知 Bug（待修，2026-07-05 搁置）

| Bug | 根因 | 修复方向 |
|-----|------|---------|
| seek 后绳索(honey_glue)固定在初始位置 | honey_glue 在 sable 子世界，不在主世界；我们的 AddEntityPacket 只加到主世界副本，对子世界渲染无效；FlashbackAccurateEntityPosition 更新主世界副本不影响子世界实体 | seek 时遍历所有 ClientSubLevel 的 honey_glue，直接 snapTo 快照坐标 |
| seek 后 Superb 载具消失/不稳定 | 我们的载波把载具加入主世界，Flashback 快进阶段 vanilla 流式再发同 ID AddEntityPacket 产生冲突；已加 `ReplayAddEntityDeduplicateMixin` 但多次 seek 场景仍有问题 | 进一步完善去重逻辑，或在 seek 时先清后建 |
| 物理结构不阻挡 tacz/superb 子弹 | 弹道判定未联动 sable 结构碰撞（superb 炮弹正常交互，小子弹不正常） | 挂 tacz/superb 弹道碰撞检测，加 sable 结构 AABB 查询 |

## 上游可反哺

- sable#705:本方案全文(官方收编最优)
- aeronautics:`HotAirBurnerBlockEntity.write` 的 ClientBalloon CCE
- Flashback(另案):`lenient_registry.MixinRegistry` 兜底返回值本体而非
  `Holder.Reference` 的类型 bug(1.21.11 分支仍在),修法=包 `wrapAsHolder`
