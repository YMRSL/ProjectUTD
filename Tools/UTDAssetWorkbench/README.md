# UTD Asset Workbench

“誓死坚守”的本地物品、配方与 Loot 档案台。它把 Mod 导出的白名单 snapshot、当前 `utd_recipe_data.js`、Loot registry/balance 汇入一份规范项目，并提供三栏网页检查器、状态回写清单、KJS 和 Excel 单向接口。游戏端导出的客户端渲染图标会以内嵌 PNG 随 snapshot 携带；网页同时读取旧工作簿的 8 类物品映射。

## 当前边界

- 状态目录是全量的：包含白名单身份、所有 KJS 配方中的 item 输入/产出，以及每一条 Loot identity。
- 可视图是严格限流的：只显示 `human_selected` 白名单根、产出根物品的配方，以及这些配方的一层直接依赖。
- Tag 只保留引用节点，不读取或展开 tag 内容。
- UTD-owned / catalogued 记录可编辑；纯外部依赖灰显且只读。
- FPE、TaCZ 等变体先按 `asset_key` 精确匹配，再按单一 `variant_discriminator` 匹配；变体绝不退化为 base registry id 匹配。
- 游戏导出的 `clientNameZhCn / translationKey` 是只读观察证据；改名和物品介绍写入独立的 `presentations[]` 草稿，不覆盖原始档案。
- 方块右键替换制造使用独立的 `blockTransforms[]` 草稿；工作台只生成审核文件，不直接改运行目录。
- “属性”页使用独立的 `itemProperties[]` 草稿；启动器自动读取 RarityCore、BlockZ、TaCZ 枪包与 FPE 当前值，候选包按原生协议生成覆盖文件，不修改第三方 JAR；独立部署器负责哈希校验、备份、应用与回滚。
- Excel 目前是单向审阅接口 JSON；实际 `.xlsx` 由上层导出器生成，Excel 不是权威源。
- 分类权威映射位于 `data/utd_item_categories.json`，由 `ItemNameCatch分类汇总_合成设计方案_v1.xlsx` 的“汇总”表机械提取；未命中的新物品明确显示“未分类”，不会按模组名猜测。

## 本地启动

在 Windows 中可直接双击 `打开UTD资产工作台.cmd`。它会从仓库 `outputs/projectutd-assets-20260711/workbench/workbench.json` 装载当前真实项目，并把游戏 `config/utd_asset_manager/exports/` 中最新一次导出的图标按精确身份合并到网页副本；随后自动检查依赖、构建页面、启动本地服务并打开浏览器。已经启动时只会复用现有页面。

手动启动方式：

```powershell
npm.cmd ci
npm.cmd run dev
```

打开终端显示的本地地址。生产构建与本地预览：

```powershell
npm.cmd run build
npm.cmd run preview
```

若 `public/data/workbench.json` 存在，页面会自动载入；否则显示内置小样本。也可以在页面右上角手动载入任意 `utd-asset-workbench/v1` 项目 JSON。

### 浏览器草稿与候选包

页面会按载入时固定的 `projectId + catalogHash` 自动保存轻量 `v2` 草稿。草稿只包含物品显示、方块替换、属性覆盖、配方可编辑字段和 Loot 可编辑字段，不保存完整物品目录、配方 `raw` 或整份项目；刷新页面会按精确身份恢复，身份集合不一致时拒绝模糊回放。写入前使用与恢复时相同的 2 MB 上限，超限不会覆盖上一份可恢复草稿。当前真实目录连同 50 把枪的源参数约 453 KB。

右上角“候选包 ZIP”会在全部校验通过后一次下载 `<projectId>.candidate.zip`，其中固定包含：

- `workbench.json`：本次编辑后的规范项目；
- `utd_block_transforms.json`：方块替换运行候选；
- `utd_item_presentations.json`：名称/介绍候选；
- `utd_item_properties.json`：统一属性候选；启用条目还会附带 `integrations/` 下的 RarityCore、BlockZ、TaCZ 与食品原生覆盖文件；
- `manifest.json`：上述三个文件的 WebCrypto SHA-256 和 UTF-8 字节数。

