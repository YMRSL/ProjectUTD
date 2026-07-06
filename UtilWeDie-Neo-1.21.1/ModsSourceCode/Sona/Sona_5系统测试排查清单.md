# Sona 本体 5 系统 · 游戏内测试排查清单

> 生成日期 2026-06-19 · 贴 `Sona-1.21.1-neoforge` 源码实测路径，非泛泛而谈。
> 总前提：测试时**关创造/旁观**（代码里 `isCreative()||isSpectator()` 直接跳过所有系统）。
> 配置文件：`.minecraft/.../config/sona-common.toml`（开关名见每节）。改 toml 后需重进世界生效。

---

## 通用排查（5 系统都不动时先看这）
1. **mod 是否加载**：F3 看不到崩溃；`/sona`、`/infection`、`/rot` 等命令能 Tab 补全 = 服务端类正常注册。
2. **配置总开关**：`sona-common.toml` 里 `INFECTION_OPEN / INJURY_OPEN / ROT_OPEN / RUST_OPEN / SOUND_OPEN` 是否为 `true`。
3. **单机即"服务端"**：所有 Manager 逻辑跑在逻辑服务端；单机存档同样生效。
4. 命令通用形态：`get` 看当前值、`set <数字>` 直接设、`add <数字>` 增量。

---

## ① 感染 Infection（开关 `INFECTION_OPEN`）
**触发**
- 被「感染源」攻击：`INFECTION_SOURCE_MOB` 列表里的怪物打你 → 感染值上升（`InfectionManager.onAttacked`）。
- 用「感染源物品」：吃/用 `INFECTION_SOURCE_ITEM` 里的物品（`onUseItem`）。
- 被 `INFECTION_SOURCE_PROJECTILE` 弹射物命中。
- **测试命令**：`/infection @s set 50`、`/infection @s add 20`、`/infection @s get`。

**预期可见**
- 屏幕角落**绿色感染 overlay**（[InfectionOverlay.java]）：≤40 n1 → ≤70 g2 → ≤90 g3 → <100 g4 → =100 g5，五档贴图逐级加深。位置由 `INFECTION_OVERLAY_PRESET`/`X_OFFSET`/`Y_OFFSET` 控制。
- `BLUR_MESSAGE=true` 时，感染越高聊天文字越模糊（`InfectionManager.blurMessage`）。
- `TURN_ZOMBIE=true` 且感染 > `INFECTION_THRESHOLD` 时死亡 → 变僵尸（`turnZombie`，僵尸类型取 `ZOMBIE_LIST`）。
- **区块感染区** `INFECTED_ZONE_OPEN=true`：远离出生点的区块雾/天空/草/水变色（`InfectionFogRenderer` + `INFECTED_ZONE_*_COLOR`），并按规则限制/替换刷怪。

**没反应排查**
- overlay 不出 → 是否创造/旁观；`INFECTION_OPEN` 是否 true；先 `/infection @s set 100` 强制拉满看 overlay 是否出现（排除"值根本没涨"还是"涨了但没画"）。
- 值不涨 → 攻击你的怪是否在 `INFECTION_SOURCE_MOB` 白名单；`SUSCEPTIBLE_POPULATION`（易感人群）是否包含玩家。
- 区块感染区颜色不变 → `INFECTED_ZONE_OPEN`、出生点零区是否已生成（`LevelEvent.CreateSpawnPosition` 只在建世界时算一次，老存档可能没有）。
- ⚠ **Fungal 孢子感染 + 眩晕依赖此系统**：Fungal 那 4 只怪的孢子/眩晕若失效，先确认本系统正常。

---

## ② 受伤 Injury（开关 `INJURY_OPEN`）
**触发**
- 受到伤害（`InjuryManager.onAttacked`），但 `INJURY_EXCEPT_DAMAGESOURCE` 列表内的伤害类型不计。
- **测试命令**：`/injury @s set 50`、`/injury @s add 30`、`/injury @s get`；绷带值 `/bandage @s set 3`。

**预期可见**
- 屏幕**受伤 overlay**（[InjuryOverlay.java]，血迹/红边类）。
- 治疗：用 `INJURY_TREATMENT_ITEM`（绷带等）降低受伤值。
- 睡觉治疗 `HEAL_WHILE_SLEEP=true`：醒来回血（`healBySleep`），受 `HEAL_NEED_BANDAGE`/`HEAL_AMOUNT`/`HEAL_THRESHOLD` 约束。
- `RISE_UNDERWATER=true`：水下受伤相关上升行为。

**没反应排查**
- overlay 不出 → 同感染：先 `/injury @s set 100` 强制看画面；确认非创造。
- 受击不涨 → 该伤害源是否在 `INJURY_EXCEPT_DAMAGESOURCE`。
- 睡觉不回 → `HEAL_WHILE_SLEEP`、`HEAL_NEED_BANDAGE`（需要绷带却没绷带值）、`HEAL_THRESHOLD`。

---

## ③ 腐烂 Rot（开关 `ROT_OPEN`）★本次修复点
**触发**
- 食物随**游戏时间**腐烂；记账发生在**开/关容器**时（`containerOpen` / `onClose→rotTimeUpdate`）和背包内。速率 `ROT_WEIGHT`，温度影响 `ROT_TEMPERATURE`（白名单 `ROT_TEMPERATURE_WHITELIST`）。
- **测试命令**（作用于**手持**物品）：`/rot set 80`、`/rot add 50`、`/rot get`。

