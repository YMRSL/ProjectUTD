# BlockZ 迁移转换约定 (1.20.1 Forge → 1.21.1 NeoForge)

**所有移植必须严格遵守本文档。原则:从原理改对,faithful 移植,只改 API 迁移必须改的,不删功能、不取巧、不绕过。**

源码(只读参照):`_upstream-1.1.6-forge/src/main/java/com/yitianys/BlockZ/...`
目标工程:`BlockZ-1.21.1-neoforge/src/main/...`
包名保持 `com.yitianys.BlockZ`(大写 B 不变)。mod id = `blockz`。

---

## 0. 范围(只移植 KEEP,跳过 DROP)
KEEP:A 占格物品栏 / B 5背包+Curios / C 服装 / H 支撑(配置·界面·键位·网络包·创造标签·音效·相关命令) / 联动 Curios 全套 + TaczAmmoCompat 弹药。
DROP(整体不移植,引用处一律删除):护理医疗(Bleeding/Fracture/Analgesic 效果+Bandage/Splint/Rags/Morphine/Codeine/Medical 物品)、DayZ僵尸、尸体(Corpse/ZombieCorpse)、DayZ HUD 覆盖层、TaCZ 镜头/卧倒/瞄准(camera 包全部、TaczProneCompat/TaczClientCompat/TaczRenderCompat/TaczShootAimOverrideAccess、所有 Lean/FirstPersonBody/WalkSway/Prone mixin)、主菜单片头(mainmenu 包、widget 包、MixinTitleScreen)、体力/感染/倾身系统、VersionTestMod、lootr 联动。

SPLIT 文件(部分删改):移植时**只保留 KEEP 逻辑,删掉 DROP 分支**,详见各文件注释。

---

## 1. 注册系统 (DeferredRegister)
- `DeferredRegister.create(ForgeRegistries.ITEMS, MODID)` → `DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID)`
- 其他注册表:`DeferredRegister.create(Registries.SOUND_EVENT, MODID)`、`Registries.MENU`、`Registries.CREATIVE_MODE_TAB`、`Registries.MOB_EFFECT`(不用)。导入 `net.minecraft.core.registries.Registries`。
- `RegistryObject<Item>` → `DeferredItem<Item>`(item 专用)或 `DeferredHolder<T, U>`。导入 `net.neoforged.neoforge.registries.DeferredItem` / `DeferredHolder`。
- **物品注册(1.21 需 id 注入 Properties)**:用 `ITEMS.registerItem("name", props -> new XItem(props, ...), new Item.Properties().stacksTo(1))`。lambda 收到的 props 已带好 id。**不要**用 `() -> new XItem(new Item.Properties())`(1.21 会因缺 setId 崩)。
- 注册器在主类构造里 `X.register(modBus)`(DeferredRegister 需绑定 modBus)。
- `ForgeRegistries.ITEMS.getKey(item)` → `BuiltInRegistries.ITEM.getKey(item)`(`net.minecraft.core.registries.BuiltInRegistries`)。
- `ForgeRegistries.ITEMS.getValue(rl)` → `BuiltInRegistries.ITEM.get(rl)`。

## 2. ResourceLocation
- `new ResourceLocation("blockz","x")` → `ResourceLocation.fromNamespaceAndPath("blockz","x")`
- `new ResourceLocation("blockz:x")` → `ResourceLocation.parse("blockz:x")`
- 自模组路径可用 `ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "x")`。

## 3. 配置
- `net.minecraftforge.common.ForgeConfigSpec` → `net.neoforged.neoforge.common.ModConfigSpec`(类型全等改名:`ModConfigSpec.IntValue/BooleanValue/DoubleValue/ConfigValue/Builder`)。builder API 不变。
- **BlockZConfigs 已移植完成**(裁剪版,只剩 gui 背包相关 + backpacks 段)。不要重写它。已删除的 getter(getShowDayzHud/isStaminaEnabled/isLeanEnabled/getCorpseDespawnTime/isNursing.../camera.../mainmenu... 等)——KEEP 文件里若有引用,说明那是 DROP 逻辑,删掉该引用。
- 主类注册配置:`modContainer.registerConfig(ModConfig.Type.COMMON, BlockZConfigs.COMMON_SPEC)`(`net.neoforged.fml.config.ModConfig`)。

## 4. 主类 @Mod
- 构造签名:`public BlockZ(IEventBus modBus, ModContainer modContainer)`(不再是 FMLJavaModLoadingContext)。
- `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`(`net.neoforged.neoforge.common.NeoForge`)。
- `@Mod.EventBusSubscriber(...)` → `@EventBusSubscriber(modid=..., bus=...)`(`net.neoforged.fml.common.EventBusSubscriber`);`Bus.FORGE`→`EventBusSubscriber.Bus.GAME`,`Bus.MOD`→`Bus.MOD`。
- `@SubscribeEvent` 不变(`net.neoforged.bus.api.SubscribeEvent`)。
- `FMLCommonSetupEvent`/`FMLClientSetupEvent` 在 `net.neoforged.fml.event.lifecycle.*`。
- 移除 `GeckoLib.initialize()` 与一切 geckolib(仅僵尸/尸体用,已 DROP)。移除 DayZZombieConfig/ModEffects/ModEntities/registerSpawnPlacements/Lean(onEntityJoinLevel)。