启用但不完整、重复 ID、创造模式策略错误，或相同目标状态与优先级冲突的规则都会阻止 ZIP 生成。单项导出按钮仍保留给诊断使用，但正式发布必须保留 ZIP、规范项目和差异报告。

### 属性部署与回滚

属性候选包导出后，先完全退出游戏，再双击 `部署UTD属性候选包.cmd`。部署器会自动选择下载目录里最新的 `.candidate.zip`（没有时打开选择窗口），随后执行：

属性页修改任意稀有度、占格、枪械或食品字段时，该条目会自动切换为“参与候选发布”。部署器拒绝 `enabled=0` 的空属性包；清除上一批属性必须使用回滚入口，避免出现“部署成功但游戏完全没变化”的假成功。

1. 校验 ZIP 清单中每个文件的 SHA-256 与字节数；
2. 预览 RarityCore、BlockZ、TaCZ 与食品的启用数量及写入文件数；
3. 在 `config/utd_asset_manager/property_backups/` 制作逐文件备份；
4. 原子写入运行文件，并生成 `property_deployment.json` 部署账本。

双击 `回滚最近一次UTD属性部署.cmd` 可以恢复部署前的逐文件快照。若运行文件在部署后又被其他工具修改，普通回滚会停止，避免覆盖后来的人工调整。

实际运行目标为：

- RarityCore 普通物品：`config/raritycore/FinalRarityConfig/utd_asset_workbench.json`；
- RarityCore 组件变体：`config/raritycore/item_data_matches/utd_asset_workbench/*.json`；
- BlockZ/DayZ：合并写入 `config/blockz/grid_items.json`，保留非 UTD 条目；
- TaCZ：只覆盖 `tacz/utd_workbench_pack/data/*/data/guns/*.json`，保留工作台模型和其它枪包资源；
- FPE 食品：`config/firstpersonfoodeating/utd_food_overrides.json`，运行时每秒检查一次变化。

TaCZ、RarityCore 和 BlockZ 改动需要重启客户端/服务器；FPE 食品属性支持热加载。游戏内 O 界面检查器会把已部署的四类属性显示为 `R/B/T/F`。

## 从真实运行数据导入

CLI 能直接解析纯 JSON，也能安全抽取当前约 3 MB 的 KJS 赋值格式，例如 `global.UTD... utd.recipeData = { ... };`。

```powershell
npm.cmd run cli -- import `
  --snapshot "D:\path\to\utd_asset_snapshot.json" `
  --recipes "D:\path\to\kubejs\startup_scripts\utd_recipe_data.js" `
  --loot-registry "D:\path\to\kubejs\startup_scripts\utd_loot_registry_data.js" `
  --loot-balance "D:\path\to\kubejs\startup_scripts\utd_loot_balance_data.js" `
  --presentations "D:\path\to\config\utd_asset_manager\presentation_drafts.json" `
  --block-transforms "D:\path\to\config\utd_asset_manager\block_transforms.json" `
  --categories "data\utd_item_categories.json" `
  --out "artifacts\current" `
  --public "public\data\workbench.json" `
  --generated-at "2026-07-11T19:50:00+08:00"
```

`generated_at` 表示本次工作台发布时刻；配方、snapshot 等各输入自己的旧时间仍单独保存在 `manifest.source`，不会混成发布日期。`--presentations` 和 `--block-transforms` 均可省略。前者读取游戏内检查器保存的 `utd-item-presentation/v1` 草稿：先按 `asset_key` 精确关联，键不一致时再按唯一的 `registry_id + variant_discriminator` 关联，无法唯一确定时会停止并明确报错。后者读取 Java 运行器使用的 `utd-block-transforms/v1` 嵌套 `rules[]` 文件，也兼容旧工作台的 `block_transforms[]` 平铺文件。

