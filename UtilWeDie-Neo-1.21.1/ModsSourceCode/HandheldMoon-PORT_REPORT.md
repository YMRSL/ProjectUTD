# HandheldMoon: LambDynLights → SodiumDynamicLights 改挂报告（已完成）

日期: 2026-06-13
基线: `ModsSourceCode\HandheldMoon-1.21.1neo`（mod_version `1.1.0-fix`，用户当前安装版）。
前置目标: 把 HandheldMoon 的动态光从 **LambDynLights (LDL) 4.8.x** 改挂到 **SodiumDynamicLights (SDDL) 1.0.x**，让它与整合包统一使用的 SDDL / `create-dyn-light-sable` 共用同一套动态光后端（Sodium/Iris 友好）。

---

## 一、结论（置顶，覆盖旧版「不可行」结论）

**可行，已改挂 SDDL，并已构建出可用 jar。**

旧报告（2026-06-12）判定 "NOT FEASIBLE"，理由是 "SDDL 没有逐方块光场回调、无法表达方向性/线光源"。该结论**已被推翻**：

- SDDL 的 `toni.sodiumdynamiclights.SodiumDynamicLights.maxDynamicLightLevel(BlockPos pos, DynamicLightSource source, double currentMax)` 是 SDDL 在区块光照计算时**对每个受影响方块逐格调用**的静态方法。虽然它的*默认*实现是纯径向点光，但只要在它的 **HEAD `@Inject`（cancellable）** 处接管，就能按 `pos` 逐格改写返回值 —— 这正是 1.20.1 时代「方向光改挂动态光」的标准做法，等价于 LDL 的 `lightAtPos` 逐方块回调。
- 因此线光源/锥光源在 SDDL 上**可以表达**：把 query 方块亮度算成「该格相对一条 起点+方向 射线的锥形衰减」（沿用 mod 自带的 `LineLightMath`），而不是相对单点的球形衰减。

这套核心（mixin + HmLight* 系统）由前一 agent 写就，本轮负责**收尾去 LDL 残留 + 修依赖 + 构建**。

---

## 二、改挂后的架构（实际实现）

动态光不再走 LDL 的 `DynamicLightBehavior` / entrypoint，而是：

1. **光源登记**（每客户端 tick 重建）：`lights/HmLightCache`（`@EventBusSubscriber CLIENT`，`ClientTickEvent.Post`）
   扫描 `mc.level`：
   - 持有/穿戴**已通电**手电筒的玩家 → 记入 `selfLightSourceList`（自发光 15）+ `realLightData`（视线锥，range 32）。门槛是 `Utils.isUsingFlashlight`（→ `isPoweredFlashlight` → `MoonlightLampItem.getPowered()==1`）。**关闭手电筒的玩家不登记** → 无任何光（见 §五 bug 修复）。
   - `FullMoonEntity`：绑灯时 → 方向锥（用灯的 yaw/pitch + `getLampLuminance()`）；非绑灯（小月亮）→ 全向点光（range 18）。
   - 光源对象用的是 SDDL 自己给 vanilla 实体/玩家实现的 `DynamicLightSource`（SDDL 的 `EntityMixin`/`PlayerEntityMixin` 已实现该接口）。我们**不调用** `addLightSource`，只覆写 SDDL 为它们算出的逐格亮度。

2. **逐格亮度接管**：`mixin/sdl/DirectionalLightMixin`（`@Mixin(SodiumDynamicLights.class, remap=false)`）
   - `@Inject(HEAD, cancellable)` 进 `maxDynamicLightLevel` → `HmLightHandler.entityLight(...)`：按 `realLightData` 算锥/点亮度，大于当前值就 `cir.setReturnValue`。
   - `@Inject(HEAD, cancellable)` 进 `getLivingEntityLuminanceFromItems` → `HmLightHandler.selfLight(...)`：登记过的源返回 15（第一人称自发光）。

3. **自发光字段同步**：`mixin/sdl/SodiumDynamicLightsMixin`
   - `@Inject(HEAD)` 进 `updateTracking`，对登记过的源用反射把 SDDL 加在 `net.minecraft.world.entity.Entity` 上的包私字段 `sodiumdynamiclights$luminance` 置 15。

4. **区块重光调度**：`lights/HmLightRefresh`（`@EventBusSubscriber CLIENT`）
   光源移动/转向/消失时，调 `SodiumDynamicLights.scheduleChunkRebuild(levelRenderer, sectionPos)` + `source.sdl$resetDynamicLight()` 触发周边 section 重新查询 `maxDynamicLightLevel`（因为锥依赖朝向，SDDL 自己不会因为「只转头」而重光）。

5. **锥数学**：`util/LineLightMath`（纯数学，已去 LDL import）。

