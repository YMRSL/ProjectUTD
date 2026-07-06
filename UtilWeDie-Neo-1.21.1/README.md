# ProjectUTD → 1.21.1 NeoForge 迁移计划

> 源端: `UntilWeDieOriginal/.minecraft/versions/1.20.1-Forge_47.4.6/mods/` (192项)
> 目标: Minecraft 1.21.1 / NeoForge
> 日期: 2026-06-04

---

## 📊 总览

| 分类 | 数量 | 文件夹 |
|------|------|--------|
| ✅ 直接下载 | ~130 | `01-direct-download/` |
| 🔗 开源移植 | 9 | `02-open-source-port/` |
| 🔧 反编译升级 | 2 | `03-decompile-upgrade/` |
| ⚠️ Fabric→Connector | 3 | `04-fabric-connector/` |
| 🔧 本地附属 | ~14 | `local-addons/` |
| 🗑️ 丢弃 | ~25 | `05-discarded/` |

---

## 📥 01 — 直接下载最新版本

这些 mod 在 Modrinth/CurseForge 上已有 1.21.1 NeoForge 版本。逐个下载替换即可。

### 前置库 (Libraries)
| Mod | 1.21.1 下载源 |
|-----|-------------|
| Architectury | Modrinth `architectury-api` |
| Cloth Config | Modrinth `cloth-config` |
| GeckoLib | Modrinth `geckolib` |
| KotlinForForge | Modrinth `kotlin-for-forge` |
| Rhino | Modrinth `rhino` |
| Curios | Modrinth `curios` |
| Balm | Modrinth `balm` |
| CreativeCore | Modrinth `creativecore` |
| Kiwi | Modrinth `kiwi` |
| Coroutil | Modrinth `coroutil` |
| SmartBrainLib | Modrinth `smartbrainlib` |
| Patchouli | Modrinth `patchouli` |
| Collective | Modrinth `collective` |
| PandaLib | Modrinth `pandalib` |
| OELib | Modrinth `oelib` |
| Fzzy Config | Modrinth `fzzy-config` |
| Sodium Options API | Modrinth `sodium-options-api` |
| Player Animation Lib | Modrinth `playeranimator` |
| LDLib | Modrinth `ldlib` |
| Citadel | Modrinth `citadel` |
| GPUTape | Modrinth `gputape` |
| Sinytra Connector | Modrinth `sinytra-connector` |
| Fabric API | 随 Connector 更新 |
| FTB Library | CurseForge `ftb-library-forge` |
| FTB Teams | CurseForge `ftb-teams-forge` |
| YACL | Modrinth `yacl` (原 `yet_another_config_lib_v3`) |
| ConnectorExtras | Modrinth `connector-extras` |
| Framework (MrCrayfish) | Modrinth/CurseForge |
| RarityCore | CurseForge |

### 优化 & 光影
| Mod | 下载源 |
|-----|--------|
| Embeddium | Modrinth `embeddium` |
| Iris | Modrinth `iris` (替代 Oculus) |
| ModernFix | Modrinth `modernfix` |
| EntityCulling | Modrinth `entityculling` |
| ImmediatelyFast | Modrinth `immediatelyfast` |
| Radium | Modrinth `radium` |
| C2ME NeoForge | Modrinth `c2me-neoforge` |
| Chloride | Modrinth `chloride` |
| Flerovium | Modrinth `flerovium` |
| ColorWheel | Modrinth `colorwheel` |
| ColorWheel Patcher | Modrinth `colorwheel-patcher` |
| CreateBetterFPS | Modrinth `createbetterfps` |
| AllTheLeaks | Modrinth |
| Polymorph | Modrinth `polymorph` |
| CustomSkinLoader | Modrinth `customskinloader` |
| Rubidium Extra | Modrinth `rubidium-extra` |
| Spark | Modrinth `spark` |

### UI/信息
| Mod | 下载源 |
|-----|--------|
| JEI | Modrinth `jei` |
| Jade | Modrinth `jade` |
| JadeAddons | Modrinth |
| AppleSkin | Modrinth `appleskin` |
| Xaero's Minimap | Modrinth `xaeros-minimap` |
| Xaero's World Map | Modrinth `xaeros-world-map` |
| Ping Wheel | Modrinth `ping-wheel` |
| Smart Key Prompts | Modrinth `smart-key-prompts` |
| A Good Place | Modrinth `a-good-place` |
| WATUT | Modrinth `what-are-they-up-to` |
| EnhancedVisuals | Modrinth `enhancedvisuals` |
| Observable | Modrinth `observable` |
| CuteWords | CurseForge `cute-words-chat-replacer` |
| ETF | Modrinth `entitytexturefeatures` |
| EMF | Modrinth `entity-model-features` |