网页启动器使用 `merge-snapshot` 只把最新游戏导出的 `icon_data_url` 合入现有完整目录，不会用本机少量标注覆盖 327 条历史人工根，也不会改写配方、Loot 或草稿。未能唯一匹配的新身份会在命令摘要中计为 `unmatched`，需下一次正式重建目录处理。

也可从已编辑的规范项目重新生成所有输出：

```powershell
npm.cmd run cli -- export --project "artifacts\current\workbench.json" --out "artifacts\regenerated"
npm.cmd run cli -- validate --project "artifacts\regenerated\workbench.json"
```

属性候选也可以通过命令行预览、部署和回滚：

```powershell
npm.cmd run cli -- deploy-properties --candidate "utd-assets.candidate.zip" --instance "D:\path\to\instance" --dry-run true
npm.cmd run cli -- deploy-properties --candidate "utd-assets.candidate.zip" --instance "D:\path\to\instance"
npm.cmd run cli -- rollback-properties --instance "D:\path\to\instance"
```

正式发布方块替换规则时，可以只让这一类规则的 `error` 决定校验退出码；历史配方循环等其它内容问题仍会保留在项目中，但不会阻断本次规则发布：

```powershell
npm.cmd run cli -- validate `
  --project "artifacts\regenerated\workbench.json" `
  --only block_transform
```

发布前可将当前运行配置与候选 Workbench（或另一份运行配置）做确定性差异比较：

```powershell
npm.cmd run cli -- diff-block-transforms `
  --baseline "D:\server\config\utd_asset_manager\block_transforms.json" `
  --candidate "artifacts\regenerated\workbench.json" `
  --out "artifacts\regenerated\utd_block_transform_diff.json"
```

`--baseline` 和 `--candidate` 均接受 `utd-asset-workbench/v1` 或 `utd-block-transforms/v1`。Workbench 项目始终先经过正式运行配置导出器；直接输入的运行配置则严格按 Java v1 解析器校验并规范化。必填对象、字符串/布尔值/32 位整数类型、注册名与规则 ID、状态属性、枚举、`copyProperties` 去重和创造模式取材关系只要有一项非法，命令就以非零状态退出，不会把错误值补成默认值继续比较。

差异 JSON 的 `summary` 记录两个原文件的 SHA-256、两侧规则总数和启用数，随后固定按 Java 归一化规则 ID 排序，列出完整的新增、删除和修改规则。`enabled / disabled` 只表示两侧都存在的规则发生启停切换；`modified` 包含这些启停变化，因此计数可能重叠。比较字段为 `id / enabled / priority / target / catalyst / activation / result / creative`。省略 `--out` 时，JSON 直接输出到标准输出；指定文件时，控制台也会打印候选 SHA-256、规则总数和启用数。`--out` 经路径规范化和可用的真实路径解析后，不得与 `--baseline` 或 `--candidate` 指向同一文件；Windows 下比较不区分大小写，以免误覆盖部署配置或候选源文件。

仓库内的可复现小样本：

```powershell
npm.cmd run cli -- import `
  --snapshot "examples\sample-snapshot.json" `
  --recipes "examples\sample-recipe-data.json" `
  --loot-registry "examples\sample-loot-registry.json" `
  --presentations "examples\sample-presentation-drafts.json" `
  --out "examples\sample-output"
```

## 输入 snapshot

首选 Mod 的平铺格式：

```json
{
  "schema_version": "utd-asset-snapshot/v1",
  "producer": "utd_asset_manager",
  "exported_at": "2026-07-11T09:30:00+08:00",
  "items": [
    {
      "asset_key": "stable-key-from-mod",
      "variant_key": "stable-variant-key-from-mod",
      "registry_id": "tacz:modern_kinetic_gun",
      "identity_kind": "components",
      "variant_discriminator": "GunId=tacz:ak47",
      "client_name_zh_cn": "AK-47",
      "translation_key": "item.tacz.modern_kinetic_gun",
      "components_snbt": "{GunId:\"tacz:ak47\"}",
      "components_canonical": "完整观察快照",
      "identity_components_canonical": "GunId=tacz:ak47",
      "icon_data_url": "data:image/png;base64,..."
    }
  ]
}
```

