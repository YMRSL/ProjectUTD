# FirstPersonFoodEating — NeoForge 1.21.1 移植报告

ProjectUTD WS-7 / 升级轨道。源 1.20.1 Forge → 目标 NeoForge 1.21.1。**构建成功，jar 已产出并入包待入目录。**

---

## 1. 结构判断（摸清的真实实现）

源码真正的 mod 工程在 `D:\ModDevelop\ProjectUTD\FirstPersonFoodEating\forge_1_20_1_port\`（同目录还混了
`Relative\`(参考别人的 Thirst-Mod/TACZ/EP_GUN 美术包)、`capture_output\`、`CrashReport\`、`4685...@3@15`
加密残留、`Animation\`/`tools\` 等 codex 工作文件——**均非 mod 源码，已忽略**）。

- **modid**: `firstpersonfoodeating`；**包名/group**: `io.github.ymrsl.firstpersonfoodeating`
- **主类**: `FirstPersonFoodEatingMod`（注册 COMMON config + ModItems + ModMobEffects）
- 53 个 `.java`(约 11200 行) + 完整 resources(geo_models/animations/display/scripts/sounds/textures/lang/packs)

### 动画实现方式（关键结论：**不是 player-animation-lib，也不是 GeckoLib**）
这是一套**完全自带、自实现的渲染+动画引擎**，零外部动画库依赖：
- **自写 GeckoLib 风格渲染器**：`FoodGeoModel`(解析 `.geo.json` bedrock 模型自己出顶点)、
  `FoodFirstPersonGeoRenderer`(第一人称手部姿态/弹簧跟随/跑步摇晃)、
  `FoodGeoItemRenderer`(继承 `BlockEntityWithoutLevelRenderer`，即 BEWLR/自定义物品渲染器)。
  全部走原版 Mojang API(PoseStack/MultiBufferSource/RenderType/VertexConsumer)。
- **LuaJ 脚本状态机**：`client/script/statemachine/Lua*` + `client/script/runtime/*`，用 Lua 脚本
  (`assets/.../scripts/*.lua`)驱动手部动画状态机(draw/use/put_away/inspect)。依赖 **luaj**(纯 Java Lua VM)。
- **第一人称挂钩**：Forge `RenderHandEvent`(NeoForge 同名)里取消原版手并改渲染自家 geo 模型 + 玩家手臂。
- 物品使用：标准 `Item.use/onUseTick/finishUsingItem/releaseUsing` + `UseAnim`，配合
  `ConsumableUseLockController`(用 `getPersistentData` 锁定使用)。

> 蓝图里"player-animation-lib / geckolib(4.8.4) 二选一"的预判**不成立**——本包二者都没用。
> 目标包里虽有这些库的 jar，但本 mod 不引用它们。

---

## 2. 依赖

| 依赖 | 处理方式 |
|---|---|
| **luaj**(脚本动画 VM) | 旧成品 jar 的 jar-in-jar 里是 `luaj-jse-3.0.1.jar`(figura 变体仅 Connector 需要)。已抽出放 `neoforge-1.21.1/libs/luaj-jse-3.0.1.jar`，build.gradle 用 `shade files(...)` **shade 进最终 jar**(344 个 luaj class)。**绕开 JitPack FiguraMC 依赖，离线可构建。** |
| GeckoLib / player-animation-lib | **不需要**(自带实现) |
| Thirst(`dev.ghen.thirst`, Forge) | 见下"风险/不对版"——已降级为 no-op stub |

构建栈：ModDevGradle `net.neoforged.moddev 2.0.107` + neoForge `21.1.233` + JDK21 + 系统 Gradle 8.9。
（前一 agent 已搭好骨架 build.gradle/settings.gradle/gradle.properties，本次仅把 luaj 源改成本地 jar。）

---

## 3. 主要改动（1.20.1 Forge → 1.21.1 NeoForge）

数字为发现时的编译错误规模，最终 **0 error**：

- **事件总线注解**(32 处): `@Mod.EventBusSubscriber` → 独立 `@EventBusSubscriber`(`net.neoforged.fml.common`)；
  `Bus.FORGE` → `Bus.GAME`，`Bus.MOD` 保留。
- **Tick 事件**: `TickEvent.PlayerTickEvent`(END) → `PlayerTickEvent.Post`(`event.getEntity()`)；
  `TickEvent.ClientTickEvent`(END) → `ClientTickEvent.Post`。
- **ResourceLocation**(12 处): `new ResourceLocation(ns,path)` → `ResourceLocation.fromNamespaceAndPath(...)`
  / `withDefaultNamespace(...)`。
- **注册系统**: `ForgeRegistries.ITEMS/MOB_EFFECTS` + `RegistryObject` →
  `DeferredRegister.createItems` / `DeferredRegister.create(Registries.MOB_EFFECT,...)`，
  返回 `DeferredItem`/`DeferredHolder`(`.isPresent()` → `.isBound()`)。
- **MobEffect → Holder<MobEffect>**: `ModMobEffects` 暴露 `Holder<MobEffect>`；
  `MobEffectInstance`/`player.getEffect/hasEffect/removeEffect`/`instance.getEffect()` 全切 `Holder`，
  类别判断改 `effect.value().getCategory()`。
- **主类入口**: `@Mod` 构造 `(IEventBus modEventBus, ModContainer modContainer)`；
  `FMLJavaModLoadingContext.get().getModEventBus()` 去掉，config 改 `modContainer.registerConfig(...)`。
- **物品 API**: `Item.getUseDuration(ItemStack)` → `getUseDuration(ItemStack, LivingEntity)`(含 ItemStack 同名调用)；
  `stack.isEdible()` → `stack.has(DataComponents.FOOD)`。`UseAnim` 在 1.21.1 **仍叫 UseAnim**(未改名)。
- **FoodProperties.Builder**: `alwaysEat()` → `alwaysEdible()`，`saturationMod()` → `saturationModifier()`。
- **ItemStack NBT → DataComponents**(核心数据迁移): `FoodStackData` 原本 `stack.getOrCreateTag()/getTag()`
  存全部 profile(食物 id/营养/效果/口味文案/耐久/use 选择器…)。1.21 取消了 ItemStack NBT，改用
  **`DataComponents.CUSTOM_DATA`(`CustomData`)** 包裹一个 CompoundTag。新增
  `mutateRoot(stack, Consumer<CompoundTag>)` 做 read-modify-write，**900 行序列化逻辑零改动**，
  仅 2 个 helper + 12 个 setter 套壳。
- **顶点 API**(2 处, geo 渲染核心): `VertexConsumer.vertex(matrix,x,y,z).color().uv().overlayCoords().uv2().normal().endVertex()`
  → `addVertex(matrix,x,y,z).setColor().setUv().setOverlay().setLight().setNormal()`(无 endVertex)。
- **GUI overlay 事件**: `RenderGuiOverlayEvent.Post` → `RenderGuiEvent.Post`，
  `getPartialTick()` 现返回 `DeltaTracker` → `.getGameTimeDeltaPartialTick(false)`。
- **partial tick**: `Minecraft.getFrameTime()` → `getTimer().getGameTimeDeltaPartialTick(true)`(4 处)。
- **DistExecutor**(已删) → `if (FMLEnvironment.dist == Dist.CLIENT)`。
- **资源包 API**(FoodPackLoader 整改): `Pack.readMetaAndCreate` 签名变为
  `(PackLocationInfo, ResourcesSupplier, PackType, PackSelectionConfig)`；
  `PathPackResources` 迁到 `net.minecraft.server.packs` 且构造 `(PackLocationInfo, Path)`；
  **`DelegatingPackResources` 在 NeoForge 1.21.1 已移除** → 改为**每个外部食物包各注册一个 Pack**
  (经 `AddPackFindersEvent.addRepositorySource`)，行为等价。
- **玩家皮肤**: `player.getSkinTextureLocation()` → `player.getSkin().texture()`。
- **效果伤害事件**: `LivingHurtEvent`(已删) → `LivingDamageEvent.Pre`；
  `MobEffectEvent.Applicable` 的 `event.setResult(Event.Result.DENY)` → `setResult(Result.DO_NOT_APPLY)`。
- 元数据: `mods.toml` → `META-INF/neoforge.mods.toml`(modId 依赖 neoforge `[21.1,)` + minecraft `[1.21.1,1.21.2)`)；
  `pack.mcmeta` pack_format=34。

运行时挂钩(全部 NeoForge 正确)：BEWLR 经 `IClientItemExtensions.getCustomRenderer()`；
资源重载经 `RegisterClientReloadListenersEvent`；外部包经 `AddPackFindersEvent`。

---

## 4. 产出 jar 与测试法

- **工程**: `D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\ModsSourceCode\FirstPersonFoodEating\neoforge-1.21.1\`
- **成品 jar**: `build\libs\firstpersonfoodeating-0.1.0-neoforge-1.21.1.jar`(3.4 MB)
  - 已复制到入包目录:
    `...\.minecraft\versions\1.21.1-NeoForge_21.1.233\mods_WS7待入包\firstpersonfoodeating-0.1.0-neoforge-1.21.1.jar`
  - 内含: 129 mod class + 344 luaj class(shaded) + neoforge.mods.toml + 117 个 geo/anim/sound 资源
    + 内置 `default_food_pack.zip`(1.5 MB)。
- **构建命令**(JAVA_HOME 指 JDK21):
  `gradle build -x test --no-daemon`（`gradle` = `D:\Program daily\GradleBuilder\gradle-8.9`）

### 怎么测（吃东西看第一人称手部动画）
1. 把 jar 放进整合包 `mods\`，进 1.21.1 NeoForge 存档。
2. 创造模式物品栏找 **First Person Food Eating** 标签页 → 取出 `pack_food`(默认呈现 `i_bang_a` 等 profile)。
3. 第一人称手持，**右键长按使用**：应看到自家 geo 模型 + 玩家手臂播放 draw/use 动画(LuaJ 状态机驱动)，
   伴随 `ep_gun/...` 音效；使用完成触发营养/效果，注射类(zhenji/syringe)有屏幕白闪 overlay。
4. 按检视键(ClientKeyMappings.INSPECT，默认见键位)看 inspect 动画。
5. 外部食物包：游戏目录 `FoodsPack\` 放 zip(含 `foodpack.meta.json`)，资源包列表里会出现各包，自动加载。

> 本次仅做到**编译 + 构建通过**(0 error / 31 deprecation warning)。**未实机启动验证**——
> 渲染/Lua 脚本是运行期行为，建议入包后跑一次客户端确认手部动画与音效。

---

## 5. 美术/资源不对版登记（功能优先，原样保留）

- 借用了别人的素材包(`Relative\EP_GUN_FuShuBao...` 等),**包名与内容不对版是已知现象**：
  - 音效目录全在 `assets/firstpersonfoodeating/sounds/ep_gun/...`(原是枪 mod 的"附属包"音效路径),
    被复用作食物/医疗物品音(如 `guantou/`=罐头、`bengdai/`=绷带、`zhenji/`=注射器),路径名看着像枪械。
  - 物品/模型名是拼音(bang/bengdai/dai/guan/guantou/he/jia/jijiubao/ping/yaoping/zhenji),
    与"食物"语义弱关联,属原作者命名,未改。
- 资源**全部原样搬运**，路径未报缺失(geo/anim/display/textures/scripts/sounds + default_food_pack.zip 齐全),
  **无需补占位**。

---

## 6. 风险 / 后续

1. **Thirst 联动已降级为 no-op**(`ThirstCompatBridge`)。原代码用反射 + Forge `Capability<IThirst>`
   (`dev.ghen.thirst` 这个 Forge mod)。**NeoForge 1.21.1 整套删掉了 Forge capability 系统**，且目标整合包
   装的是**另一个**渴值 mod(`ThirstWasTaken`,API 完全不同)——原 bridge 无论如何都接不上。已重写为安全空实现
   (`isAvailable()=false`,保留公开方法签名,所有调用点的 thirst 分支自然短路)。**饥渴联动功能在本包失效**,
   其余功能不受影响。若需 NeoForge 原生渴值联动,在此类内对 ThirstWasTaken 的 API 重实现即可,不必动调用点。
2. **未实机测试**(配额/无头环境)。编译期 0 error,但 geo 渲染矩阵、LuaJ 脚本路径、BEWLR 注册等运行期行为
   建议入包后跑客户端确认。
3. **BEWLR/`IClientItemExtensions.initializeClient` 标了 deprecated-for-removal**(31 个警告之一)——
   1.21.1 仍可用,未来大版本可能要迁到新物品渲染管线。当前不影响。
4. `default_food_pack.zip` 由旧 Forge 构建的 PowerShell 脚本(`bundleFoodArtPack`)生成;本 NeoForge 工程
   **直接复用 resources 里已有的 zip**(未接该脚本)。如需重新生成默认包,需另行处理那段美术打包流程。
5. `mod_version` 仍是 `0.1.0`(→ jar 名 `0.1.0-neoforge-1.21.1`),沿用旧版本号。

---

## luaj 冲突修复（2026-06-13，JPMS 包冲突回归）

### 诊断
入包后游戏启动崩溃:
```
java.lang.module.ResolutionException: Modules luaj.core.figura and firstpersonfoodeating
export package org.luaj.vm2.compiler to module jei
```
根因:**Axiom(5.4.2)和本 mod 都把 luaj 库 shade 在顶层包 `org/luaj/vm2/**`**。
- Axiom jar:`org/luaj/` 共 **356** 个 class(含 `org/luaj/vm2/compiler`)。
- 本 mod 旧 jar(`shade files("libs/luaj-jse-3.0.1.jar")` 直塞):`org/luaj/` 共 **344** 个 class。
- JPMS 禁止两个模块导出同名包 → JEI(`jei`)解析依赖时双重导出 `org.luaj.vm2.compiler`,启动失败。

实证:目标实例 `mods/` 里 `Axiom-5.4.2-for-MC1.21.1.jar` 在场,FPE 旧 jar 已被改名
`firstpersonfoodeating-0.1.0-neoforge-1.21.1.jar.disabled`(手动禁用以让游戏能启动)。
Axiom 是用户要保留的 → **FPE 让步,重定位自带 luaj**。

### 方案:shadow 插件 + relocate
`build.gradle` 改动:
1. 引入 `id 'com.gradleup.shadow' version '8.3.5'`(兼容 Gradle 8.9 / JDK21;johnrengelman 已停更,gradleup 为其官方继任)。
2. `shadowJar` 配置:
   - `configurations = [project.configurations.shade]`(只打包 shade 配置,不动 MC/NeoForge 依赖)。
   - `relocate 'org.luaj', 'com.sighs.firstpersonfoodeating.shadow.luaj'` —— **字节码级**重写,
     既改 luaj 库自身内部引用,也改 mod 源码编译产物里的 `import org.luaj.*`。
   - `mergeServiceFiles()` —— 把 `META-INF/services/javax.script.ScriptEngineFactory` 的**内容**
     一并重定位(path-only relocate 不会改文件正文;此条确保引擎服务声明自洽)。
   - `exclude 'lua.class'/'luac.class'/'luajc*.class'` —— 丢弃 luaj 默认(无名)包里的 CLI 工具类
     (lua/luac/luajc,开发工具,运行期不用,且无名包无法 relocate)。
   - `exclude 'META-INF/*.SF/*.DSA/*.RSA'` —— 去签名/清单冲突。
   - `archiveClassifier = ''` —— shadowJar 即正式产物 `firstpersonfoodeating-0.1.0-neoforge-1.21.1.jar`。
3. 普通 `jar` 改 `archiveClassifier = 'slim'` 让位;`assemble` 依赖 `shadowJar`。
   - 注:ModDevGradle 2.0.107 不往 manifest 注入任何特殊属性(实测仅 `Manifest-Version: 1.0`),
     mod 身份靠 `META-INF/neoforge.mods.toml`(在 resources 里,随 `sourceSets.main.output` 进 shadowJar),
     故 shadowJar 直接作正式产物安全,不丢 mod 元数据。

### 源码 / 脚本侧:无需手改
逐项核查(MEMORY 步骤 2):
- **字符串引用 `"org.luaj"/"org/luaj"`**:全工程 0 处。
- **`Class.forName` / `loadClass` / 反射**:luaj 相关 0 处。
- **`luajava`**:0 处(本 mod 不用 luajava 桥;`FoodAssetsManager` 直接 `new Globals()` + 注入 lib)。
- **`.lua` 脚本(3 个)**:均不含 `org.luaj` / `luaj` / java 类名硬编码,只用注入常量(INPUT_*/PLAY_*)和标准 lua 库。
→ shadow 字节码 relocate 已自动覆盖所有 Java `import`,**无任何 .lua / 字符串需手动改**。

