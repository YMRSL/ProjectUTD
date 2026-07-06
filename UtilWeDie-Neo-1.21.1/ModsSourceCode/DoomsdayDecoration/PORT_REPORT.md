# Doomsday Decoration — NeoForge 1.21.1 移植报告

**源**: `doomsday_decoration` v1.1.3, MCreator 生成, Forge 1.20.1, modid `doomsday_decoration`
**目标**: NeoForge 1.21.1 (neoForge 21.1.233, ModDevGradle 2.0.107, JDK 21)
**策略**: 数据驱动再生成 — 不手改 1153 个类，提取一张方块清单 (manifest.json)，运行时由单个注册器分派构造。
**结果**: ✅ 构建成功，1143 个方块 + 1143 个 BlockItem + 3 个创造栏全部覆盖（100%），jar 已部署。

---

## 1. 策略

MCreator 把每个方块生成为一个独立 Java 类（`*Block.java`，共 1143 个），属性全是模板化的 SRG 调用。
不逐个翻译，而是：

1. **提取** (`extract.py`)：用正则解析反编译的 `DoomsdayDecorationModBlocks.java`（注册名→类名映射）
   和每个 block 类（继承的父类、FACING/WATERLOGGED 状态、sound/strength/lightLevel/instrument），
   外加 `DoomsdayDecorationModTabs.java`（3 个创造栏及其有序方块列表），产出
   `manifest.json`（人读版）+ `doomsday_decoration_manifest.json`（打进 jar 的紧凑版）。
2. **数据驱动注册** (`init/ModRegistry.java`)：mod 构造时从 classpath 读 manifest，用
   `DeferredRegister.createBlocks/createItems` 批量注册；按 `type` 字段分派构造（纯方块走通用
   `DecoBlock`，特殊类型走对应 vanilla 类）。
3. **资源搬运** (`copy_resources.py`)：blockstates/models/textures/lang/sounds 原样复制；
   `loot_tables`→`loot_table`（1.21 单数目录）；pack_format→34。

---

## 2. 提取的方块数与类型分布

| 类型 (type) | 数量 | 映射到的 1.21 类 |
|---|---|---|
| block (纯方块, 含 FACING/WATERLOGGED) | 817 | `DecoBlock`（自写，可配 FACING/WATERLOGGED）|
| stair (楼梯) | 97 | `StairBlock` |
| slab (台阶) | 97 | `SlabBlock` |
| wall (墙) | 94 | `WallBlock` |
| door (门) | 19 | `DoorBlock(BlockSetType.IRON)` |
| trapdoor (活板门) | 4 | `TrapDoorBlock(BlockSetType.IRON)` |
| fence (栅栏) | 3 | `FenceBlock` |
| pane (玻璃板) | 3 | `IronBarsBlock` |
| fence_gate (栅栏门) | 3 | `FenceGateBlock(WoodType.OAK)` |
| pressure_plate (压力板) | 3 | `PressurePlateBlock(BlockSetType.STONE)` |
| button (按钮) | 3 | `ButtonBlock(BlockSetType.STONE, 20)` |
| **合计** | **1143** | |

其余统计：FACING 朝向方块 698 个、WATERLOGGED 599 个、带光照 28 个、BlockEntity 1 个（acrate，见缺口）。
所有 1143 个类都继承标准 vanilla 方块类——**没有任何奇异多方块/自定义状态结构**，分派表完备。

> 注：原 mod 里名为 `damageddoor_*` 的方块其实继承 `Block`（带 FACING 的平面装饰门），
> 并非 vanilla `DoorBlock`，已正确归为 block 类型。真正的 19 个 DoorBlock 是 `door_1..16`/
> `nomapdoor`/`nomapdoor_2`/`errordoor`，其 blockstate 含完整 facing/half/hinge/open 状态，已核对匹配。

---

## 3. 数据驱动注册器设计

- `ModRegistry.init(modBus)`：读 manifest → 注册 blocks/items/tabs → `register(bus)`。
- 每个方块的 `BlockBehaviour.Properties` 由 manifest 字段重建：`strength(destroy,resist)`、
  `sound(...)`、`instrument(...)`、`noOcclusion()`、`lightLevel(...)`。
- `DecoBlock`（自写，单类替代 817 个纯方块类）：构造参数 `hasFacing`/`hasWaterlogged`，
  实现 `getStateForPlacement`/`getFluidState`/`updateShape`/`rotate`/`mirror`，
  含 1.21 必需的 `codec()`（`simpleCodec`）。
- 特殊类型直接 new 对应 vanilla 类，门/活板门/压力板/按钮用通用 `BlockSetType`，栅栏门用 `WoodType.OAK`
  （仅影响开关音效，装饰用途无碍）。
- 透明渲染：noOcclusion / facing / 薄方块（门/玻璃板/栅栏/墙/地毯等）在客户端 `FMLClientSetupEvent`
  里统一 `ItemBlockRenderTypes.setRenderLayer(..., RenderType.cutout())`，避免贴图透明区发黑。
