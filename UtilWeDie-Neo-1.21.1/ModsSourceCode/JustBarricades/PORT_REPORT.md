# PORT_REPORT · Just Barricades → NeoForge 1.21.1

**结论:✅ 移植完成,一次编译通过(2026-06-12,主线亲做)**
产物:`mods_WS7待入包\just_barricades-1.0.0-neoforge-1.21.1.jar`(41KB)
工程:`ModsSourceCode\JustBarricades\neoforge-1.21.1\`(原版源码在同级 `Just_Barricades-master\`)

## 路线
无捷径(Modrinth 仅 1.20.1 forge、GitHub 仅 master 分支)→ 手动移植。原 mod 仅 6 个手写类,新建 ModDevGradle 2.0.107 + neoforge 21.1.233 工程重写。

## 文件级改动
| 文件 | 改动 |
|---|---|
| JustBarricades.java | @Mod 构造注入 (IEventBus, ModContainer);registerConfig 走 modContainer;删无用的 MinecraftForge.EVENT_BUS.register(this);BuildCreativeModeTabContentsEvent 换 neoforged 包 |
| ModBlocks/ModItems | DeferredRegister.createBlocks/createItems;RegistryObject→DeferredBlock |
| JustBarricadesCommonConfig | ForgeConfigSpec→ModConfigSpec;物品校验改 BuiltInRegistries.ITEM.containsKey(ResourceLocation.tryParse);**config 字段名/默认值逐字保留**(老配置兼容);顺手修了原版 `name == ""` 的字符串比较 bug |
| BarricadeBlock | +codec()(1.21 必需);use→useWithoutItem;getShape/getCollisionShape/entityInside/attack/canBeReplaced/isPathfindable 改 protected;isPathfindable 新签名(去 level/pos 参);ForgeRegistries.ITEMS→BuiltInRegistries.ITEM;ResourceLocation.parse;**voxel 形状/血量状态机/僵尸破坏/修理逻辑零改动** |
| ModTags | ResourceLocation.fromNamespaceAndPath |
| 资源 | mods.toml→neoforge.mods.toml(重写);pack_format=34;**数据包目录改名** recipes→recipe、tags/blocks→tags/block(含 tacz 联动 tag);recipe result.item→result.id(9 个配方) |

## 游戏内测试清单(待人工)
1. 合成:6×同种木台阶→4 个路障(9 种木都有配方);
2. 放置后徒手左键逐层破坏(4 层血量,形状逐层变化);
3. 僵尸贴脸时按概率啃路障(默认 2.5%/tick),啃光留可修理残骸;
4. 右键+木棍修理(消耗 1 棍/层);
5. config `just_barricades-common.toml` 字段与 1.20.1 版一致;
6. TACZ 持枪时对路障按交互键可修理(tacz interact_key 白名单 tag 已随迁,需 TACZ fork 同名 tag 机制支持,存疑项)。

## 风险
低。唯一存疑 = tacz tag 在 1.21.1 fork 里路径/机制是否未变(测试项 6 验证)。