兼容两种旧输入：平铺 `entries[]`，以及 `entries[] = { asset, status }` 包络。`asset_key` 和 `variant_key` 一旦由 Mod 提供，Workbench 绝不会从 observed components 重算。

`variant_discriminator` 与 Mod 使用同一优先级，只取第一个命中：

1. `GunId`
2. `AmmoId`
3. `AttachmentId`
4. `food_id`

## 输出

| 文件 | 用途 |
| --- | --- |
| `workbench.json` | 规范项目；网页与后续导出的权威输入 |
| `utd_recipe_data.generated.js` | 保留完整原始配方载荷的 KJS 数据 |
| `utd_loot_registry_data.generated.js` | 保留每条变体身份的 Loot KJS 数据 |
| `status_manifest.json` | 给 Mod / Tooltip / UI 的部署状态目录；固定目标为 `config/utd_asset_manager/status_manifest.json` |
| `utd_asset_status_manifest.json` | 兼容旧工具的同内容别名 |
| `utd_asset_status_manifest.js` | 同一状态目录的 `global.UTD` KJS 包装 |
| `utd_item_presentations.json` | 与游戏内工具共用的 `utd-item-presentation/v1` 草稿；顶层为 `drafts[]`，多行介绍为 `description_zh_cn[]` |
| `utd_lang_overlays.json` | 已启用显示覆盖的 `zh_cn` 条目，按 namespace 汇总 |
| `lang_overlays/<namespace>/zh_cn.json` | 可逐 namespace 审核/复制的标准语言 JSON；工作台不会自动部署 |
| `utd_block_transforms.json` | 可直接交给 Java 运行器的嵌套 `rules[]` 右键方块替换制造规则 |
| `utd_item_properties.json` | 稀有度、占格/容量、枪械和食品属性的统一审核契约 |
| `utd_asset_excel_interface.json` | README、Items、Recipes、Inputs、Outputs、Loot、Presentation、Block Transforms、Issues 工作表接口 |
| `manifest.json` | 输入源哈希、每个生成物哈希、计数与图范围 |

状态主契约位于 `items[]`：

```text
asset_key, registry_id, identity_kind, variant_discriminator,
catalogued, human_selected, recipe_input_count, recipe_output_count,
loot_enabled, loot_level, sync_state, catalog_hash, deployed_hash,
stale, issues
```

为旧消费者暂时附带 `item_key / managed / sync` 别名。新集成应使用上面的主字段。

分类字段为 `category_key / category_label_zh_cn / category_level`；网页可以与白名单、纳管、依赖、问题筛选及名称/ID/变体搜索组合使用。当前映射覆盖旧表中的基础资源、组件、可制作近战武器、枪械、装备、实用物品、工作台、工具与玩具。组件等级继续保留，可用来区分基础组件与高级材料。

`sync_state` 只允许：`local_only | pending | synced | stale | error`。外部依赖和孤儿状态分别通过 `source` 与 `issues` 表达，不占用 sync 枚举。

### 物品显示覆盖

在检查器“档案”页启用显示覆盖后，可以编辑中文名称和多行物品介绍，并选择：

- `registry`：覆盖整个注册物品；同 registry 的其它变体会解析并复用同一条覆盖，导出前按语义目标去重；
- `identity`：只覆盖当前精确变体。带 discriminator 的物品默认使用此范围。

FPE `pack_food` 必须固定使用 `identity`，界面、旧项目迁移和草稿导入都会阻止它退化为 registry 覆盖。它会从 `food_id` 自动派生变体语言键。例如 `food_id=firstpersonfoodeating:i_bang_a` 生成：

```text
item.firstpersonfoodeating.i_bang_a
tooltip.firstpersonfoodeating.i_bang_a.desc
```

这份数据只进入导出物，导入时观察到的名称和 translation key 始终原样保留。导出的 `utd_item_presentations.json` 可直接作为游戏内 `presentation_drafts.json` 使用，核心字段与 Mod 一致；网页工作台额外附带 `name_key / description_key`，游戏端会安全忽略这两个生成辅助字段。

