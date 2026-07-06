# PORT_REPORT · SearchCarefully(侦察完成,移植待执行)

**状态:未开工——Opus 侦察 agent 受会话权限限制只完成研究;本报告为移植蓝图(2026-06-12)**

## 已查明事实
- 原 mod = **Forge 1.20.1**(非 Fabric,Connector 捷径不适用)。本机成品 jar 两份,**以新版为移植基线**:
  - `D:\MC\ProjectUTD\UntilWeDieOriginal\...\mods\mods_已整理\其他\[Both] SearchCarefully-1201.3.0-forge.jar`(✓ 较新)
  - `D:\MC\ProjectUTD\总模组目录\25年12月的全新mod汇总\SearchCarefully-1201.1.1-forge.jar`
- modid = `searchcarefully`;`[Both]` = 双端 mod。
- 玩法(从 searchcarefully-common.toml 还原):**战术搜刮**——战利品容器按 7 档稀有度逐格计时搜索(rarity1~7BaseTime+RandomTime、maxSearchTimeTicks、searchSpeedMultiplier、enableHotbarSearch、chestPathSegments 匹配 loot table 路径)。
- GitHub `Yanbwe/SearchCarefully` 是否有 1.21 分支:**未核实**(agent 网络工具被拒)——开工第一步先查分支与 Modrinth。

## 移植蓝图(Forge 1.20.1 → NeoForge 1.21.1)
1. **头号坑 = ItemStack NBT → DataComponents**:搜刮进度/已搜标记若存 ItemStack NBT,必须改 `DataComponentType` + DeferredRegister + Codec(getTag/getOrCreateTag 已删),并考虑旧存档兼容。
2. 常规:mods.toml→neoforge.mods.toml;pack_format=34;构建换 ModDevGradle+`net.neoforged:neoforge:21.1.233`;`MinecraftForge.EVENT_BUS`→`NeoForge.EVENT_BUS`;包名 net.minecraftforge.*→net.neoforged.*;ForgeConfigSpec→ModConfigSpec(**config 字段名/默认值逐字保留**,老配置才不失效);ResourceLocation 改静态工厂;LootTableLoadEvent/容器 Screen 签名核对。
3. 数据包目录 1.21 改名(若有):recipes→recipe、tags/blocks→tags/block。
4. 构建/产出/报告流程同 JustBarricades(见其 PORT_REPORT)。

## 测试要点
启动无报错+config 字段一致;开战利品箱出现逐格计时而非瞬开;改 config 验证生效;双端测试;DataComponents 回归(退出重进容器进度不丢)。