- 创造栏：按 manifest 的 3 个 tab 用 `DeferredRegister<CreativeModeTab>` 重建，
  `title` 用 `item_group.doomsday_decoration.<tab>` 翻译键（lang 已含），按原顺序 `displayItems`。

---

## 4. 资源处理

| 资源 | 处理 | 数量 |
|---|---|---|
| blockstates | 原样复制 | 1143 |
| models (block+item) | 原样复制 | 4009（含 block/item 子目录）|
| textures | 原样复制 | 834 |
| lang/en_us.json | 原样复制（含 1152 键 + 3 个 item_group）| 1 |
| sounds | 原样复制 | — |
| loot_tables → **loot_table** | 目录重命名（1.21 单数）| 1143 |
| pack.mcmeta | pack_format 15 → **34** | — |

- loot table 内容本身 1.20→1.21 格式兼容（`minecraft:block` + `survives_explosion` + 掉落自身），未改内容。
- blockstate 仅用 `facing=` 部分匹配；DecoBlock 额外的 WATERLOGGED 不影响 variant 匹配（部分匹配语义），与原版行为一致。
- 原工程无 `tags` 目录（脚本已预留 blocks→block / items→item 重命名逻辑，本次无文件触发）。

---

## 5. 构建结果

- `gradle build --no-daemon -x test` → **BUILD SUCCESSFUL in 17s**（仅 deprecation 警告，无错误）。
- 产物：`neoforge-1.21.1\build\libs\doomsday_decoration-1.1.3-neoforge-1.21.1.jar`（6.18 MB, 7165 entries）。
- jar 内核对：manifest.json / ModRegistry.class / DecoBlock.class / DoomsdayDecoration.class /
  neoforge.mods.toml / pack.mcmeta 均在；blockstates 1143、loot_table 1143。
- **已部署** → `D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\.minecraft\versions\1.21.1-NeoForge_21.1.233\mods_WS7待入包\doomsday_decoration-1.1.3-neoforge-1.21.1.jar`

环境：`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot`，系统 Gradle 8.9。

---

## 6. 游戏内测试法

1. 装入 NeoForge 21.1.233 实例，确认 mod 列表出现 "Doomsday Decoration"，无加载报错。
2. 创造模式物品栏：找 3 个标签页
   - `doomsday_decoration_block`（图标 = 黑白瓷砖, 474 项）
   - `doomsday_decorationcar`（图标 = 摩托车, 164 项）
   - `doomsday_decoration`（图标 = acrate, 505 项）
   确认条目数合计 1143，物品图标贴图正常。
3. 放置抽查：
   - 纯方块（如 `blackandwhiteceramictiles`）贴图正确。
   - FACING 方块（如 `accessorybox_1`）按朝向旋转。
   - 楼梯/台阶/墙（如 `floortilestaircase`/`floortilesteps`/`pooltilewall`）形状与连接正常。
   - 门 `door_1`（双格、开关、合页镜像）、活板门、栅栏门、按钮、压力板功能正常。
   - 透明方块（玻璃板 `nomap_pane`、地毯）边缘无发黑（cutout 已设）。
4. 破坏掉落：装饰方块破坏掉落自身（loot_table 已迁）。

---

## 7. 已知缺口 / 妥协（均装饰无碍，列入二期可选）

1. **自定义 VoxelShape 丢失**：原每个方块手写的碰撞/选择框（`getCollisionShape`/`getShape`）未迁移，
   现统一用完整方块 1×1×1 碰撞框。视觉渲染由 model 决定不受影响，但部分"细长/矮"装饰的碰撞框会偏大。
   如需精确碰撞，二期可把 manifest 扩展出每块的 VoxelShape 表（原类里都有 `m_49796_(...)` 字面量，可正则提取）。
2. **acrate 容器功能丢失**：原 `AcrateBlock` 是带库存的箱子型方块（唯一的 BlockEntity）。本次按"纯装饰"
   降级为普通 DecoBlock（无库存/GUI），避免移植 Menu/Container/Screen。3 个 acrate 仍可放置展示。
   二期如需可单独补一个 `BaseContainerBlockEntity` + Menu。
3. **门/按钮等的 BlockSetType/WoodType 用通用值**（IRON/STONE/OAK），仅影响开关音效，与装饰用途无关。
4. **sound 未解析的 167 个**方块走 vanilla 默认音（多为 STONE），不影响外观与放置。

---

## 8. 附属 doomsday_functionality 二期评估

`[Both] doomsday_functionality-1.20.1-0.1.0-hotfix.jar` 是 DD 的功能附属（开源）。本次**未触碰**，专注 DD 本体。
二期评估要点：
- 它依赖 DD 本体的方块注册名（本移植已保留全部原注册名 `doomsday_decoration:*`，命名兼容）。
- 需单独反编译看它注册了什么（功能性方块/物品/事件），是否引用 DD 的 BlockEntity（若引用 acrate 库存，则需先补回缺口 2）。
- 若是纯 datapack/配方/战利品扩展，迁移成本低；若含 Forge 专有 API（capabilities/事件总线），需逐个换 NeoForge 等价物。
- 建议：先在游戏内确认 DD 本体稳定，再单开对话评估附属。

