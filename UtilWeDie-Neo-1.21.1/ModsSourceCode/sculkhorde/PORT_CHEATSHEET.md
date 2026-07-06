# Sculk Horde 移植速查表 (Forge 1.20.1 → NeoForge 1.21.1)

修编译错误时**逐条对照应用**。原则：从原理改对，不删/禁用/绕过。只改你被分配的包，别动别的包。
工程根 = `D:/MC/ProjectUTD/UtilWeDie-Neo-1.21.1/ModsSourceCode/sculkhorde/neoforge-1.21.1`，源码在 `src/main/java/com/github/sculkhorde/`。

> 已由脚本批量做过的(无需再做)：Forge→NeoForge 包名(distmarker/eventbus.api→bus.api/fml/event/client/common)、`ForgeConfigSpec`→`ModConfigSpec`、`new ResourceLocation(a,b)`→`ResourceLocation.fromNamespaceAndPath(a,b)` / 单参→`ResourceLocation.parse(...)`、GeckoLib 包路径。剩下的按下表手改。

## 1. 注册 (registries) —— 最高频
- `import net.minecraftforge.registries.ForgeRegistries;` → `import net.minecraft.core.registries.Registries;`
- `import net.minecraftforge.registries.RegistryObject;` → `import net.neoforged.neoforge.registries.DeferredHolder;`
- `DeferredRegister.create(ForgeRegistries.XXX, MOD_ID)` → `DeferredRegister.create(Registries.YYY, MOD_ID)`，映射：
  ITEMS→ITEM, BLOCKS→BLOCK, MOB_EFFECTS→MOB_EFFECT, ENTITY_TYPES→ENTITY_TYPE, BLOCK_ENTITY_TYPES→BLOCK_ENTITY_TYPE,
  SOUND_EVENTS→SOUND_EVENT, PARTICLE_TYPES→PARTICLE_TYPE, POTIONS→POTION, MENU_TYPES→MENU,
  RECIPE_TYPES→RECIPE_TYPE, RECIPE_SERIALIZERS→RECIPE_SERIALIZER, STRUCTURE_TYPES(用 Registries.STRUCTURE_TYPE), 
  STRUCTURE_PROCESSOR? 用 `Registries.STRUCTURE_PROCESSOR`。
- `RegistryObject<T>` → `DeferredHolder<元素类型, T>`。元素类型=该 DeferredRegister 的注册类型：
  ModItems→`DeferredHolder<Item, T>`，ModBlocks→`DeferredHolder<Block, T>`，ModMobEffects→`DeferredHolder<MobEffect, T>`，
  ModEntities→`DeferredHolder<EntityType<?>, T>`，ModBlockEntities→`DeferredHolder<BlockEntityType<?>, T>`，
  ModSounds→`DeferredHolder<SoundEvent, SoundEvent>`，ModParticles→`DeferredHolder<ParticleType<?>, T>`，ModPotions→`DeferredHolder<Potion, T>`，
  ModMenuTypes→`DeferredHolder<MenuType<?>, MenuType<T>>`，ModRecipes→对应。
- `.register(name, supplier)` 不变；`.get()` 不变；`DeferredRegister.register(modEventBus)` 不变。
- 注意：很多地方 `RegistryObject` 变量本身就是 `Holder<X>`（DeferredHolder 实现 Holder），需要 Holder 的地方可直接传它(见§3)。
- `ForgeSpawnEggItem` → `net.neoforged.neoforge.common.DeferredSpawnEggItem`。
- 用到 `ForgeRegistries.XXX.getKey(obj)` / `.getValue(rl)` 直接访问注册表的 → 改 `BuiltInRegistries.YYY.getKey(...)`/`.get(...)`（import `net.minecraft.core.registries.BuiltInRegistries`）。

## 2. 事件 (events)
- `@Mod.EventBusSubscriber` → `@EventBusSubscriber`(import `net.neoforged.fml.common.EventBusSubscriber`)；
  `bus = Mod.EventBusSubscriber.Bus.MOD` → `bus = EventBusSubscriber.Bus.MOD`；`Bus.FORGE` → `Bus.GAME`。
- `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`(import `net.neoforged.neoforge.common.NeoForge`)。
- `TickEvent.ClientTickEvent` → `net.neoforged.neoforge.client.event.ClientTickEvent.Post`(或 .Pre)；
  `TickEvent.ServerTickEvent` → `net.neoforged.neoforge.event.tick.ServerTickEvent.Post`/.Pre；
  `TickEvent.LevelTickEvent` → `LevelTickEvent.Post`/.Pre；判 `event.phase == END` 的逻辑改用 .Post 事件、去掉 phase 判断。