## 5. 存储层(架构核心,务必按此实现)
### 5a. PlayerBackpack → 数据附件 (Data Attachment)
- 新建 `init/BlockZAttachments.java`:`DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID)`。
- 注册:`PLAYER_BACKPACK = ATTACHMENTS.register("player_backpack", () -> AttachmentType.serializable(PlayerBackpack::new).copyOnDeath().build())`。
- `PlayerBackpack` 改为 `implements INBTSerializable<CompoundTag>`(`net.neoforged.neoforge.common.util.INBTSerializable`)。**serialize/deserialize 增加 `HolderLookup.Provider provider` 参数**:`CompoundTag serializeNBT(HolderLookup.Provider provider)` / `void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt)`。内部 `inventory.serializeNBT(provider)` / `inventory.deserializeNBT(provider, tag)`。
- 访问:`player.getData(BlockZAttachments.PLAYER_BACKPACK)` 取(不存在自动创建);写后对 ServerPlayer 同步走网络包。`player.setData(...)` 不常用(getData 返回的是可变对象,直接改即可)。
- **删除 PlayerBackpackProvider.java**(provider 机制被附件取代)。删除 AttachCapabilitiesEvent 相关。

### 5b. BlockZPlayerItemHandler(玩家身上的 IItemHandler capability)
- NeoForge:用 `RegisterCapabilitiesEvent`(`net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent`)在主类 modBus 监听里注册:`event.registerEntity(Capabilities.ItemHandler.ENTITY, EntityType.PLAYER, (player, ctx) -> new BlockZPlayerItemHandler(player))`。
- `BlockZPlayerItemHandler` 本体保留,`net.minecraftforge.items.*` → `net.neoforged.neoforge.items.*`。
- **删除 BlockZPlayerItemHandlerProvider.java**(被 capability 注册取代)。
- 取用:`player.getCapability(Capabilities.ItemHandler.ENTITY, null)`(返回 IItemHandler,可能 null)。`LazyOptional` 全部去掉:Forge `getCapability(...).ifPresent(h->...)` → `IItemHandler h = entity.getCapability(...); if (h!=null){...}`。

### 5c. NestedStorageItemHandler(背包物品内嵌库存)→ DataComponent
- 1.21 ItemStack 无 getOrCreateTag/getTag。新建 `init/BlockZDataComponents.java`:`DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(MODID)`。
- 注册一个承载内嵌库存的组件:`BACKPACK_INVENTORY = COMPONENTS.registerComponentType("backpack_inventory", builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(CompoundTag.CODEC)))`,类型 `DataComponentType<CompoundTag>`。
- `NestedStorageItemHandler` 里凡 `stack.getOrCreateTag()`/`getTag()`/`getTagElement("X")` 读写库存 NBT 的地方:
  - 读:`CompoundTag t = stack.getOrDefault(BlockZDataComponents.BACKPACK_INVENTORY, new CompoundTag())`(若组件存的就是 inventory tag,直接用);
  - 写:`stack.set(BlockZDataComponents.BACKPACK_INVENTORY, tag.copy())`。
  - 保持原有"读 currentStack 的 inventory tag、写回 stack"的语义不变,只换底层读写为组件。
- `ItemStackHandler.serializeNBT()` → `serializeNBT(provider)`;`deserializeNBT(tag)` → `deserializeNBT(provider, tag)`。NestedStorage 内部若 new ItemStackHandler 并 serialize,需要 provider —— 可从 `stackSupplier` 拿不到 provider 时,用 `net.minecraft.server.MinecraftServer` 的 registryAccess,或把 provider 作参数传入。**优先**把 provider 通过调用链传进来;实在拿不到的纯客户端读场景可用 `net.minecraft.client.Minecraft.getInstance().level.registryAccess()`,服务端用 server.registryAccess()。

## 6. 网络 (SimpleChannel → Payload)
- 新建/改写 `network/NetworkHandler.java`:监听 `RegisterPayloadHandlersEvent`(modBus),`registrar.playToServer(Type, StreamCodec, handler)` / `playToClient(...)`。version 用 `registrar("1")`。
- 每个包改成 `record XPacket(...) implements CustomPacketPayload`:
  - `public static final Type<XPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MODID,"x"))`;
  - `public static final StreamCodec<RegistryFriendlyByteBuf, XPacket> STREAM_CODEC = StreamCodec.composite(...)` 或 `ofMember`/手写 `StreamCodec.of((buf,msg)->...,(buf)->...)`;
  - `@Override public Type<? extends CustomPacketPayload> type(){return TYPE;}`。
