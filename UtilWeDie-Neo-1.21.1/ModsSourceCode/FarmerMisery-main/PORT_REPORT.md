# FarmerMisery — NeoForge 1.21.1 移植报告

- 源：AmarokIce/FarmerMisery（1.20.1 Forge 47.1.0，modid `farmer_misery`，包名 `club.someoneice.cockroach`）
- 目标：NeoForge **21.1.233** / Minecraft **1.21.1**，构建工具 **ModDevGradle 2.0.107** + Gradle 8.9 + JDK 21
- 结论：**移植成功，jar 已构建并通过完整性校验**。14 个食物道具 + 酿造 + 喷溅瓶投掷实体全部可用；蟑螂捕捉/生成做成 Alex's Mobs 软可选联动（当前整合包内 AM 缺失，此功能自动休眠，不影响加载）。

## 产物
- 新工程：`ModsSourceCode\FarmerMisery-main\neoforge-1.21.1\`（原 1.20.1 源码完整保留未动）
- 构建 jar：`neoforge-1.21.1\build\libs\farmer_misery-1.0.1-neoforge-1.21.1.jar`
- 已复制到：`.minecraft\versions\1.21.1-NeoForge_21.1.233\mods_WS7待入包\farmer_misery-1.0.1-neoforge-1.21.1.jar`

## 关键发现（务必知悉）
1. **真实硬依赖是 Alex's Mobs，不是 FarmersDelight。** 原 `build.gradle` 依赖 `curse.maven:alexs-mobs` + `citadel`；`BottleEntityRoach` 与 `ItemBottleMixin` 直接引用 `com.github.alexthe666.alexsmobs` 的 `EntityCockroach` / `AMEntityRegistry.COCKROACH`。FD 仅"主题相关"——Java 代码 0 处 import FD；FD 只用在数据包 `cooking_pot` 配方里。
2. **Alex's Mobs 无 NeoForge 1.21.1 版本，且当前整合包里没装**（mods 目录只有它的前置库 citadel-2.7.0，没有 AM 本体）。因此无法编译期依赖 AM，运行时 `alexsmobs:cockroach` 实体也不存在。
3. 处理方式：新增 `compat/AlexsMobsCompat.java`，用 `ModList.isLoaded("alexsmobs")` + 运行时实体注册表查 `alexsmobs:cockroach` 的软联动层。**装了 AM → 捕捉/生成蟑螂功能自动启用；没装 → 食物/酿造/投掷全部照常，投掷瓶落地只是像喷溅药水一样碎掉、不生成蟑螂。** mod 可独立加载、独立运行。

## 改动文件清单（neoforge-1.21.1/ 下，全部新建）
构建：
- `build.gradle`（ModDevGradle，neoForge 21.1.233，无 mixin AP——见下）、`settings.gradle`、`gradle.properties`

Java（`src/main/java/club/someoneice/cockroach/`）：
- `ModMain.java` — `@Mod(IEventBus, ModContainer)` 构造注入；TABS 用 `Registries.CREATIVE_MODE_TAB`
- `ItemInit.java` — `DeferredRegister.createItems` + `DeferredItem<Item>`
- `EntityInit.java` — `Registries.ENTITY_TYPE` + `DeferredHolder`；`build("roach_bottle")`（21.1.233 仍是 String 重载，注意：1.21.3+ 才换成 ResourceKey）
- `Datas.java` — `List<MobEffect>` → `List<Holder<MobEffect>>`（1.21 效果全面 Holder 化）
- `item/`（13 类）— `FoodProperties.Builder.effect(MobEffectInstance,float)`（去掉 Supplier）；删除 1.21 已移除的 `.meat()`；craftRemainder 道具改为直接 `new ItemStack(Items.GLASS_BOTTLE/BOWL)` 返还容器；`UseAnim`（非 1.21.2+ 的 ItemUseAnimation）
- `item/ItemThrowableRoachBottle.java` — `getXRot/getYRot` 替代 `xRotO/yRotO`；**移除原版从弓箭抄来的无效"无限附魔"判定**（1.21 `EnchantmentHelper.getEnchantments` 已删；喷溅瓶本无此逻辑），改为创造模式不消耗、否则 shrink(1)
- `entity/BottleEntityRoach.java` — 删除 `NetworkHooks.getEntitySpawningPacket` 重载（1.21 NeoForge 已删，vanilla `ThrowableItemProjectile` 自带 spawn 包）；`getGravity()`→`getDefaultGravity()`(float→double)；蟑螂生成走 AlexsMobsCompat
- `compat/AlexsMobsCompat.java` — 【新增】AM 软联动层
- `ForgeEvent.java` — 酿造从已删的静态 `BrewingRecipeRegistry.addRecipe` 改为监听 `RegisterBrewingRecipesEvent`，调 `event.getBuilder().addRecipe(Ingredient,Ingredient,ItemStack)`（NeoForge 保留了同签名 API）
- `ModEvents.java` — `EntityRenderersEvent.RegisterRenderers` 换 `net.neoforged.neoforge.client.event` 包；`@EventBusSubscriber(... value=Dist.CLIENT)`
- `mixin/ItemBottleMixin.java` — 注入方法名 `interactLivingEntity` 不变；改用 AlexsMobsCompat.isCockroach 判定；注入方法加 `farmer_misery$` 前缀避免冲突

资源（`src/main/resources/`）：
- `META-INF/neoforge.mods.toml` — 由 `mods.toml` 重写：loaderVersion `[1,)`、neoforge `[21.1,)`、minecraft `[1.21.1,1.21.2)`；farmersdelight 与 alexsmobs 均声明为 **optional**；保留 `[[mixins]]`
- `pack.mcmeta` — pack_format **34**
- `farmer_misery.mixins.json` — compatibilityLevel `JAVA_21`；**删掉 refmap 字段**（见下）
- `assets/farmer_misery/...` — lang/models/textures 原样拷贝（1.21 布局不变）；**补了 `en_us.json`**（原 mod 只有 zh_cn）
- `data/farmer_misery/recipe/`（9 个）— 目录 `recipes`→`recipe` 单数化；配方类型 `farmersdelight:cooking`→**`farmersdelight:cooking_pot`**（FD 1.21.1 重命名，已解包 FD jar 的 CookingPotRecipe$Serializer codec 核实）；`result:{count,item}`→`result:{id,count}`（ItemStack codec）；标签 `forge:salad_ingredients`/`forge:vegetables`→**`c:salad_ingredients`**/**`c:vegetables`**（已核实 FD 1.21.1 在 `data/c/tags/item/` 下填充这两个标签）

## 构建踩坑（已解决）
- **Sponge Mixin AP 报 "Unable to locate obfuscation mapping for interactLivingEntity"**：NeoForge 1.21 运行时用官方(Mojang)映射，不需要 SRG refmap。解法：build.gradle 去掉 `annotationProcessor 'org.spongepowered:mixin:...:processor'`，mixins.json 去掉 `refmap` 字段。NeoForge 直接按 Mojang 名加载 mixin。
- `EntityType.Builder.build()` 在 21.1.233 仍要 **String**（不是 ResourceKey），错用 ResourceKey 会编译失败。
- 残留告警（非致命，不影响出包/运行）：`EventBusSubscriber.Bus` 标记 for-removal（21.1.x 仍可用）；`ItemRoachInBottle` 用了 deprecated 的 craftRemainder（仍可用）。

## 游戏内测试法
1. **道具/创造栏**：创造物品栏找"蟑螂乐事/Cockroach Delicacies"标签页，应有 14 个道具，图标=瓶装蟑螂。
2. **食物效果**（吃下后，服务端）：
   - 瓶装蟑螂 bottled_roach：中毒 30s，吃完返还玻璃瓶
   - 蟑螂烧/汉堡/卷饼/沙拉拼盘等：随机叠加多组对冲效果（力量/虚弱、速度/缓慢、再生/中毒），并按等级清除中毒/虚弱
   - 康复新液 kangfu_xin_ye：清所有有害效果+抗火 10s+回血 1 心，喝（DRINK 动画），返还玻璃瓶
3. **FD 联动配方**（需装 FarmersDelight，已在包内 1.3.2）：用**炖锅 Cooking Pot** 按 `data/.../recipe/*.json` 合成（如 1 瓶装蟑螂→爆浆烤蟑螂；3 瓶装蟑螂+小麦面团→2 蟑螂肉饼）。JEI 可查。
4. **酿造**：酿造台——下方放普通药水(POTION)、上方材料放瓶装蟑螂 → 出康复新液；下方放瓶装蟑螂、材料放火药 → 出喷溅瓶装蟑螂。
5. **喷溅瓶投掷**：手持"喷溅瓶装蟑螂"右键投出（喷溅药水音效+抛物线），落地/命中实体会碎。**装了 Alex's Mobs 才会在落点生成一只蟑螂**；没装则只碎不生成。
6. **瓶子抓蟑螂**（仅装 AM 时）：手持空玻璃瓶右键 Alex's Mobs 的蟑螂 → 蟑螂消失、获得瓶装蟑螂。

## 风险/已知限制
- **蟑螂相关两项功能（投掷生成 + 瓶子捕捉）在当前整合包里不会触发**，因为包内没装 Alex's Mobs（且 AM 暂无 NeoForge 1.21.1 版）。其余功能不受影响。若日后加入 AM 1.21.1：modid 须为 `alexsmobs`、蟑螂实体注册名须为 `alexsmobs:cockroach`，则功能自动生效（如 AM 用了别的注册名，改 `AlexsMobsCompat.COCKROACH_ID` 一处即可）。
- 配方里的 `c:vegetables`/`c:salad_ingredients` 依赖 FD（或其他 mod）填充该标签；包内 FD 1.21.1 已填充，正常。`farmersdelight:tomato/onion/wheat_dough` 道具 id 沿用原配方，FD 1.21.1 仍存在这些 id。
- mixin 注入 vanilla `Item.interactLivingEntity` HEAD，仅在手持玻璃瓶且目标是 AM 蟑螂时动作；AM 缺失时 `isCockroach` 直接返回 false，零副作用、不会误伤其它玻璃瓶右键交互。
- 未做 DataComponents 自定义（原 mod 无自定义 NBT，无需）。喷溅实体网络同步用 vanilla 默认通道，1.21.1 下 ItemSupplier 实体可正常渲染（ThrownItemRenderer）。
- 未实机进游戏跑测（仅构建+jar 结构校验）；建议首次进游戏看日志确认无 mixin apply 失败 / 注册冲突。
