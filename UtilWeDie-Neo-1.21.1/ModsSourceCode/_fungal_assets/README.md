# Fungal-Hazard 僵尸资产提取 + CNPC 复刻方案

> 来源：`ModsSourceCode/_src_download/fungal/Fungal-Hazard-master`（1.20.1 Forge，作者 Scarasol）
> 提取日期：2026-06-14
> 本目录 = Sona 三件套迁移第一步的资产产出 + 可行性结论。

---

## TL;DR（先回答最关键的「模型可行性」）

**结论：CNPC 接不了 Fungal 的 GeckoLib 模型（走 c 路线）。** 三条路逐一核实：

- **(a) 有现成 CNPC-Gecko-Addon 直接喂 .geo.json** → **不存在**。新包 `mods/` 里只有 `geckolib-neoforge-1.21.1-4.8.4.jar`（GeckoLib 本体渲染引擎）+ `CustomNPCs-Unofficial-NeoForge-1.21.1.20251230.jar` + `CustomSkinLoader`。**没有任何「CNPC×GeckoLib 桥接 addon」**（旧包、整个 ProjectUTD 也搜过，无）。缺口扫描里提到的 "CNPC-Gecko-Addon" 在现实里没有这个 mod 进包，且据我所知社区也没有公开的「把任意 .geo.json 喂给 CNPC」的 addon。
- **(b) 把 GeckoLib 模型转成 CNPC/OBJ 格式** → **CNPC 根本没有 OBJ/数据驱动模型加载器**。反编译 CNPC jar 确认：`INPCDisplay.setModel(String)` 的参数是一个**字符串预设名/注册名**，不是文件路径；CNPC 的模型全是**硬编码 Java 类**（`ModelNpcDragon` / `ModelPony` / `ModelNPCGolem` / `ModelNpcSlime` / `ModelClassicPlayer` 等盒子模型）。它的「借用别的 mod 模型」机制（`GuiCreationExtra$GuiTypeCobblemon/Pixelmon/DoggyStyle` + `CobblemonHelper`/`PixelmonHelper`）是**对特定 mod 的硬编码集成**，不是通用模型接口。jar 内 `gecko`/`bernie` 引用数 = **0**。所以没有「转格式后喂进去」的入口。
- **(c) CNPC 接不了自定义 GeckoLib 模型，换思路** → **这是现实路线**。CNPC 侧只能用：玩家皮肤模型 / CNPC 自带的少数模型 / clone 原版或其它 mod 的实体外观。Fungal 的 4 个怪都是 GeckoLib 自定义骨骼模型，**无法在纯 CNPC 内复刻其外观**。

### 那这一步到底能落地什么
模型这条路在「纯 CNPC」里走不通，但有两个**真正可行**的方向，取舍如下：

1. **行为复刻（CNPC 脚本，外观用替代）**：用 CNPC NPC + 脚本复刻 Fungal 僵尸的**行为**（集群/猛冲/扑咬骑乘/孢子/格挡闪避），外观退而求其次用原版僵尸皮肤或玩家皮肤。**集群、猛冲、孢子光环、扑咬**这些都能用脚本+原生 AI 配置实现；**死亡断裂→爬行**只能近似（见下）。这是「移植行为、放弃专属模型」。
2. **实体本体移植（保留 GeckoLib 外观，放弃 CNPC）**：把 Fungal-Hazard 本体（连同它依赖的 Sona-Survival 母 mod）移植/升级到 1.21.1 NeoForge，**保留原汁原味的模型/动画/断裂/集群**。代价是这不是 CNPC 方案，而且有依赖链（见「依赖警告」）。如果目标是「玩家在游戏里看到和原版一样的真菌僵尸」，这条才能保真。

> 一句话：**模型保真 = 必须移植实体本体（方向2）；纯 CNPC = 只能复刻行为、外观用替身（方向1）**。两者不可兼得，因为 CNPC 没有 GeckoLib 接入点。

---

## 依赖警告（移植路线必读）

Fungal-Hazard **不是独立 mod**，它是 **Sona-Survival 的官方 addon**（同作者 Scarasol）。源码里 6 个文件 import 了 `com.scarasol.sona.*`：