**预期可见**
- ★**物品栏槽位左上三色新鲜度图标**（本次修复：补回 `GuiMixin`+`AbstractContainerScreenMixin`）。腐烂阈值：<40 safe → ≥40 mild → ≥70 bad → ≥90 awful。发酵物额外叠 `warped` 图标。**快捷栏和背包/箱子界面都应出现**。
- 悬停 tooltip 显示新鲜度文字（这条迁移时一直正常，可作对照）。
- `ROT_EFFECT=true`：吃下腐烂食物给负面效果（`eatRotFood`）。
- 发酵 `ROT_WARPED=true`：潜行状态主手食物 + 副手催化物右键 → 发酵（`warpFood`，催化物见 `WARPED_ITEMS`）。

**没反应排查（重点：图标 vs tooltip 分离判断）**
- **tooltip 有、槽位图标没有** → 正是本次修的 bug。确认 mods 里 `sona-101.1.21.1.jar` 是 2026-06-19 15:31 之后的新 jar；**改 mixin 必须重启客户端**，reload(F3+T)不够。
- 图标、tooltip 都没有 → `ROT_OPEN` 是否 true；该物品是否食物（`has(DataComponents.FOOD)`）且在可腐烂范围（`ROT_WHITELIST`/`ROT_DETAIL`）；先 `/rot set 95` 强制拉满再看。
- 图标位置/透明度怪 → `ItemMarkHandler.renderMark` 用 0.5 alpha + 8×8 贴图，属正常。

---

## ④ 锈蚀 Rust（开关 `RUST_OPEN`）★本次修复点（同图标链路）
**触发**
- 工具/武器随使用、受击锈蚀（`RustManager.onAttacked`），速率 `RUST_WEIGHT`。可锈范围 `RUST_WHITELIST` 减 `RUST_BLACKLIST`。
- **测试命令**（作用于**手持**物品）：`/rust set 80`、`/rust add 50`、`/rust get`。

**预期可见**
- ★**槽位锈蚀三色图标**（与腐烂共用 `ItemMarkHandler`，本次一并修复）：<40 safe → ≥40 bad → ≥70 awful。打蜡物额外叠 `waxed` 图标（图标画在下半格 y+8）。
- 悬停 tooltip 显示锈蚀文字（对照用）。
- 锈蚀**降低物品属性**（`rustAttributeModifierEvent` → `addRustAttributeModifier`）。
- 打蜡防锈：潜行主手物品 + 副手 `WAX_ITEM` 右键（`wax`），`WAX_PERMANENT`/`WAX_TIMES` 控制次数。
- 除锈：副手 `RUST_REMOVE_ITEM` 右键（`removalRust`）。

**没反应排查**
- tooltip 有、图标没有 → 同腐烂，本次修复点，确认新 jar + 重启客户端。
- 完全无锈 → `RUST_OPEN`；物品是否可锈（在白名单、非黑名单、`isDamageableItem`）；`/rust set 90` 强测。
- 属性没降 → 锈蚀值是否真的高；`ItemAttributeModifierEvent` 是否被别的 mod 抢改。

---

## ⑤ 循声 Sound（开关 `SOUND_OPEN`）
**触发**
- 玩家发出声音吸引怪物：疾跑 `SPRINT_SOUND=true` 制造声响；枪声 `GUN_SOUND_ATTRACT=true`（枪白名单 `GUN_SOUND_WHITELIST`）。
- 声音生成 **`SoundDecoy` 诱饵实体**（`spawnSoundDecoy`），范围内怪物被吸引过去。
- 怪物入场时被注入索敌 AI（`SoundManager.insertAi`，`EntityJoinLevelEvent`）；可吸引怪由 `SOUND_ATTRACTED_MOB_WHITELIST` 加、`..._BLACKLIST` 减。
- 暴露值：`FIRE_EXPOSURE`（开火暴露）/`SILENCE_EXPOSURE`（消音减免）。

**预期可见**
- 安静站立 → 怪物不易直接锁定你；疾跑/开枪 → 附近怪物明显**朝声源/诱饵方向移动**。
- `DECOY_LIFE` 控制诱饵存活 tick。

**没反应排查**
- ⚠ **`SOUND_OPEN` 在服务器启动时一次性加载**（`ServerStartedEvent→setSoundOpen` + 读 `SOUND_WHITELIST`）。**改完 toml 必须完全退出存档/重启服务端**，世界内重载无效。
- 怪不被吸引 → 该怪是否在 `SOUND_ATTRACTED_MOB_WHITELIST`、是否被 `..._BLACKLIST` 排除；`insertAi` 只对 `Mob` 子类生效。
- 疾跑无声 → `SPRINT_SOUND`；枪声无效 → `GUN_SOUND_ATTRACT` + 枪是否在 `GUN_SOUND_WHITELIST`（TaCZ 枪走 KubeJS marker，见 ZSK 联动）。

---

## 一页速测脚本（逐条敲，看现象）
```
/gamemode survival
/infection @s set 100      # 看绿色感染 overlay 拉满 → 设 0 退掉
/injury @s set 100         # 看受伤 overlay → /bandage @s set 5 再用绷带降
# 手持一个食物：
/rot set 95                # 看槽位 awful 图标 + tooltip（快捷栏&背包都看）
# 手持一把铁/工具：
/rust set 90               # 看槽位锈蚀图标 + 属性下降
# 疾跑 / 开一枪，观察附近怪是否朝你/诱饵移动（需先重启确认 SOUND_OPEN 已加载）
```

> 任一系统"命令设值成功但现象不出"，基本可定位到**渲染/效果端**而非数据端；若连 `get` 都报错或值不变，则是**数据/事件端**。两端分离判断能最快缩小范围。