### 方块替换制造

检查器“配方”页可用当前纳管物品作为 catalyst 新建规则，填写被右键的目标方块、替换结果、消耗数量、是否要求潜行，并启用或删除草稿。当前契约的安全默认值是：

```text
input_source=clicked_hand, hand=main,
cancel_interaction=true, consume_input=true,
block_entity_policy=reject, priority=100
```

`100` 是普通正式规则的默认优先级；`1000` 以上保留给短期测试或紧急覆盖。旧项目迁移仍按旧契约补 `priority=0`，不会悄悄改变既有规则顺序。

切换为从 `inventory` 取材时，页面会自动启用“必须潜行”；导入的旧规则若为“背包取材且无需潜行”也会产生 warning。启用规则的目标方块、结果方块和 catalyst 必须使用 `namespace:path`，数量至少为 1；同一目标状态、同一优先级的多条启用规则会被标为冲突。

未填完且保持禁用的草稿仍保存在 `workbench.json` 中并显示 warning，但不会进入运行配置；一旦启用，结构不完整就会成为 error 并阻断导出。规则 ID 按 Java 口径转为小写并检查大小写不敏感重复，避免一份看似有效的文件触发运行器整份 fail-closed。

导出文件与 Java 运行器完全对齐，核心形状如下：

```json
{
  "schema_version": "utd-block-transforms/v1",
  "rules": [{
    "id": "utd:block_transform/example",
    "enabled": true,
    "priority": 100,
    "target": { "block": "minecraft:stone", "state": {}, "blockEntityPolicy": "reject" },
    "catalyst": { "registryId": "minecraft:coal", "variantDiscriminator": "", "componentsSnbt": "{}", "count": 1, "source": "clicked_hand", "consume": true },
    "activation": { "hand": "main", "requireSneak": false, "allowFakePlayer": false },
    "result": { "block": "minecraft:deepslate", "state": {}, "copyProperties": [] },
    "creative": { "requireInput": true, "consume": false }
  }]
}
```

`reject` 表示目标位置存在 BlockEntity 时运行时必须拒绝替换，避免无意清空箱子等容器内容。

### v1 兼容与 FPE 回收配方

`presentations` 和 `blockTransforms` 是 `utd-asset-workbench/v1` 的可选增量字段。旧项目载入时会自动补为空数组；旧平铺规则会迁移并补齐 `priority=0`、空状态、`clicked_hand / main / reject`、禁止假玩家及创造模式安全默认值。旧的 `offhand / either` 会迁移为运行器使用的 `off / any`。

`forge:partial_nbt` 等 custom ingredient 的 `nbt` 会参与规范身份提取。FPE 回收配方因此按 `food_id` 分成精确变体，而不是全部坍缩到 `firstpersonfoodeating:pack_food`；匹配 Loot 后，已有 recipe variant 会补齐 `componentsSnbt`。

## 验证

```powershell
npm.cmd run check
```

当前 62 项测试覆盖：Mod 平铺/旧包络 snapshot、canonical/SNBT 无损保留、SHA-key plain 捕获与 base 配方语义合并、组件变体不降级、FPE/TaCZ 同 registry id 变体隔离、56 条 FPE partial-NBT 回收配方、Loot SNBT 合并、显示覆盖/语言键、Java 方块替换协议往返与严格差异、v1 增量字段兼容、轻量草稿恢复/上限保护、候选 ZIP 哈希、属性部署/回滚、输出路径保护、Windows 原子写回退、全量状态目录、一层过滤图、Tag 不展开、循环检测、冻结状态字段和 KJS 往返解析。

## 目录

```text
cli/                 数据导入与生成 CLI
src/domain/          schema、解析、规范化、图谱、导出器
src/components/      三栏工作台组件
tests/fixtures/      Mod 形状兼容样本
examples/            可复现的输入与生成输出
```

这个 MVP 不做双向 Excel 回写、不扫描 JAR/纹理/模型，也不修改第三方配方。外部记录只作为依赖证据存在。