### Create 生态 (全系 ✅)
| Mod | 下载源 |
|-----|--------|
| Create | Modrinth `create` |
| Create: New Age | Modrinth `create-new-age` |
| Create: Crafts & Additions | Modrinth `createaddition` |
| Create: Diesel Generators | Modrinth `create-diesel-generators` |
| Create: Treadmill | Modrinth `createtreadmill` |
| KubeJS | Modrinth `kubejs` |
| KubeJS Create | Modrinth `kubejs-create` |
| CraftTweaker | Modrinth `crafttweaker` |
| MultiBlocked2 | Modrinth `multiblocked2` |

### 战斗 & 怪物
| Mod | 下载源 |
|-----|--------|
| SuperbWarfare 卓越前线 | Modrinth `superb-warfare` |
| Spore 真菌感染 | Modrinth `fungal-infectionspore` |
| TACZ 永恒枪械 | GitHub `MUKSC/TACZ-1.21.1` (fork) |
| Naturalist (替代 alexsmobs) | Modrinth `naturalist` |

### NPC
| Mod | 下载源 |
|-----|--------|
| CustomNPCs | CurseForge `customnpcs-unofficial` |

### 趣味 & 装饰 & 动作
| Mod | 下载源 |
|-----|--------|
| Farmer's Delight | Modrinth `farmers-delight` |
| Automobility | Modrinth `automobility` |
| Immersive Melodies | Modrinth `immersive-melodies` |
| Immersive Paintings | Modrinth `immersive-paintings` |
| Whimsy Deco | Modrinth `whimsy-deco` |
| Kaleidoscope Tavern | Modrinth `kaleidoscopetavern` |
| Kaleidoscope Compat | Modrinth `kaleidoscope-compat` |
| Kaleidoscope Doll | Modrinth `kaleidoscope-doll` |
| Kaleidoscope Cookery | CurseForge |
| NetMusic | Modrinth `netmusic-loginneed` |
| LateGameGolems | CurseForge `late-game-golems` |
| AdorableHamsterPets | CurseForge |
| NotableBubbleText | CurseForge |
| ParCool | Modrinth `parcool` |
| CarryOn | Modrinth `carry-on` |
| CarryOnExtend | Modrinth |
| SimpleVoiceChat | Modrinth `simple-voice-chat` |
| SimpleRadio | CurseForge |
| PlayerRevive | Modrinth `playerrevive` |
| DisplayDelight | Modrinth `display-delight` |
| Leawind Third Person | Modrinth `leawind-third-person` |
| Paraglider | Modrinth `paragliders` |
| LookInMyEyes | CurseForge `lookinmyeyes` |
| GallantTirelessAltruist | CurseForge |
| Immersive Vehicles | Modrinth |
| Kaminari Motor Work | CurseForge |
| mvo76 | CurseForge |
| ThirstWasTaken | CurseForge |
| HiddenNames | CurseForge |
| VisualKeybinder | CurseForge |
| BetterLooting | Modrinth |
| FTB Quests | CurseForge `ftb-quests-forge` |

### 工具 & 探索
| Mod | 下载源 |
|-----|--------|
| WorldEdit | Modrinth `worldedit` |
| Forgematica | Modrinth `forgematica` |
| MaFgLib | Modrinth `mafglib` |
| HealingCampfire | Modrinth `healing-campfire` |
| MineBackup | Modrinth `minebackup` |
| Gravestone | Modrinth `gravestone-mod` |
| InControl | Modrinth `in-control` |
| HardcoreTorches | Modrinth `hardcore-torches` |
| SearchCarefully | GitHub `Yanbwe/SearchCarefully` |
| Flashback | Modrinth (1.21.1 原版) |
| ParticleRain | Modrinth `particle-rain` |
| ItemPhysic | Modrinth `itemphysic` |
| SodiumDynamicLights | Modrinth `sodium-dynamic-lights` |
| SoundPhysicsRemastered | Modrinth `sound-physics-remastered` |
| ExplosionOverhaul | Modrinth `explosion-overhaul` |

---

## 🔗 02 — 开源移植