### 构建 + jar 验证(JDK21 + 系统 Gradle 8.9)
`gradle clean shadowJar` BUILD SUCCESSFUL(仅 31 个 NeoForge API deprecated 警告,0 error)。
对产物逐项核验:
| 检查项 | 期望 | 实测 |
|---|---|---|
| `org/luaj/**` 顶层 class | 0 | **0** |
| 任何 `org/luaj` 条目(含目录) | 0 | **0** |
| 重定位类 `com/sighs/firstpersonfoodeating/shadow/luaj/**` | >0 | **344** |
| 无名包 CLI 残留(lua/luac/luajc) | 0 | **0** |
| `META-INF/neoforge.mods.toml` | 在 | 在 |
| service 文件正文 | 新包 FQCN | `com.sighs.firstpersonfoodeating.shadow.luaj.vm2.script.LuaScriptEngineFactory` |
| mod 自身 class(`io/github/ymrsl/...`)字节码 luaj 引用 | 全指新包 | 抽检 `FoodAssetsManager`:`shadow/luaj` 63 处,`org/luaj` **0** 处 |
| 重定位 luaj 内部类(LuaC/Globals/ScriptEngineFactory)残留 `org/luaj` | 0 | **0** |
| 全 jar 原始字节扫描 `org/luaj`(任意 .class/资源) | 0 文件 | **0** |

→ **relocate 完全成功且自洽**:顶层 `org/luaj` 残留数 = **0**,luaj 功能类完整(344 个),
所有引用(mod 字节码 + 库内部 + service 声明)统一指向 `com.sighs.firstpersonfoodeating.shadow.luaj`。
与 Axiom 的 `org.luaj.vm2` 不再同名 → JPMS 双导出冲突消除。

### 产出
新 jar 覆盖到
`\.minecraft\versions\1.21.1-NeoForge_21.1.233\mods_WS7待入包\firstpersonfoodeating-0.1.0-neoforge-1.21.1.jar`
(同名,3,431,257 B;原 WS7待入包 目录此前为空)。**未动 `mods\` 正式目录**(里面的
`...jar.disabled` 与 Axiom 保持原状,由后续入包流程处理)。

> 注:本次仅消除启动期 JPMS 冲突,**未实机验证**脚本动画运行期行为;入包后建议跑客户端确认 luaj 状态机正常。
