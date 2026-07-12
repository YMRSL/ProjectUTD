# UTD Asset Manager MVP

面向 ProjectUTD 的 NeoForge 1.21.1 资产档案台；界面与采集在客户端运行，同时附带默认无规则的公共侧方块替换制造运行器。它继承旧 ItemNameCatch 的“由人明确选择物品”原则，但不扫描完整物品注册表，也不修改 KubeJS、Loot 或客户端部署目录。

**发布状态（2026-07-12）：** `0.1.2-test2` 已通过用户完整实机验收；`0.1.3-test3` 的项目目录主体已通过实机测试，并暴露 FPE 预览与长搜索问题；`0.1.4-test4` 已通过 43 项自动测试，等待本轮定向实机验收。

## 当前能力

- `O` 或 `/utdasset open` 打开工业档案台三栏界面；
- 默认“本机”页显示已经人工标注的 variant、玩家背包内容，以及打开档案台前所在容器的可见槽位；
- 只读“项目目录”页浏览完整 `status_manifest.json`，并把项目历史标注与本机白名单明确分开；
- 搜索名称、registry id、asset key、variant 类型和稳定 discriminator，搜索框支持 512 字符并对长 `food_id` 自动横向滚动；按人工标注、项目管理范围、配方、Loot、同步、异常分组；
- 项目目录会按 FPE `food_id` 构造只读预览栈，直接显示资源包中的实际中文名与贴图；界面用 `food_id=i_bang_a` 短标识展示，完整值仍可搜索和导出；
- 在检查器内标注/取消标注，并可按当前搜索与状态筛选批量标注/取消；支持重载外部状态、导出 JSON；
- 检查器可编辑游戏内中文名和多行物品介绍，草稿即时用于资产界面与 Tooltip 预览，并与物品 Components 身份隔离；
- `/utdasset markhand`、`unmarkhand`、`export`、`reload`；
- 所有物品 Tooltip 实时显示本机 `human_selected`、`catalogued`、`recipe_input`、`recipe_output`、`loot_enabled`、`sync_state`、`stale`、`issues`；项目历史人工标注在只读目录中单独显示；
- 保存客户端当前显示名、translation key、registry id、完整 ItemStack SNBT、原始 components SNBT、确定性 observed/identity canonical forms、asset key 和 variant key；非 `zh_cn` 客户端禁止标注与正式导出。

## 文件协议

运行后使用游戏目录下的 `config/utd_asset_manager/`：

- `whitelist.json`：Mod 持久化的人工白名单观察记录；
- `status_manifest.json`：外部资产管理器原子写回的只读状态（文件名必须完全一致）；
- `presentation_drafts.json`：游戏内检查器保存的中文名称/介绍编辑草稿，schema 固定为 `utd-item-presentation/v1`；
- `block_transforms.json`：可选的方块右键替换制造规则，`schema_version` 固定为 `utd-block-transforms/v1`（兼容旧 `schema` 字段）；首次运行只创建空 `rules`，不会自动启用示例；
- `exports/utd-assets-*.json`：显式导出的白名单与合并状态；
- `exports/utd-presentation-drafts-*.json`：显式导出的名称/介绍草稿，供桌面工具审查和生成资源文件。

`status_manifest.json` 的示例位于 `examples/status_manifest.example.json`。状态是正交字段，不以一个含混的“已完成”代替：配方输入/输出使用数量，Loot 带等级，同步同时携带 catalog/deployed hash 与 stale 标记。`syncState` 只允许 `local_only / pending / synced / stale / error`。

名称/介绍草稿精确保存 `asset_key`、`registry_id`、`variant_discriminator`，并以 `apply_scope = identity | registry` 明确“仅此变体”或“同注册物品全部变体”的应用范围。每条草稿还包含观察到的中文名、目标中文名、按行保存的中文介绍、启用状态、编辑时的 catalog hash 和更新时间。解析时先匹配 exact asset key，再以唯一的 `registry_id + variant_discriminator` 桥接项目目录 synthetic key 与游戏捕获 SHA key，最后才使用 registry 级草稿；任何语义歧义都会在加载或写入时被拒绝。禁用草稿保留供编辑，但不参与有效值解析。

## 方块右键替换制造（P0）

`block_transforms.json` 的完整禁用示例位于 `examples/block_transforms.example.json`。规则按 `priority` 从高到低匹配，只有显式写成 `enabled: true` 才会运行。`target.state` 是目标方块状态的部分匹配；`result.copyProperties` 先从目标复制同名兼容属性，再由 `result.state` 的显式值覆盖。默认激活方式是主手右键，材料来源是当前点击手，数量为 1，并在成功后消耗。`source: inventory` 会改为检查并消耗玩家 36 格主背包（包括快捷栏，不包括盔甲栏和副手栏）。

材料始终先匹配 `registryId`；非空 `variantDiscriminator` 再匹配 TaCZ/FPE 的稳定变体标识，非空且不为 `{}` 的 `componentsSnbt` 则精确匹配完整 `components` Compound。创造模式以 `creative.requireInput` 和 `creative.consume` 单独决定是否需要、是否消耗材料；为避免无法兑现的规则，`consume: true` 不允许与 `requireInput: false` 同时出现。