- handler 签名:`(XPacket payload, IPayloadContext ctx) -> { ctx.enqueueWork(()->{...}); }`。`ctx.player()` 取玩家(server 侧是 ServerPlayer)。
- 发送:`PacketDistributor.sendToServer(payload)`(C2S) / `PacketDistributor.sendToPlayer(serverPlayer, payload)`(S2C)。
- 旧的 `buf.writeXxx/readXxx` 逻辑迁进 StreamCodec。

## 7. 物品 / DataComponents
- `net.minecraftforge.common.ForgeSpawnEggItem`、医疗物品类:DROP,不移植。
- 物品类构造现在收 `Item.Properties`(已由 registerItem 注入 id)。
- 凡用 `ItemStack.getOrCreateTag()/getTag()/hasTag()` 存自定义数据 → 改 DataComponent(见 5c 模式,按数据类型注册对应组件)。`stack.getTagElement` 同理。

## 8. 客户端 / 渲染 / GUI
- `net.minecraftforge.client.event.*` → `net.neoforged.neoforge.client.event.*`(RegisterKeyMappingsEvent / RenderGuiEvent / InputEvent / RegisterClientReloadListenersEvent / EntityRenderersEvent / RegisterMenuScreensEvent 等)。
- 菜单屏幕注册:`RegisterMenuScreensEvent`(modBus,client):`event.register(ModMenus.DAYZ_MENU.get(), DayZInventoryScreen::new)`。
- 键位注册:`RegisterKeyMappingsEvent`。
- `GuiGraphics` 渲染 API 1.21 基本兼容;`Screen#render(GuiGraphics, int, int, float)` 签名不变。
- `Font`/`blit`/`drawString` 注意 1.21 `GuiGraphics.blit` 多了重载,保持参数对应。
- ClothingLayer:`RenderLayer<...>` 玩家层,`EntityRenderersEvent.AddLayers` 注册;`net.minecraftforge.client.event.RenderPlayerEvent`/AddLayers → neoforge 版。

## 9. 事件 / 命令
- 事件包名 forge→neoforge(`net.minecraftforge.event.*` → `net.neoforged.neoforge.event.*`,如 `entity.player.PlayerEvent`、`tick.PlayerTickEvent`(注意 1.21 拆成 Pre/Post)、`RegisterCommandsEvent`、`entity.EntityJoinLevelEvent`)。
- `PlayerTickEvent` → `PlayerTickEvent.Post`/`Pre`(`net.neoforged.neoforge.event.tick.PlayerTickEvent`)。
- 命令 API(brigadier)不变。SPLIT:CommandInit 删 corpse 清理命令;只保留 toggle_ui/reload/grid_item/clothing_capacity。

## 10. Mixin
- 配置 `blockz.mixins.json`(已建 stub),`compatibilityLevel JAVA_21`,package `com.yitianys.BlockZ.mixin`。
- 只移植 KEEP mixin(背包界面/槽位/Curios/TaCZ弹药相关),DROP 的(Lean/Prone/FirstPersonBody/WalkSway/TitleScreen/GunHud/camera)全部不移植,且不写进 mixins.json。
- mixin 目标类的 1.21.1 名/方法签名需核对(parchment 映射)。`@Inject` target 字符串按 1.21.1 调整。

## 11. 杂项 API 映射
- `net.minecraftforge.items.ItemStackHandler/IItemHandler/IItemHandlerModifiable` → `net.neoforged.neoforge.items.*`。
- `LazyOptional<T>` → 直接用可空 `T`(去掉 Optional 包装)。
- `Component.literal/translatable` 不变。
- `net.minecraftforge.fml.util.thread.SidedThreadGroups` / `DistExecutor` → `net.neoforged.fml.*`;`DistExecutor.unsafeRunWhenOn` 优先改成 `if (FMLEnvironment.dist.isClient())` 或 `@OnlyIn`/分客户端类。
- `@OnlyIn(Dist.CLIENT)` → `net.neoforged.api.distmarker.OnlyIn`/`Dist`。
- Curios:导入 `top.theillusivec4.curios.api.*`(CuriosApi、SlotResult、ICuriosItemHandler);9.x API 与 5.x 略有差异,按 1.21.1 curios 9.5.1 的类签名核对。
- TaCZ:`com.tacz.guns.api.*`,1.1.8(1.21.1) 的类/方法名核对;原代码多用反射(Class.forName),保留反射更稳。

## 12. 编译循环
分批移植,每批后 `gradlew :compileJava --offline`(JDK21 已在 gradle.properties 钉死),按报错逐个修。资源(模型/lang/贴图)从 upstream resources 拷贝,删 DROP 项。
