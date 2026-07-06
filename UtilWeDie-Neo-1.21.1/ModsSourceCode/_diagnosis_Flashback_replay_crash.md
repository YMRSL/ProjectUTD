# Flashback 回放打开必崩 — 根因诊断与手术报告（第 4 轮·覆盖更新）

诊断日期: 2026-06-12（航空学包并入后复发）
环境: NeoForge 1.21.1 (21.1.233) + Sinytra Connector 2.0.0-beta.14 + Forgified Fabric API + KubeJS + Flashback 0.39.5（经 Connector 跑的 Fabric mod）
本轮依据崩溃报告: `错误报告集\错误报告-2026-6-12_20.59.06\`

---

## 1. 精确根因（已锁定）

**污染源：`createpropulsion-1.1.4.jar`（机械动力·推进，航空学包成员）内的孤儿 loot 文件**

- 文件：`data/createpropulsion/loot_tables/blocks/propeller.json`（注意是 **复数 `loot_tables`** 旧路径）
- 该文件 drop `createpropulsion:propeller`，但 **`propeller` 这个方块/物品在 1.1.4 版本里已被删除、根本没注册**：
  - 无 `blockstates/propeller.json`
  - 无 `models/block/propeller` / `models/item/propeller.json`
  - 无 lang key（`item.createpropulsion.propeller` / `block.createpropulsion.propeller` 均不存在）
  - 只剩一张残留贴图 `assets/createpropulsion/textures/block/propeller.png`
- 机理与之前判断完全一致：loot 元素引用不存在物品 → 注册表兜底返回 `AirItem` → 强转 `Holder$Reference` 失败 → `ClassCastException` → Flashback 回放走全量并行重载、该路径不 catch 单元素异常 → 整崩（`LootDataType.deserialize:46` ← `ReloadableServerRegistries.scheduleElementParse`，被 KubeJS `LootDataTypeMixin` + Forgified Fabric loot/resource-conditions mixin 接管）。

**为什么之前静态扫描没扫到、为什么是航空包带来的：**
- createpropulsion 的 jar 里 loot 同时存在两套目录：
  - `data/createpropulsion/loot_table/blocks/`（**单数**，NeoForge 1.21.1 正确路径，21 个文件，**不含** propeller）
  - `data/createpropulsion/loot_tables/blocks/`（**复数**，pre-1.21 / Fabric 旧路径，含 propeller.json 等 13 个残留）
- 原版只读单数路径，但 **Forgified Fabric loot mixin / KubeJS LootDataTypeMixin 把复数路径的 propeller.json 也吃进了重载**，于是坏引用被激活。
- 该 jar 是航空学包后并入的（见下方"扫描方法学"），所以这是新引入的污染。复数目录里其余 12 个文件（wing/thruster/burner 等）引用的物品都真实存在，**只有 propeller 是死引用**——全 jar 唯一一处。

> 交叉验证（排除其它嫌疑）：
> - **KubeJS 脚本**：`kubejs/{server,startup,client}_scripts/main.js` 全是默认 `Hello World` 空 stub，`kubejs/data` 为空。没有任何 LootJS / ServerEvents loot / addLoot。**Task 1 KubeJS 动态 loot 假设证伪**——栈里的 `kubejs.mixins.json:LootDataTypeMixin` 只是 KubeJS 挂接原版 loot 管线的接管点，不是污染来源。
> - **better_looting**：是客户端拾取 HUD mod，不注入 loot 表，排除。
> - **incontrol/loot.json** 等 config：扫描无坏引用。
> - **全 156 个当前 jar 的 loot 表"命名空间不存在"扫描**：唯一命中 `wow:chests/test`（spore 的 `document_chest.json`/`equipment_chest.json`），但那是 `random_sequence` 字段（RNG 种子 id），不引用物品、不崩，无害假阳性。
> - **`createtreadmill:treadmill_item`**：模型启发式误报。该 mod 在航空包之前就一直加载且从不崩，`treadmill_item` 是其 BlockItem 的注册名（模型经 blockstate 解析，故无独立 item 模型文件），是有效物品，未动。

---

## 2. 已做手术（已执行，可直接重测）

精确切除唯一污染文件，已先备份整 jar：

- 备份：`mods\createpropulsion-1.1.4.jar.bak`（原始 jar 完整副本）
- 操作：从 `mods\createpropulsion-1.1.4.jar` 中删除条目 `data/createpropulsion/loot_tables/blocks/propeller.json`，jar 重新打包。
- 已校验：重打包后该条目不再存在；单数 `loot_table/` 目录本就无 propeller，故删除孤儿复数文件后 propeller 方块（已不存在）不再有任何 loot 表，问题彻底消除。

> 注：未删除复数目录里其余文件——它们引用真实物品（在新路径下重复定义，至多冗余，无害）。只切了死引用这一个。

---

## 3. 待用户重测

1. 启动游戏 → 进存档 → **打开一段 Flashback 回放**，确认不再 `AirItem cannot be cast to Holder$Reference` 崩溃。
2. 若仍崩：把新崩溃报告目录发来。由于本轮扫描已覆盖全 156 jar 的 loot 物品引用 + 命名空间，若还有坏引用，极可能是：
   - 存档内 `datapacks/` 或 `world/` 里的自定义 loot（本轮未重扫存档 datapack，上一轮已扫过 ThirstWasTaken，建议复测时若复发再扫一遍当前存档）；
   - 或某 mod 物品被 tag 间接引用且 tag 为空（codec 路径在极少数情况下也会触发，本轮 `#ns:tag` 扫描无异常命中）。