| 用到的 Sona 类 | 用途 |
|---|---|
| `com.scarasol.sona.util.SonaMath` | 向量夹角计算（集群/格挡/闪避判定，×4 文件） |
| `com.scarasol.sona.init.SonaMobEffects` | INFECTION（感染）/STUN（眩晕）/SLIMINESS/IMMUNITY 等药水效果 |
| `com.scarasol.sona.manager.InfectionManager` | `canBeInfected()` —— 感染传播系统 |
| `com.scarasol.sona.entity.SoundDecoy` | 声音诱饵实体（Volatile 判断目标用） |
| `com.scarasol.sona.effect.PhysicalEffect` | 物理类效果基类（Volatile 只吃这类效果） |
| `com.scarasol.sona.accessor.IBaseContainerBlockEntityAccessor` | mixin accessor |

`build.gradle` 依赖：`curse.maven:sona-survival-...`、`geckolib`、`zombie-survival-kit`、`timeless-and-classics-zero`、`superb-warfare`、`curios`、`mixinextras`、`jade`。
**当前新包里既没有 sona-survival 也没有 fungal 的 jar。** 移植 = 至少要先把 Sona-Survival 也搞到 1.21.1（孢子怪的感染、Volatile 的眩晕/物理效果都依赖它），否则这些怪降级成「没有感染/眩晕能力的普通版」。

---

## 1. Fungal 僵尸清单（4 种 + 各自资产/特点）

注册在 `init/FungalHazardEntities.java`，渲染器在 `init/FungalHazardClientRegister.java`。

| 怪 (registryName) | 中文/英文名 | 模型 geo | 动画 | 贴图 | 类别 | 血/攻/速/甲 | 渲染器 |
|---|---|---|---|---|---|---|---|
| `infected` | 受感染者 / Infected | `infected.geo.json` | `infected.animation.json` | `infected1.png` (+`infected1_glowmask.png`) | 普通杂兵（最常见，地牢权重180） | HP20 / ATK3 / 0.23 / 甲2 | `MutilatableZombieEntityRenderer`(glow=true) |
| `sporer` | 孢子者 / Sporer | `sporer.geo.json` | `sporer.animation.json` | `sporer.png` | 辅助/支援（地牢80） | HP30 / ATK2 / 0.23 / 甲2 | `MutilatableZombieEntityRenderer`(glow=false) |
| `volatile` | 狂暴者 / Volatile | `volatile.geo.json` | `volatile.animation.json` | `volatile.png` (+`volatile_glowmask.png`) | **精英/Boss 级**（地牢20，稀有） | HP85 / ATK13 / 0.45 / 甲15·韧8·击退抗性1 | `HumanoidFungalZombieEntityRenderer`(glow=true) |
| `lurker` | 潜伏者 / Lurker | `lurker.geo.json` | `lurker.animation.json` | `lurker.png` | 特殊（蜘蛛类，**不自然刷新**，靠别的怪召唤/或其它来源） | HP8 / ATK2 / 0.3 / 甲2 | `ArachnidFungalZombieEntityRenderer`(glow=false) |

继承结构：
- `Zombie` → `AbstractHumanoidFungalZombie`（FSM 状态机 + GeckoLib + 集群/巡逻）→ `AbstractMutilatableZombie`（断裂/爬行）→ `InfectedEntity` / `SporerEntity`
- `AbstractHumanoidFungalZombie` → `VolatileEntity`（精英，自带格挡/闪避/跳扑处决，**不参与集群** `canJoinPatrol()=false`）
- `Spider` 系 → `AbstractArachnidFungalZombie` → `LurkerEntity`（扑到目标身上骑乘并持续注入 INFECTION+POISON）

各怪招牌能力：
- **Infected**：有 `CanRun` 几率（`INFECTED_RUN_CHANCE`）变成会**冲刺追击**（CHASING 状态，动画 2 倍速）的快僵尸。可断裂→爬行。
- **Sporer**：活着时每 10 tick 对范围内 UNDEAD 治疗、对范围内活体施加 INFECTION（`aiStep`）；**死亡后还会持续喷孢子一段时间**（`tickDeath` 里 `SPORER_ABILITY_TIME_DEATH`），是「死了更恶心」的辅助怪。可断裂→爬行。
- **Volatile**：精英。会**格挡**（GUARD，正面减伤+招架眩晕）、**闪避**（DODGE，识别玩家拿枪/拿近战时侧跳）、**跳扑处决**（JUMP→JUMPING→把玩家按住 RIDING→EXECUTION 高伤）、白天逃跑（`VOLATILE_FLEE_IN_SUN`）。**不断裂**（没继承 Mutilatable）。
- **Lurker**：小蜘蛛，跳到目标身上**骑乘**（RIDING），每秒注入 INFECTION+POISON，被盾挡下来会弹开。

---

## 2. 「死亡断裂 → 爬行僵尸」机制剖析（重点）

