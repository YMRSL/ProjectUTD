# Fungal 行为脚本 — 挂法说明

这些是 **CNPC ECMAScript（JS）脚本**，逐怪一段，挂在 NPC 的「高级 → 脚本（Scripts）」里。
基础版即可用，锦上添花用，可按口味删改数值。

> 前提：`playGeckoAnim(name)` 通道已修通（addon `MixinNpcWrapper#playGeckoAnim`），脚本里
> `npc.playGeckoAnim("xxx")` 会向所有客户端广播一次性动画，播完自动回到 idle/walk。
> 动画名必须是该怪 `*.animation.json` 里真实存在的（见《Fungal_CNPC配置卡.md》各怪动画清单）。

## CNPC 脚本面板 → 事件对应（重要）
打开 NPC 万能棒右键 → **高级** → **脚本**。语言选 **ECMAScript / javascript**。
左侧是「事件下拉」，把对应脚本贴进对应事件的代码框（**一个事件一个函数**）：

| 脚本文件 | 贴进哪个事件 | 触发时机 |
|---|---|---|
| `infected_sporer_集群.js` | **Tick / Update**（每 tick） | 周期性找同类聚团、跟首领 |
| `volatile_格挡处决.js` 的 `damaged(e)` 段 | **Damaged**（受击） | 被打时概率举盾 `guard`/`parry` |
| `volatile_格挡处决.js` 的 `tick(e)` 段 | **Tick / Update** | 低血举盾、抓机会 `execution` |
| `volatile_格挡处决.js` 的 `killed(e)` 段 | **Killed Entity**（击杀） | 杀掉目标时放处决动画 |
| `lurker_跳扑.js` 的 `tick(e)` 段 | **Tick / Update** | 接近玩家时 `start_jump` 扑过去 |
| `lurker_跳扑.js` 的 `target(e)` 段（可选） | **Target**（选定目标） | 锁定目标瞬间起跳 |

> CNPC 不同事件框是**独立函数体**，函数名（`tick`/`damaged`/`killed`/`target`）只是约定，
> CNPC 实际是按你贴进的「事件框」来调用的。所以：把标了 `// === 事件：Tick ===` 的整段，
> 贴进脚本面板的 Tick 框即可；标 `// === 事件：Damaged ===` 的贴进 Damaged 框，依此类推。

## 通用 API 速查（本包脚本用到的，均已对 CNPC-Unofficial-1.21.1 jar 校验）
- `npc.playGeckoAnim("名")` — 放一次性 gecko 动画（addon 提供）。
- `npc.world.getClosestEntity(pos, 半径, 类型)` / `npc.world.getNearbyEntities(pos, 半径, 类型)`
  - 类型：`1`=玩家，`2`=NPC（CNPC 常用约定）。
  - `pos` 用 `npc.getWorld().getIPos(x,y,z)` 或直接 `npc.world.getIPos(...)` 造。
- `npc.setAttackTarget(living)` — 设定攻击目标；CNPC 自带 AI 会**自动寻路过去**（脚本里最稳的“走向某处”手段，无需手写寻路）。
- `npc.getName()` / `entity.getName()` — 名字（集群按名字判同类，**给同类怪起同一个名**！）。
- `npc.getHealth()` / `npc.getMaxHealth?` — 血量（低血判定）。
- `npc.getX/Y/Z()`、`entity.getX/Y/Z()` — 坐标。
- `npc.getStoreddata()` / `npc.getTempdata()` — 存冷却计数（tempdata 重启清空，做冷却足够）。

## 集群的关键前置：同类要**同名**
集群脚本靠「名字相同」判断谁是同类。配怪时把所有 infected 命名为 `infected`，
所有 sporer 命名为 `sporer`（NPC 万能棒 → 主页 → Name）。否则它们认不出彼此。

## 测试顺序建议
1. 先挂 **lurker 跳扑**（最直观，单怪即可看效果）。
2. 再挂 **volatile 格挡/处决**（拿剑砍它，看是否举盾/被它处决）。
3. 最后挂 **infected 集群**（至少摆 3~5 只同名 infected，看是否聚团跟首领）。