SDDL API 签名已对 `_inspect_sable\sdl.jar` 用 `javap` 逐一核对一致（`maxDynamicLightLevel(BlockPos,DynamicLightSource,double)`、`getLivingEntityLuminanceFromItems(LivingEntity)→int`、`updateTracking(DynamicLightSource)`、`scheduleChunkRebuild(LevelRenderer,BlockPos)`、`DynamicLightSource.sdl$resetDynamicLight()`、`Entity#sodiumdynamiclights$luminance`）。

---

## 三、本轮实际改动清单

### 源码（去 LDL 残留）
- `util/LineLightMath.java`：删 `import dev.lambdaurora...DynamicLightBehavior` + 删依赖它的死方法 `getBlockVolume`。
- `client/HandheldMoonClient.java`：删 `HandheldMoonDynamicLightsInitializer` import 及 `startWordTick`（其只调 `updatePlayerBehaviors/updateFullMoonEntityBehaviors`，已被 HmLightCache 的 tick 订阅取代）；删随之失效的 `ClientTickEvent` import。
- `block/FullMoonBlock.java`：删 `Initializer` import + 仅委托的 `onPlace`/`onRemove` 两个 override（其客户端分支只调 `ensure/removeFullMoonBehaviorAt`）。
- `block/FullMoonBlockEntity.java`：删 `Initializer` import + `setLevel` 里客户端 `addFullMoonBehavior` 分支 + `setRemoved` override（只调 `removeFullMoonBehavior`）。满月实体的生成/绑定逻辑保留。
- `block/MoonlightLampBlockEntity.java`：删 `Initializer` import + 3 个 setter 里的 `syncLampBehavior`（保留 `ClientUtils.syncMoonlightLampBlock`）；`clientTick` 改为 no-op（灯的光由其绑定的 `FullMoonEntity` 经 SDDL 自动追踪，无需逐 tick 同步）。
- `compat/tacz/ModMixinPlugin.java`：删 `shouldApplyMixin` 里针对 `mixin.lamb.LambDynLightsMixin` 的特例分支及 `isClassLoaded` 辅助（lamb mixin 已不存在）；保留插件本体（mixins.json 仍引用它）。
- `event/handler/ShaderRayEvent.java`：整文件原本即全注释，未动（其中 `//import ...Initializer` 是注释，不参与编译）。

> 注：清单里旧任务书提到要删的 `lights/HandheldMoonDynamicLightsInitializer.java`、`*LineLightBehavior.java`、`mixin/lamb/*` —— 这些**前一 agent 已删**，本轮只清掉对它们的悬空引用。

### 资源
- `src/main/resources/handheldmoon.mixins.json`：`mixins` 数组删 `lamb.DynamicLightingEngineMixin`/`lamb.LambDynLightsMixin`/`lamb.SpatialLookupDeferredEntryMixin`，加 `sdl.DirectionalLightMixin`/`sdl.SodiumDynamicLightsMixin`（server/common 端，因 SDDL 依赖为 BOTH）。`client` 数组保留 `client.EntityRendererMixin`。`plugin`（tacz `ModMixinPlugin`）保留，对 sdl mixin 默认返回 true，正常应用。
- `src/main/resources/META-INF/neoforge.mods.toml`：删 `[modproperties...]` 下 `"lambdynlights:initializer" = ...` 整段 entrypoint；依赖 `lambdynlights_runtime`（required BOTH）→ 改为 `sodiumdynamiclights`（required BOTH）。
- `assets/handheldmoon/dynamiclights/item/moonlight_lamp.json`：保持 `powered:0 → luminance:0`（见 §五，**未回退**）。该 JSON 是 LDL/SDDL 通用的物品光源格式，SDDL 同样读取（其自带 `assets/sodiumdynamiclights/dynamiclights/item/*.json` 即同结构）。

