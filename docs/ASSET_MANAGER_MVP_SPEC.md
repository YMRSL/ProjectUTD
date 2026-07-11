# UTD 游戏物品资产管理 MVP 规格

> 冻结日期：2026-07-11。本文是今天实现与验收的统一口径。

## 1. 目标

建立一条可恢复、可审查的闭环：

`游戏内人工标注 → 客户端采集 → 桌面审查/配方树 → 生成 KJS/Excel/status → 部署 → 游戏内实时反馈`

不扫描和纳管整合包全部物品。只有用户明确标注的物品进入管理范围；配方直接依赖可以作为只读外部叶节点出现。

## 2. 系统边界

### 游戏内 Mod

- 提供可搜索、可筛选的资产管理 Screen。
- 支持单个和批量标注/取消标注。
- 采集客户端 `zh_cn` 显示名、翻译键、registry id、Components/variant 和来源模组。
- 读取桌面工具生成的 status manifest。
- 在管理 Screen 与物品 Tooltip 中实时显示纳管状态。
- 导出白名单 snapshot，不直接生成 Excel，也不直接改 KubeJS 部署文件。

### 桌面资产工作台

- 导入白名单 snapshot、当前 UTD 配方与 Loot 数据。
- 只为白名单根节点建立配方关系；未标注材料作为灰色只读叶节点。
- 原版和其他 Mod 配方只读；只有 UTD-owned 内容可编辑与生成。
- 生成规范 JSON、现有格式 KJS、可读 Excel 和回写 Mod 的 status manifest。
- 所有部署前提供验证报告和 diff。

### Loot Core JAR

- 将 UTD 自管 Loot 数据、校验和保底逻辑从散落 KubeJS/JSON 封装成独立 NeoForge 1.21.1 JAR。
- 源码位于 `UtilWeDie-Neo-1.21.1/ModsSourceCode/UTDLootCore`。
- 通过实机验收前不删除旧 KubeJS 文件；切换采用可回滚部署。

## 3. 物品身份

`registry_id` 只能保存纯 `namespace:path`，不能混入 NBT/SNBT。

同一 registry item 的不同变体必须分别保存：

- `asset_key`
- `registry_id`
- `identity_kind`
- `variant_key`
- `components_json`
- `client_name_zh_cn`
- `translation_key`
- `mod_id`

TaCZ 枪械至少保留 `GunId`；FirstPersonFoodEating 至少保留 `food_id`。Components 必须规范排序后再生成稳定 hash。

## 4. 实时状态模型

状态是正交字段，不合并成单一布尔值：

| 字段 | 含义 |
| --- | --- |
| `human_selected` | 用户已在游戏内标注 |
| `catalogued` | 已进入桌面规范资产库 |
| `recipe_input_count` | 作为 UTD 配方材料的数量 |
| `recipe_output_count` | 作为 UTD 配方产物的数量 |
| `loot_enabled` | 已进入 UTD Loot 注册与生成链 |
| `loot_level` | 当前 Loot 等级 |
| `sync_state` | `local_only / pending / synced / stale / error` |
| `issues` | 缺失、重复、namespace 漂移、生成/部署差异等问题 |
| `catalog_hash` | 桌面规范资产库版本 |
| `deployed_hash` | 最近部署版本 |

建议 Tooltip：

```text
UTD资产：已标注 · 已同步
配方：产物 2 / 材料 5
Loot：已启用 · Lv3
问题：0
```

## 5. 配方图规则

- 白名单物品是根节点。
- 默认展开直接生产配方与直接使用关系。
- 未标注材料只做只读外部叶，不自动加入白名单。
- Tag 保持 `#namespace:path` 节点，不展开所有候选物品。
- 回收和互相转换必须检测循环，并显示回链而不是无限递归。
- 一个产物允许多个配方方案；明确主方案、备选、禁用和外部只读来源。
- KJS 生成保持现有 `utd_recipe_data.js` 数据形状，注册器代码与生成数据分离。

## 6. Loot P0 完成线

- 798 条 registry seed 在运行期实际初始化，不再是 0。
- 280 个容器映射使用 `doomsday_decoration`。
- `locks/paraglider/ropebridge/the_ravenous` 14 个缺失引用被禁用或条件化。
- 4 个含 `paraglider:paraglider` 的 tier1 表恢复加载。
- 175 个受影响容器不再报告 UTD helper 表缺失。
- 78 helper tier 与 280 容器表可确定性重建。
- UTD 自管 Loot 解析失败归零；第三方 Railways/ZSK/Tracks 等错误单列，不混入完成线。

## 7. 今日 MVP 验收

1. 游戏内 Screen 能打开，显示已标注与纳管状态。
2. 标注、取消、重新打开客户端后状态仍保留。
3. 中文名、翻译键、ID 与 Components 能正确导出。
4. TaCZ/FPE 变体不会被错误合并。
5. 桌面网页能按白名单显示配方树、外部依赖、Loot 与问题。
6. status manifest 回写后，游戏 Tooltip/Screen 实时反映配方、Loot 和同步状态。
7. KJS 连续生成两次 hash 一致，并保持当前配方数量口径可解释。
8. Excel 至少包含 manifest、items、recipes、inputs、outputs、policies、loot、issues，且数量可对账。
9. Loot P0 通过离线校验和一次测试客户端启动/开箱验证。
10. UTD Asset Manager 与 UTD Loot Core 均产出可安装 JAR；旧运行文件保留可回滚。

## 8. 今日不纳入

- 全量整合包物品纳管。
- 双向 Excel 冲突合并。
- 完整游戏内配方编辑器。
- 全部第三方 Loot 错误修复。
- 所有 Create/TaCZ 自定义配方类型的可视化编辑。
- UI 美术资源全量重做。