运行器监听最低优先级的 `RightClickBlock`，不会接管已被其它 Mod 取消的交互。服务端在同一位置进行单方块临时替换，NeoForge `EntityPlaceEvent` 未被保护 Mod 取消且材料再次校验通过后才提交邻居/客户端更新，随后保留材料扣除；失败会恢复方块且不扣材料。成功返回 `sidedSuccess`（客户端 `SUCCESS`、服务端 `CONSUME`），命中但被权限或放置保护拒绝时返回 `FAIL`，两者都会阻止原方块交互；同一玩家、维度、位置、规则和游戏 tick 的重复事件会复用首次结果。未加载区块、越界位置、冒险/观察者限制、`mayUseItemAt`、出生点/世界边界权限、FakePlayer（除非显式允许）、目标或结果 BlockEntity，以及作为目标或产物的门、床、双格植物、活塞头等多方块结构都会被拒绝。

客户端持有相同规则文件时会预先取消原交互，因此单人测试可直接使用。当前没有服务端到客户端的规则同步；专用服务器必须人工保持两端 `block_transforms.json` 一致，正式多人部署前应补协议同步与权限审计。P0 使用 NeoForge 标准放置事件兼容常见保护 Mod，但无法回滚第三方事件监听器在取消前自行产生的任意外部副作用，因此仍需在目标服务端做保护 Mod 联调。

## Variant 身份

1. 将 ItemStack 数量归一为 1，并使用 1.21.1 registry provider 编码完整 ItemStack；
2. 原样保存 `componentsSnbt` 与完整 `componentsCanonical`，作为观察态审计快照；
3. TaCZ/FPE 身份只取稳定 discriminator：`GunId → AmmoId → AttachmentId → food_id`，避免弹量、射击模式或消耗耐久变化制造重复资产；
4. `assetKey = sha256(registryId + identityComponentsCanonical)`，`variantKey = sha256(identityComponentsCanonical)`；其它组件物品仍使用递归排序的完整 Components 身份。

因此 TaCZ 的 GunId/AmmoId/AttachmentId 与 FirstPersonFoodEating 的 food_id 会形成不同 variant，不会因共享基础 item id 合并，也不会因为运行态数值改变而重复。显示名取客户端当前语言下的 `ItemStack#getHoverName`，并同时保存 `capturedLocale`；若不是 `zh_cn`，Tooltip 会报错且仓库拒绝标注/导出，避免把英文误记为中文名。

## 故障保护

- 白名单写入采用同目录临时文件替换，并在每次成功更新前保留 `whitelist.json.bak`；
- 名称/介绍草稿同样采用原子替换并保留 `presentation_drafts.json.bak`；文件损坏时进入只读保护并继续使用 last-known-good，绝不会用空草稿覆盖原文件；
- 标注、取消和批量操作在写盘失败时回滚内存状态；
- 白名单损坏时进入只读保护，不会用空数据覆盖原文件；
- Manifest 先在临时索引中完整解析和检查重复键，再一次性替换内存状态；半写或损坏文件会继续使用 last-known-good 状态并显示问题；
- 项目目录没有完整 ItemStack 时不会再尝试解析空 `{}`，避免每次浏览制造 `Tried to load invalid item` 日志洪泛；
- 相同损坏版本只解析并报告一次，避免每秒重复读取完整 Manifest；手动“重载”仍可强制重试；
- exact `asset_key` 优先；只允许唯一的 plain registry 或稳定 discriminator 回退，歧义回退会拒绝并显示 issue。

## 构建与测试

项目固定 NeoForge `21.1.233`、Minecraft `1.21.1`、Java 21。工作区内运行：

```powershell
.\gradlew.bat test build
```

本目录的轻量 wrapper 委托给相邻、已锁定版本的 FirstPersonFoodEating NeoForge wrapper，避免复制二进制 wrapper JAR。若要独立迁出仓库，再运行标准 Gradle wrapper 任务生成自带 wrapper。

## MVP 边界

- 不枚举全注册表，不自动收集所有 creative tab 物品；项目目录只消费外部工具明确写入的 Manifest；
- 不在 Mod 内生成 Excel 或 KubeJS；外部管理器消费导出 JSON，完成校验、KJS/Excel 生成和 diff；
- 名称/介绍编辑只保存部署草稿，不把 `custom_name`、`lore` 或其它展示字段写回 ItemStack Components；这样不会改变 TaCZ/FPE 或其它 component-sensitive 物品的资产身份。最终语言文件/Tooltip 脚本仍由外部管理器生成并经人工确认后部署；
- 不写入 `.minecraft/kubejs`、Loot 表或任何部署文件；
- 本轮未提供服务端共享目录或多人权限，面向本地策划客户端；
- `status_manifest.json` 必须使用精确 assetKey 回写，禁止仅按基础 registry id 给 TaCZ/FPE variant 套状态。