---

## 附：工程文件

- 提取脚本：`ModsSourceCode\DoomsdayDecoration\extract.py`
- 资源脚本：`ModsSourceCode\DoomsdayDecoration\copy_resources.py`
- 清单：`ModsSourceCode\DoomsdayDecoration\manifest.json`
- 工程：`ModsSourceCode\DoomsdayDecoration\neoforge-1.21.1\`
  - `src\main\java\net\mcreator\doomsdaydecoration\DoomsdayDecoration.java`（主类）
  - `src\main\java\net\mcreator\doomsdaydecoration\init\ModRegistry.java`（数据驱动注册器）
  - `src\main\java\net\mcreator\doomsdaydecoration\block\DecoBlock*.java`（通用方块，见下方 §9 拆分为 4 个子类）

---

## 9. 构造顺序 bug 修复（2026-06-13）

### 症状
启动 / 注册阶段崩溃，日志报 `Trying to access unbound value: doomsday_decoration:acrate`（acrate 只是字母序第一个，
实际影响**全部 599 个 WATERLOGGED + 698 个 FACING 方块**），根因是这些 DeferredBlock 的 factory 抛异常导致方块 unbound。

### 根因（经典 Java 构造顺序坑）
原 `DecoBlock`（单类、用 `hasFacing`/`hasWaterlogged` 两个实例字段配置）的 `createBlockStateDefinition` 写成：
```java
if (hasFacing) builder.add(FACING);
if (hasWaterlogged) builder.add(WATERLOGGED);
```
但 JVM 构造顺序是 **super 构造 → 子类字段赋值**：`super(props)`（`Block` 基类构造）内部回调
`createBlockStateDefinition()` 时，子类字段 `hasFacing`/`hasWaterlogged` **尚未赋值，仍是默认 `false`**，
所以 FACING/WATERLOGGED **永远没被加进 `stateDefinition`**。随后构造函数后半段
`stateDefinition.any().setValue(WATERLOGGED/FACING, ...)` 因属性不存在抛
`IllegalArgumentException: Cannot set property waterlogged ... in Block{[unregistered]}` → factory 异常 → 方块注册失败。

### 修复方式：按属性组合拆成 4 个固定属性子类
`createBlockStateDefinition` 不能依赖未初始化的实例字段，故按 (facing, waterlogged) 拆分，每个子类的
`createBlockStateDefinition` **硬编码常量属性集**（`builder.add(FACING)` / `builder.add(WATERLOGGED)`，
编译期常量，super 阶段也能正确执行）：

| 子类 | FACING | WATERLOGGED | 备注 |
|---|---|---|---|
| `DecoBlockPlain` | — | — | 纯 `Block`，无额外状态 |
| `DecoBlockFacing` | ✓ | — | rotate/mirror/getStateForPlacement 仅处理 FACING |
| `DecoBlockWaterlogged` | — | ✓ | `implements SimpleWaterloggedBlock`，含 getFluidState/updateShape |
| `DecoBlockFacingWaterlogged` | ✓ | ✓ | `implements SimpleWaterloggedBlock`，两者齐全 |

每个子类构造函数用 `registerDefaultState` 设各自默认值，`codec()` 用 `simpleCodec(子类::new)` 返回自身类型。
`ModRegistry.makeBlock` 的 `block`/`default` 分支改为按 (facing, waterlogged) 4 种组合 new 对应子类
（替换原 `new DecoBlock(p, facing, waterlogged)`）。**其他 vanilla 类型（stair/slab/wall/door…）分支未动**
（它们自带属性，正常）。旧的 `DecoBlock.java` 已删除。

### 验证
- `gradle build --no-daemon` → **BUILD SUCCESSFUL**（仅 deprecation 警告）。
- **运行时实例化测试**：用 manifest 同款 NeoForge 21.1.233 运行时 classpath，bootstrap MC 后实例化 4 个子类各一个，
  并执行原崩点的 `setValue(FACING/WATERLOGGED, ...)`。结果全部通过，属性集与状态数符合预期：
  - `DecoBlockPlain` → facing=false, waterlogged=false, #states=1
  - `DecoBlockFacing` → facing=true, waterlogged=false, #states=4
  - `DecoBlockWaterlogged` → facing=false, waterlogged=true, #states=2
  - `DecoBlockFacingWaterlogged` → facing=true, waterlogged=true, #states=8

  即不再抛 "Cannot set property"，FACING/WATERLOGGED 已正确进入 `stateDefinition`。（测试为一次性产物，已清理，未入 jar。）
- 重构建 jar（6.48 MB，含 4 个 `DecoBlock*` 子类、无旧 `DecoBlock`）已覆盖部署到
  `…\.minecraft\versions\1.21.1-NeoForge_21.1.233\mods_WS7待入包\doomsday_decoration-1.1.3-neoforge-1.21.1.jar`。