- `event.getEntityLiving()` → `event.getEntity()`；`PlayerTickEvent` → `PlayerTickEvent.Post`/.Pre。
- 主类构造器：`public SculkHorde() {}` → `public SculkHorde(IEventBus modEventBus, ModContainer modContainer)`，
  在构造器里 `ModX.REGISTRY.register(modEventBus)`；`FMLJavaModLoadingContext.get().getModEventBus()` 删除、改用构造参数 modEventBus；
  `context.registerConfig(...)` → `modContainer.registerConfig(...)`。

## 3. MobEffect / Potion —— 1.21 改 Holder 包装
- 注册返回 `DeferredHolder<MobEffect, XEffect>`；它**就是** `Holder<MobEffect>`。
- `new MobEffectInstance(ModMobEffects.X.get(), ...)` → `new MobEffectInstance(ModMobEffects.X, ...)`（直接传 holder，不要 .get()）。
  即凡是 1.21 里参数要 `Holder<MobEffect>` 的地方，传 `ModMobEffects.X`（DeferredHolder）而非 `.get()`。
- `entity.hasEffect(ModMobEffects.X.get())` / `getEffect(...)` / `removeEffect(...)` → 传 `ModMobEffects.X`(Holder)。
- `MobEffectInstance.getEffect()` 现在返回 `Holder<MobEffect>`：比较时用 `inst.getEffect() == ModMobEffects.X` 或 `.is(holder)`；
  要拿 MobEffect 本体用 `inst.getEffect().value()`。
- 自定义 Effect 类里 `addAttributeModifier(Attribute, ...)` → `Holder<Attribute>`；属性常量改 `Holder<Attribute>`(见§7)。
- Potion 同理：`Holder<Potion>`，传 holder。

## 4. BlockEntity
- `saveAdditional(CompoundTag tag)` → `saveAdditional(CompoundTag tag, HolderLookup.Provider registries)`；
  `load(CompoundTag tag)` → `loadAdditional(CompoundTag tag, HolderLookup.Provider registries)`；两者都 `super.xxx(tag, registries)`。
- `getUpdateTag()` → `getUpdateTag(HolderLookup.Provider registries)`；`handleUpdateTag(tag)` 同加 provider。
- ItemStack 存取 NBT：`ItemStack.save(tag)` → `stack.save(registries)`；`ItemStack.of(tag)` → `ItemStack.parseOptional(registries, tag)`。

## 5. Entity
- `defineSynchedData()` → `defineSynchedData(SynchedEntityData.Builder builder)`；体内 `this.entityData.define(KEY, v)` → `builder.define(KEY, v)`；super 调 `super.defineSynchedData(builder)`。
- `EntityDimensions` 字段私有：`dim.width`/`dim.height` → `dim.width()`/`dim.height()`。
- `readAdditionalSaveData(tag)`/`addAdditionalSaveData(tag)` 不变(仍 1 参)，但内部 ItemStack/Holder 存取按 §4。
- `getDimensions(Pose)` 等不变；`createAttributes()` 返回 `AttributeSupplier.Builder` 不变，但属性键用 `Holder<Attribute>`(见§7)。

## 6. Item / TooltipContext
- `appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag)` →
  `appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag)`(import `net.minecraft.world.item.Item`)。体内若要 level 用 `context.level()`(可能为 null)。
- `Item.Properties().durability(n)` 等不变。`isEdible()`→`has(DataComponents.FOOD)`；`getFoodProperties()` 改组件。
- `use`/`useOn`/`hurtEnemy` 等签名一般不变。

## 7. 属性 Attributes (1.21 Holder 化)
- `Attribute` 引用改 `Holder<Attribute>`：`Attributes.MAX_HEALTH` 等本就是 `Holder<Attribute>`(不用改)；
  自定义/传参处 `Attribute` → `Holder<Attribute>`。
- `AttributeModifier(UUID, name, value, op)` → `AttributeModifier(ResourceLocation id, double value, Operation op)`(去掉 name, UUID→ResourceLocation)。
- `addTransientModifier`/`getAttribute(Attribute)` 传 `Holder<Attribute>`。