**实现方式 = 同一个实体的状态切换 + GeckoLib 客户端骨骼隐藏，不是替换成另一个实体。** 关键在 `AbstractMutilatableZombie.java` + `MutilatableZombieEntityRenderer.java`。

### 2.1 断肢（缺胳膊少腿的外观）
- `EntityDataAccessor<Integer> APPEARANCE`（同步数据，0~5）决定缺哪个部位。
- `finalizeSpawn()` 里按 `CommonConfig.MUTILATION_CHANCE` 几率 roll `appearance = nextInt(1,6)`：
  - 1 = 缺右臂、2 = 缺左臂、3 = 缺右腿、4 = 缺左腿、5 = 缺双腿。
  - `appearance > 2`（缺腿）→ 直接进入 `CREEP` 爬行状态 + `addKnockBackAttributeModifier()`。
- **外观断裂是纯客户端渲染**：`MutilatableZombieEntityRenderer.renderRecursively()` 覆盖 GeckoLib 的逐骨骼渲染，根据 appearance 对 `RightArm`/`LeftArm`/`RightLeg`/`LeftLeg`（含 `*Leg` 模糊匹配）调 `bone.setHidden(true)`，渲完再 `setHidden(false)`。手持物也对应隐藏（`canRenderMainHandItem`/`canRenderOffHandItem`）。
- 服务端同步：缺臂的同时把对应手槽 `setItemSlot(..., ItemStack.EMPTY)` 清空。

### 2.2 被打断腿 → 倒地 → 爬行（动态触发）
全在 `AbstractMutilatableZombie.hurt()` + FSM：
- 受到**远程/间接伤害**（`damageSource.isIndirect()`，如枪/箭）时，按 `amount * MUTILATION_COEFFICIENT / maxHealth` 几率，**且命中点算出来低于腿部高度**（`hitY < onPos + 1 + bbHeight*0.35`，即「打中下半身」）→ `setState(FALL)`（被打倒）。
- 受到**坠落伤害**也按几率进 `FALL`。
- `FALL` 状态：站不起来（`startFallState` 停导航、加击退抗性、播 `fall` 动画）；`fallState` 里每 tick 有 1% 几率转 `CREEP`。
- `CREEP`（爬行）状态：换成爬行碰撞箱 `CREEP_DIMENSIONS(0.9×0.7)`、爬行动画组（`creep`/`creep_idle`/`creep_attack`/`creep_attack2`/`creep_death`）、不能跳、`canPatrol=false`、`isMutilation()=true`。
- 动画切换由 GeckoLib 三个 controller 管：`animationController`(站立)、`creepAnimationController`(爬行)、`deathAnimationController`(死亡时按 isMutilation 选 `death` 或 `creep_death`)。`fall_to_creep` 是倒地→开始爬的过渡动画。

> **要点**：所谓「断裂为爬行的僵尸」=「同一实体被打断腿后进入 CREEP 状态 + 隐藏腿骨 + 切爬行动画」。**没有第二个实体**。这对 CNPC 复刻是坏消息（见第 4 节）。

---

## 3. 集群 + 「破坏方块」剖析

### 3.1 集群 = 巡逻/迁徙系统（不是物理聚团）
源码里**没有 cluster/swarm/horde 关键字**；用户说的「数量多时聚集成集群」对应的是 **Patrol（巡逻迁徙）系统**：
- 入口 `AbstractHumanoidFungalZombie.registerGoals()` → `FungalZombiePatrolGoal`（仅 `canJoinPatrol()=true` 的怪加，即 Infected/Sporer；Volatile/Lurker 不参与）。
- 触发条件 `canJoinPatrolNow()`：开关 `CommonConfig.INFECTED_PATROLLING` 打开、室外、夜晚/阴雨（或不怕阳光）。配置注释原文：「Whether regular infected that are outdoors will spontaneously gather and begin migrating.」
- 逻辑（`FungalZombiePatrolGoal.tick()` + `findPatrolCompanions()`）：在**周围 16 格**找其它 `IPatrolMob` 同类 → 选一个 leader（`canBeLeader`）→ leader 随机找 500 格外的 `patrolTarget`，所有 companion `setPatrolTarget` 并导航跟随 leader → 离 leader >256 格脱队、>64 格加速追。`FungalZombieGroundPathNavigation` 让非 leader 跟随 leader 移动。
- 效果：散布的僵尸自发汇成一支朝同一方向迁移的「队伍/潮」。这就是「集群」。

