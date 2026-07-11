# UTD Asset Workbench

“誓死坚守”的本地物品、配方与 Loot 档案台。它把 Mod 导出的白名单 snapshot、当前 `utd_recipe_data.js`、Loot registry/balance 汇入一份规范项目，并提供三栏网页检查器、状态回写清单、KJS 和 Excel 单向接口。

## 当前边界

- 状态目录是全量的：包含白名单身份、所有 KJS 配方中的 item 输入/产出，以及每一条 Loot identity。
- 可视图是严格限流的：只显示 `human_selected` 白名单根、产出根物品的配方，以及这些配方的一层直接依赖。
- Tag 只保留引用节点，不读取或展开 tag 内容。
- UTD-owned / catalogued 记录可编辑；纯外部依赖灰显且只读。
- FPE、TaCZ 等变体先按 `asset_key` 精确匹配，再按单一 `variant_discriminator` 匹配；变体绝不退化为 base registry id 匹配。
- Excel 目前是单向审阅接口 JSON；实际 `.xlsx` 由上层导出器生成，Excel 不是权威源。

## 本地启动

在 Windows 中可直接双击 `打开UTD资产工作台.cmd`。它会从仓库 `outputs/projectutd-assets-20260711/workbench/workbench.json` 装载当前真实项目，自动检查依赖、构建最新页面、启动本地服务并打开浏览器；已经启动时只会复用现有页面。

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

## 从真实运行数据导入

CLI 能直接解析纯 JSON，也能安全抽取当前约 3 MB 的 KJS 赋值格式，例如 `global.UTD... utd.recipeData = { ... };`。

```powershell
npm.cmd run cli -- import `
  --snapshot "D:\path\to\utd_asset_snapshot.json" `
  --recipes "D:\path\to\kubejs\startup_scripts\utd_recipe_data.js" `
  --loot-registry "D:\path\to\kubejs\startup_scripts\utd_loot_registry_data.js" `
  --loot-balance "D:\path\to\kubejs\startup_scripts\utd_loot_balance_data.js" `
  --out "artifacts\current" `
  --public "public\data\workbench.json" `
  --generated-at "2026-07-11T19:50:00+08:00"
```

`generated_at` 表示本次工作台发布时刻；配方、snapshot 等各输入自己的旧时间仍单独保存在 `manifest.source`，不会混成发布日期。

也可从已编辑的规范项目重新生成所有输出：

```powershell
npm.cmd run cli -- export --project "artifacts\current\workbench.json" --out "artifacts\regenerated"
npm.cmd run cli -- validate --project "artifacts\regenerated\workbench.json"
```

仓库内的可复现小样本：

```powershell
npm.cmd run cli -- import `
  --snapshot "examples\sample-snapshot.json" `
  --recipes "examples\sample-recipe-data.json" `
  --loot-registry "examples\sample-loot-registry.json" `
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
      "identity_components_canonical": "GunId=tacz:ak47"
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
| `utd_asset_excel_interface.json` | README、Items、Recipes、Inputs、Outputs、Loot、Issues 工作表接口 |
| `manifest.json` | 输入源哈希、每个生成物哈希、计数与图范围 |

状态主契约位于 `items[]`：

```text
asset_key, registry_id, identity_kind, variant_discriminator,
catalogued, human_selected, recipe_input_count, recipe_output_count,
loot_enabled, loot_level, sync_state, catalog_hash, deployed_hash,
stale, issues
```

为旧消费者暂时附带 `item_key / managed / sync` 别名。新集成应使用上面的主字段。

`sync_state` 只允许：`local_only | pending | synced | stale | error`。外部依赖和孤儿状态分别通过 `source` 与 `issues` 表达，不占用 sync 枚举。

## 验证

```powershell
npm.cmd run check
```

测试覆盖：Mod 平铺/旧包络 snapshot、canonical/SNBT 无损保留、FPE/TaCZ 同 registry id 变体隔离、discriminator 优先级、全量状态目录、一层过滤图、Tag 不展开、循环检测、冻结状态字段、KJS 往返解析。

## 目录

```text
cli/                 数据导入与生成 CLI
src/domain/          schema、解析、规范化、图谱、导出器
src/components/      三栏工作台组件
tests/fixtures/      Mod 形状兼容样本
examples/            可复现的输入与生成输出
```

这个 MVP 不做双向 Excel 回写、不扫描 JAR/纹理/模型，也不修改第三方配方。外部记录只作为依赖证据存在。