### 构建（build.gradle）
- 删全部 LDL 依赖（api compileOnly + runtime implementation）。
- 改用**本地 jar 作 compileOnly**（不打包进产物，运行期由整合包提供），放在工程 `libs/`：
  - `sodiumdynamiclights-1.0.10.jar`（源自 `_inspect_sable\sdl.jar`）
  - `curios-neoforge-9.5.1.jar`、`cloth-config-15.0.140-neoforge.jar`、`tacz-neoforge-1.21.1-1.1.8-hotfix-r1.jar`、`sable-neoforge-1.21.1-1.2.2.jar` + 从 sable jarjar 解出的 `sable-companion-common-1.21.1-1.6.0.jar`（均拷自整合包 `mods\`）。
- **删掉会 403 / 非必要的 CurseMaven 与 Modrinth 编译依赖**：`carry-on`、`sodium-extra`、`mafglib`、`tweakerge`、`tacz(curse)`、`create`、`create-aeronautics`、`veil`、`sodium`、`iris`、`jei`。核实源码**未在编译期 import** 这些（veil 仅出现在全注释的 `ShaderRayEvent`；create/iris/sodium/jei 等为纯运行期 compat）。编译第三方依赖实测仅需：SDDL、tacz、curios、cloth-config、sable(+companion)。
  - 原 `files("libs/sable-neoforge-1.21.1-1.1.3.jar")` 指向**不存在**的文件（工程无 `libs/`），亦一并修正为现存的 1.2.2。

---

## 四、构建结果

- 环境：`JAVA_HOME = Eclipse Adoptium jdk-21.0.9.10-hotspot`；系统 Gradle 8.9（`D:\Program daily\GradleBuilder\gradle-8.9`）；NeoForge 21.1.227 / MC 1.21.1。
- `gradle compileJava --no-daemon` → **BUILD SUCCESSFUL**（仅 javac deprecation/unchecked 提示，无错误）。
- `gradle build -x test --no-daemon` → **BUILD SUCCESSFUL**（processResources / mixin AP / jar 全通过）。
- 产物：`build\libs\HandheldMoon-neoforge-1.21.1-1.1.0-fix.jar`。
- jar 内核对：含 `mixin/sdl/DirectionalLightMixin`、`mixin/sdl/SodiumDynamicLightsMixin`、`mixin/client/EntityRendererMixin`、`lights/Hm*`；**无任何 `lamb/*`、`*Initializer*`、`*LineLightBehavior*`**；`neoforge.mods.toml` 已展开为依赖 `sodiumdynamiclights`、无 LDL entrypoint；`handheldmoon.mixins.json` 内容正确。

### 交付 jar
已复制为：
`...\1.21.1-NeoForge_21.1.233\mods_WS7待入包\HandheldMoon-neoforge-1.21.1-sdl.jar`

---

## 五、保留的 Bug 修复（手电筒关闭不发光）

- `assets/handheldmoon/dynamiclights/item/moonlight_lamp.json` 的 `match.components.handheldmoon:powered = 0` 规则 luminance **= 0**（关闭不发手持球光），**已确认未回退**。
- 改挂 SDDL 后该「关闭不发光」语义有**双重保障**：
  1. 物品光源 JSON：powered=0 → luminance 0（SDDL 读取此格式）。
  2. 方向锥：`HmLightCache` 只在 `Utils.isUsingFlashlight`（要求 `getPowered()==1`）为真时才登记玩家光源 → 关灯时既无锥也无自发光。
- 灯**方块**（非手持）通断电沿用原语义：`setLampState(..., powered?15:1)`，关时降到 luminance 1（极弱）而非 0——此为原 port 行为，未改动。

---

## 六、游戏内测试法

1. **手电筒关闭不发光（核心）**：手持 `moonlight_lamp`，不开。脚下/周围方块**无**任何动态提亮。
2. **手电筒开启发光**：按 `FLASHLIGHT_SWITCH` 点亮 → 出现**沿视线的光锥**，墙面有光斑，转头光斑跟随（HmLightRefresh 触发重光）；关掉立即消失。
3. **月灯方块**：放置 `moonlight_lamp` 方块并通电 → 朝其朝向投出光锥（由绑定的 FullMoonEntity 提供）。
4. **满月实体/方块**：满月实体（小月亮）应有约 18 格全向点光；绑灯实体投锥。
5. **与 SDDL / create-dyn-light-sable 共存（关键回归点）**：整合包已装 `sodiumdynamiclights-...` 与 `create-dyn-light-2.3.1-sodium-sable.jar`。三者共用 SDDL 后端，逐格取 max 叠加，应**互不吞光**；确认装/卸 HandheldMoon 不影响 create-dyn-light 自身的动态光。
6. **专用服务器**：SDDL 依赖声明为 required BOTH，sdl mixin 在 common `mixins` 数组——运行期 SDDL jar 须同时在服务端（整合包已满足），否则 mixin 找不到 `SodiumDynamicLights` 目标类。若仅客户端装 SDDL 而服务端缺，会启动报错；整合包部署时确保两端一致即可。

---

## 七、风险与备注

- **编译依赖改为本地 jar**：工程现自带 `libs\*.jar`（compileOnly），构建离线可复现，不再受 CurseMaven 403 影响。若日后升级 SDDL/tacz/sable/curios/cloth 版本，需同步替换 `libs\` 下对应 jar 并核对 API 签名。
- **SDDL 依赖 side = BOTH**：与旧 `lambdynlights_runtime` 一致。光照逻辑全部 `@Dist.CLIENT` 门控，服务端不主动用 SDDL，但因 sdl mixin 放在 common 数组，服务端仍需有 SDDL jar（整合包 BOTH 安装即可）。若未来要支持「服务端无 SDDL」，可改为 `side = "CLIENT"` 并把两个 sdl mixin 移到 mixins.json 的 `client` 数组。
- **反射取 `sodiumdynamiclights$luminance`**：该字段由 SDDL 的 `EntityMixin` 加到 vanilla `Entity` 上，运行期存在；若 SDDL 改字段名，`SodiumDynamicLightsMixin` 的静态初始化会抛错（带明确信息）。已对 1.0.x 核对字段名一致。
- 未触碰：`mods\` 正式目录、其它 `ModsSourceCode` 子工程、蓝图文档。