### 3.2 「破坏方块」——实际只有破门
**全仓库 grep 确认：没有任何 `destroyBlock` / `setBlock` / `removeBlock` / mob_griefing 代码。** 僵尸唯一的破坏方块行为是 **vanilla `Zombie` 自带的破门**：`AbstractHumanoidFungalZombie` 里 `new MoveThroughVillageGoal(this, 1.0D, true, 4, this::canBreakDoors)`，`canBreakDoors` 继承自原版 Zombie（困难难度破木门）。**Fungal 没有给僵尸加「砸墙/挖方块」能力。**

> 所以「破坏方块」如果指砸墙挖洞，**原版 Fungal 里并不存在**。用户备忘里验证过的「CNPC 脚本 rayTraceBlock+removeBlock 破坏方块」是要**新增**的能力，不是从 Fungal 移植——可以做，但属于 CNPC 侧自定义，与 Fungal 行为无关。

---

## 4. CNPC 调用方案（务实评估）

### 4.1 模型：走 (c)。CNPC 内无法复刻 Fungal 外观
理由见 TL;DR。CNPC `setModel` 只认预设字符串，无 GeckoLib/OBJ 加载器，无通用模型接口。**本目录提取的 .geo.json / .animation.json / .png 对「纯 CNPC」方案没有直接用途**——它们只有在「方向2：移植实体本体」时才会被 GeckoLib 直接加载（路径见第 5 节）。

### 4.2 若坚持 CNPC（方向1）——行为能复刻到什么程度

CNPC 脚本 API 已具备（反编译确认）：`ICustomNpc.getAi()`(`INPCAi`：移动/导航/破门 `setDoorInteract`/跳跃 `setLeapAtTarget`/游泳)、`getDisplay()`(`INPCDisplay`：`setModel`/`setSize`/`setModelScale`/`setTint`/`setHitboxState`)、`getRole()/getJob()`、事件钩子（tick/attack/died/interact，见 `cnpc_api_extract` 里的 PlayerEvent/ItemEvent 事件类）、世界操作 `IWorld`/`IRayTrace`/`IBlock`（你已验证可 rayTrace+removeBlock）。CNPC 还自带 `AniCrawling` 爬行动画。

| Fungal 行为 | CNPC 可行性 | 怎么做 |
|---|---|---|
| **集群/迁徙** | ✅ 可近似 | 给一群 NPC 挂 tick 脚本：周期性 `world.getNearbyEntities` 找同 faction NPC → 选最近的当 leader → 其余 `npc.getNavigator().navigateTo(leader)` 或设共同路点。或更省事：用 CNPC 自带「Follower/羊群」类 Job + 一个 leader NPC 巡逻。 |
| **Infected 冲刺追击** | ✅ | 设较高 `setWalkingSpeed`，脚本里有目标时临时提速。 |
| **Sporer 孢子光环/感染** | ⚠️ 部分 | tick 脚本对范围内玩家加药水（CNPC 能 `addPotionEffect`）；但 Fungal 的「INFECTION」是 Sona 的机制，CNPC 没有，只能用原版/其它 mod 的效果近似。死亡持续喷孢子：用 died 事件后留一个粒子/伤害源 NPC 或定时器。 |
| **Volatile 格挡/闪避/扑咬处决** | ⚠️ 高难 | 闪避/格挡判定（识别玩家拿枪、正面减伤）要在 tick/damaged 事件里手写大量几何判定（可参考源码 `canDodge`/`canGuard` 的角度公式），工作量大；「扑住玩家骑乘+处决」CNPC 脚本难以让玩家强制 startRiding NPC，近似度低。 |
| **Lurker 跳扑骑乘注毒** | ⚠️ 高难 | 同上，强制玩家骑乘 NPC 在 CNPC 脚本里不易实现，只能用「跳到玩家附近+持续 debuff」近似。 |
| **死亡断裂→爬行** | ❌ 难/只能粗近似 | Fungal 是「同实体切 CREEP 状态+隐藏腿骨+爬行动画」。CNPC 做不到隐藏自定义模型的骨骼。**只能近似**：在 died/damaged 事件里，把该 NPC `setHealth` 不死 + 切到 CNPC 内置 `Crawling` 动画 + 缩小 hitbox + 降速，模拟「被打趴下爬行」；但**没有缺胳膊少腿的视觉**。 |
| **破门** | ✅ | `npc.getAi().setDoorInteract(...)` 原生支持。 |
| **砸墙/挖方块**（原版 Fungal 没有，若想新增） | ✅ | tick 脚本 `world` rayTrace 前方方块 + `removeBlock`（你已验证）。属于新增能力。 |