### Sona 生态链 (3 个 mod)
- **Sona**: GitHub `Scarasol/Sona-Survival-101` → fork `1.20.1` 分支, 升级到 1.21.1 NeoForge
- **Fungal Hazard**: 依赖 Sona → 等 Sona 移植完成后适配
- **Zombie Survival Kit**: 依赖 Sona → 同上
- **优先级**: 🔴 高 (阻塞 3 个核心 mod)

### 战斗/机制 (4 个)
- **TACZ Unidict 铳械协议**: 本地 YMR 修复版 → 反编译适配 `MUKSC/TACZ-1.21.1`
- **survival_instinct**: 闭源反编译, 功能简单(装备), 美术资产多
- **just_barricades**: GitHub `Tejty/Just_Barricades` → 直接升级
- **Mobreaker 坚壁重构**: CurseForge `mobreaker` 闭源 → 反编译(只需僵尸破坏方块逻辑)
- **优先级**: 🟡 中

### 工具/QOL (5 个)
- **ftbquestlocalizer**: GitHub `Litchiiiiii/FTB-Quests-Localizer`
- **Sculk Horde 幽匿部落**: 开源, 移植到 1.21.1
- **ModernMayhem**: GitHub `TKG112/mm`
- **Hourglass**: GitHub `DuckyCrayfish/hourglass`
- **BlockZ**: GitHub `yitian77/BlockZ`
- **优先级**: 🟢 低

---

## 🔧 03 — 反编译升级 (闭源)

| Mod | 方案 |
|-----|------|
| **Doomsday Decoration** | MC百科 class/17818, CurseForge `doomsday-decoration`, 闭源。仅装饰模型, 附属 `doomsday_functionality` 开源。反编译→提取模型→重写 NeoForge 1.21.1 |
| **Mobreaker** | CurseForge `mobreaker`, 闭源。功能简单(僵尸追击时破方块)。反编译→提取逻辑→重写 |

---

## ⚠️ 04 — Fabric → Connector 桥接

| Mod | 说明 |
|-----|------|
| **CNPC-Gecko-Addon** | Fabric 1.21.1 开源 → 通过 Sinytra Connector 运行 |
| **SeriousPlayerAnimations** | GitHub `McVader34/Serious-Player-Animations` Fabric → Connector |
| **Axiom** | v5.4.2 Fabric 1.21.1 → Connector (或等 NeoForge 版) |

---

## 🔧 本地附属 (local-addons/)

这些是本地制作/修改的 mod, 有源码可直接升级:

| Mod | 来源 |
|-----|------|
| attackablecreatetrain | 本地源码 |
| farmer_misery | 本地源码 |
| itemnamecatch | 本地源码 |
| vehicleload | 本地源码 |
| utd_doomsday_patch | 本地源码 (核心!) |
| holdmyitems (YMRFixed) | 本地源码 |
| HandheldMoon (YMRFixed) | 本地源码 |
| riautomobility (YMRFied) | 本地源码 |
| firstpersonfoodeating | 本地源码 → 直接升级 1.21.1 NF |
| create-dyn-light | 已是 1.21.1 |
| spore (YMRfix) | → 用官方 fungal-infectionspore 替代 |

---

## 🗑️ 05 — 丢弃清单

车万女仆全套(7) · TACZ扩展(gundb/lrtactical/taczjs/sentrymechanicalarm/taczxgunlightsaddon/gunsmithlib) · alexsmobs(→Naturalist) · BBS(→Flashback) · ferritecore · Tritium · pomkots(2) · doday · immersive_furniture · crafttakestime · fallingtrees · dyairdrop · Locks · RopeBridge · Warium · WorldEditCUI · backstabreforged · Ravenous

---

## 🗺️ 推荐执行顺序

1. **Phase 1: 基础设施** — NeoForge + 前置库 + 优化栈 + UI (纯下载, 零风险)
2. **Phase 2: 核心玩法** — Create 生态 + Spore + SuperbWarfare + TACZ fork + FarmersDelight (下载 + GitHub fork)
3. **Phase 3: 辅助内容** — Fun/装饰/动作/载具/工具 mod (批量下载 CurseForge/Modrinth)
4. **Phase 4: 移植** — Sona 链 → survival_instinct → just_barricades → 幽匿部落 → ModernMayhem...
5. **Phase 5: 反编译** — Doomsday Decoration → Mobreaker (最后攻克)
6. **Phase 6: 本地附属** — 升级本地源码
7. **Phase 7: Connector** — Fabric mod 桥接验证