## 8. GeckoLib 4.8 (1.21.1) 签名
- `GeoEntityRenderer.actuallyRender(PoseStack, T, BakedGeoModel, RenderType, MultiBufferSource, VertexConsumer, boolean, float, int, int, int)`：
  末尾旧 `float r,g,b,a`(4 个) → 新 `int colour`(1 个, 传 `0xFFFFFFFF` 或用 `Color`)。`render(...)` 同理末尾 color。
- `GeoModel.handleAnimations(T, long, AnimationState<T>)` 签名/`AnimationState` 泛型：按编译器提示补 `<T>`。
- `getTextureLocation(T)` 去掉 `(GeoAnimatable)` cast。
- `RenderUtils`→`RenderUtil`(已脚本改)。`software.bernie.geckolib.cache.object.GeoBone` 等包已改。
- `DefaultedEntityGeoModel`/`DefaultedBlockGeoModel`/`AutoGlowingGeoLayer` 包未变(model/renderer.layer)。

## 9. 杂项
- `Level.isClientSide` 不变；`level.getServer()` 不变。
- `RandomSource` 不变；`level.random` 不变。
- `BlockState`/`Block` 大多不变；`Block.box(...)` 不变。
- `ResourceKey`/`TagKey` 不变。
- `SoundEvent`：`SoundEvent.createVariableRangeEvent(rl)` / `createFixedRangeEvent`，旧 `new SoundEvent(rl)` 改之。
- `ParticleType`：注册返回类型用 `SimpleParticleType`。
- 找不到符号且引用 `ModItems.X`/`ModBlocks.X` 等 **core 包注册项的** → 那是 core 包在修，**你不用管**(core 修好后自动解决)；你只修自己包里的 API 签名/类型错误。
- 改完**不要**自行运行 gradle 编译(会跟别的 agent 抢)；改完报告你处理了哪些文件/哪些没把握。

## 10. 硬骨头 (renamed / 大改 API)
- **BlockPathTypes → PathType**：`net.minecraft.world.level.pathfinder.BlockPathTypes` → `net.minecraft.world.level.pathfinder.PathType`；
  `setPathfindingMalus(BlockPathTypes.X, f)` → `setPathfindingMalus(PathType.X, f)`。
- **Capabilities 能力系统(1.21 NeoForge 全新)**：
  - `net.minecraftforge.items.IItemHandler` → `net.neoforged.neoforge.items.IItemHandler`；
    `net.minecraftforge.items.ItemStackHandler` → `net.neoforged.neoforge.items.ItemStackHandler`；
    `net.minecraftforge.items.wrapper.*` → `net.neoforged.neoforge.items.wrapper.*`。
  - **`LazyOptional` 已删除**：删掉 `LazyOptional<IItemHandler>` 包装，直接持有 `ItemStackHandler` 字段。
  - 旧 `getCapability(Capability, Direction)` override + `ForgeCapabilities.ITEM_HANDLER` → **删除该 override**；
    改为在主类(或 core 的事件类)用 `@SubscribeEvent` 监听 `RegisterCapabilitiesEvent`：
    `event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.XXX.get(), (be, side) -> be.getItemHandler())`。
    (import `net.neoforged.neoforge.capabilities.Capabilities` / `RegisterCapabilitiesEvent`)
  - BlockEntity 里给个 `public IItemHandler getItemHandler(){ return this.itemHandler; }`。
  - **若某 BE 的能力逻辑复杂、你没把握**：保留 ItemStackHandler 字段、删掉 LazyOptional/getCapability override 让它先能编译，**在报告里列出该文件**让我复核能力注册。
- **GameEventListener.Holder / VibrationSystem**：1.21 接口仍在但泛型/方法签名可能调整，按编译器提示补；
  `VibrationSystem.Data` / `VibrationSystem.User` / `getListenerSource()` 等按 deobf 源对照。没把握就在报告里列出。
- **menu/容器**：`AbstractContainerMenu`、`MenuType` 构造 `IContainerFactory` → NeoForge `IMenuTypeExtension.create(...)`(import `net.neoforged.neoforge.common.extensions.IMenuTypeExtension`)。
- **网络包(若有)**：Forge `SimpleChannel`/`NetworkEvent` → NeoForge `PayloadRegistrar`/`CustomPacketPayload`。本 mod 若有自定义包且复杂，列出来给我。
- **DamageSource**：`DamageSource` 现需 `Holder<DamageType>`，`level.damageSources().xxx()` 工厂仍可用；自定义伤害类型按需。
- **registerCapabilities / EntityCapability**：实体能力同理走 `event.registerEntity(...)`。