### 4.3 推荐
- 若目标是**「玩家看到和原版一样的真菌僵尸（带断裂/爬行/专属模型）」** → 只能走**方向2：把 Fungal-Hazard（+Sona-Survival 依赖）移植到 1.21.1 NeoForge**。本目录的资产此时可直接复用（geo/anim/png 1.20.1→1.21.1 GeckoLib 4.x 基本兼容，format_version 1.12.0 通用）。
- 若目标是**「用 CNPC 编剧/兵棋里摆几个能聚团、会冲、会破门的真菌杂兵」**，外观能接受用僵尸皮替身 → 走**方向1：CNPC 脚本复刻行为**，重点做「集群+冲刺+破门(+可选砸墙)」，断裂/孢子/精英技能做粗近似。

---

## 5. 本目录资产清单 + 原 mod 路径

源 assets 根：`Fungal-Hazard-master/src/main/resources/assets/fungal_hazard/`
（移植到 1.21.1 时按相同相对路径放进新 mod 的 `assets/<modid>/` 即可，Java 里 `getModel()`/`getTexture()`/`getAnimation()` 已写死这些相对路径。）

```
_fungal_assets/
├─ 模型_geo/            (原 assets/fungal_hazard/geo/，Bedrock geo, format_version 1.12.0)
│   ├─ infected.geo.json    骨骼: Main/Body/Head/RightArm/LeftArm/RightLeg/LeftLeg/RightForeArm.../MainHandItem/OffHandItem
│   ├─ sporer.geo.json
│   ├─ volatile.geo.json
│   └─ lurker.geo.json      (蜘蛛型, 多腿 leg/body/zui...)
├─ 动画_animations/     (原 assets/fungal_hazard/animations/)
│   ├─ infected.animation.json   含: idle walk walk2 run attack attack2 death | fall fall_to_creep creep creep_idle creep_attack creep_attack2 creep_death
│   ├─ sporer.animation.json     含: idle walk attack attack2 death | fall fall_to_creep creep creep_idle creep_attack creep_attack2 creep_death
│   ├─ volatile.animation.json   含: idle walk sprint attack attack2 parry guard guard_walk start_jump ground stagger start_dodge execution execution_ground riding death
│   └─ lurker.animation.json     含: idle walk start_jump jumping start_riding riding death1 death2
├─ 贴图_textures/       (原 assets/fungal_hazard/textures/entities/)
│   ├─ infected1.png + infected1_glowmask.png   (glowmask = 自发光层, 由 glow=true 渲染器叠加)
│   ├─ sporer.png
│   ├─ volatile.png + volatile_glowmask.png
│   └─ lurker.png
└─ README.md (本文件)
```

注：动画文件里除上述「招式名」外还有大量 `boneXX` 是骨骼轨道名，不是独立动画；权威动画名以各实体 Java 类里的 `RawAnimation` 常量为准（已列在上表）。声音(`sounds/`)、粒子(`particles/spore`)、物品模型等未提取——按需再取。

---

## 6. 相关源码定位（便于后续移植/复刻参考）

- 注册：`init/FungalHazardEntities.java`、客户端渲染注册：`init/FungalHazardClientRegister.java`
- 断裂/爬行核心：`entity/humanoid/AbstractMutilatableZombie.java`（状态+hurt 判定）、`client/renderer/MutilatableZombieEntityRenderer.java`（隐藏骨骼）
- FSM：`entity/ai/fsm/FungalZombieStates.java`（所有状态枚举）、`FungalZombieState.java`、`StateHandler.java`
- 集群/巡逻：`entity/goal/FungalZombiePatrolGoal.java`、`entity/ai/FungalZombieGroundPathNavigation.java`、`api/IPatrolMob.java`/`IPatrolLeader.java`
- GeckoLib 绑定：`client/model/FungalHazardGeoEntityModel.java`（getModel/getAnimation/getTextureResource）、`client/renderer/FungalHazardGeoEntityRenderer.java`、`api/IFungalHazardGeoEntity.java`
- 各怪：`entity/humanoid/{Infected,Sporer,Volatile}Entity.java`、`entity/arachnid/LurkerEntity.java`
- 破门（唯一方块交互）：`AbstractHumanoidFungalZombie.java` 第149行 `MoveThroughVillageGoal`
- Sona 依赖点：grep `com.scarasol.sona` 命中的 6 个文件（见「依赖警告」）
- CNPC API 反编译参考：`D:/ModDevelop/cnpc_api_extract*`；本次额外反编译的 `INPCDisplay`/`INPCAi`/`ICustomNpc` 方法签名见上文 4.2。