---

## 4. 扫描方法学（供下次复用）

关键发现：**本轮崩溃报告（20:58）实际是航空包"完全并入前"的旧会话**——FML 在 20:53 扫描到的 207 个 mod 文件里 **不含** createbigcannons / create_dragons_plus / aeronautics / copycats / sable / createpropulsion 等;但 mods 文件夹当前有这些 jar(26 个"扫描时不在、现在在"的差集,即航空包+Connector 处理的 Fabric jar)。
因此不能只信旧崩溃报告的 Mod List,必须 **以 mods 文件夹当前真实 jar 的 mods.toml 重建已安装 modid 集(215 个)**,再扫 loot。脚本逻辑:
1. 遍历全 jar 的 `META-INF/neoforge.mods.toml` 收集 `modId`;
2. 遍历全 jar 的 `data/*/loot_table(s)/**.json`,深度 walk 提取所有 `type=item` 的 `name`,以及任意 `ns:path` 字符串;
3. 命名空间不在已安装集 → 报"缺失 mod";命名空间在、但物品在该 mod 自身 jar 的 item 模型集里找不到 → 报"疑似删除/改名物品",再用 lang key 二次确认;
4. createpropulsion:propeller 在第 3 步双重命中(无模型 + 无 lang)且为航空包新成员,确诊。

---

## 5. 未根治时的下一步（Flashback 源码容错方案 — 本轮未动用）

本轮污染源唯一且在静态 jar 内,**精确手术已足够,无需改 Flashback 源码**。若未来污染源变多/运行时生成难根除,再走源码容错:
- 源码：`ModsSourceCode\Flashback-1.21-branch`(或反编译当前 jar)；
- 目标:`Flashback.java` 的 `openReplayWorld`(约 1148–1165 行)调 `WorldLoader.load` / registry 加载处,对单个 loot 元素解析加 try-catch(仿 vanilla `RecipeManager` 逐元素容错),让坏元素被跳过并 log 而非整崩——正好实现"让崩溃晚点、把所有坏引用一次性引出来";
- 构建环境:`JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot`,Gradle 8.9,GitHub codeload zip。
- 评估:工作量中等(单点 try-catch + 重新 build jar),但当前不需要。保留为备选。
