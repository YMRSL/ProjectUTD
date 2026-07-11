# UTD Asset Manager MVP

面向 ProjectUTD 的 NeoForge 1.21.1 客户端资产档案台。它继承旧 ItemNameCatch 的“由人明确选择物品”原则，但不扫描完整物品注册表，也不修改 KubeJS、Loot 或客户端部署目录。

**发布状态（2026-07-11）：** 源码、12 项自动测试和 JAR 构建已通过，JAR 与首份状态清单已安装到本地 NeoForge 21.1.233 客户端；完整整合包中的 O 键界面、Tooltip、标注持久化和 TaCZ/FPE 实机身份稳定性仍待用户验收。

## 当前能力

- `O` 或 `/utdasset open` 打开工业档案台三栏界面；
- 界面只显示已经人工标注的 variant、玩家背包内容，以及打开档案台前所在容器的可见槽位；
- 搜索名称、registry id、asset key、variant 类型；按人工标注、已纳管、配方、Loot、同步、异常分组；
- 在检查器内标注/取消标注，并可按当前搜索与状态筛选批量标注/取消；支持重载外部状态、导出 JSON；
- `/utdasset markhand`、`unmarkhand`、`export`、`reload`；
- 所有物品 Tooltip 实时显示 `human_selected`、`catalogued`、`recipe_input`、`recipe_output`、`loot_enabled`、`sync_state`、`stale`、`issues`；
- 保存客户端当前显示名、translation key、registry id、完整 ItemStack SNBT、原始 components SNBT、确定性 observed/identity canonical forms、asset key 和 variant key；非 `zh_cn` 客户端禁止标注与正式导出。

## 文件协议

运行后使用游戏目录下的 `config/utd_asset_manager/`：

- `whitelist.json`：Mod 持久化的人工白名单观察记录；
- `status_manifest.json`：外部资产管理器原子写回的只读状态（文件名必须完全一致）；
- `exports/utd-assets-*.json`：显式导出的白名单与合并状态。

`status_manifest.json` 的示例位于 `examples/status_manifest.example.json`。状态是正交字段，不以一个含混的“已完成”代替：配方输入/输出使用数量，Loot 带等级，同步同时携带 catalog/deployed hash 与 stale 标记。`syncState` 只允许 `local_only / pending / synced / stale / error`。

## Variant 身份

1. 将 ItemStack 数量归一为 1，并使用 1.21.1 registry provider 编码完整 ItemStack；
2. 原样保存 `componentsSnbt` 与完整 `componentsCanonical`，作为观察态审计快照；
3. TaCZ/FPE 身份只取稳定 discriminator：`GunId → AmmoId → AttachmentId → food_id`，避免弹量、射击模式或消耗耐久变化制造重复资产；
4. `assetKey = sha256(registryId + identityComponentsCanonical)`，`variantKey = sha256(identityComponentsCanonical)`；其它组件物品仍使用递归排序的完整 Components 身份。

因此 TaCZ 的 GunId/AmmoId/AttachmentId 与 FirstPersonFoodEating 的 food_id 会形成不同 variant，不会因共享基础 item id 合并，也不会因为运行态数值改变而重复。显示名取客户端当前语言下的 `ItemStack#getHoverName`，并同时保存 `capturedLocale`；若不是 `zh_cn`，Tooltip 会报错且仓库拒绝标注/导出，避免把英文误记为中文名。

## 故障保护

- 白名单写入采用同目录临时文件替换，并在每次成功更新前保留 `whitelist.json.bak`；
- 标注、取消和批量操作在写盘失败时回滚内存状态；
- 白名单损坏时进入只读保护，不会用空数据覆盖原文件；
- Manifest 先在临时索引中完整解析和检查重复键，再一次性替换内存状态；半写或损坏文件会继续使用 last-known-good 状态并显示问题；
- exact `asset_key` 优先；只允许唯一的 plain registry 或稳定 discriminator 回退，歧义回退会拒绝并显示 issue。

## 构建与测试

项目固定 NeoForge `21.1.233`、Minecraft `1.21.1`、Java 21。工作区内运行：

```powershell
.\gradlew.bat test build
```

本目录的轻量 wrapper 委托给相邻、已锁定版本的 FirstPersonFoodEating NeoForge wrapper，避免复制二进制 wrapper JAR。若要独立迁出仓库，再运行标准 Gradle wrapper 任务生成自带 wrapper。

## MVP 边界

- 不枚举全注册表，不自动收集所有 creative tab 物品；
- 不在 Mod 内生成 Excel 或 KubeJS；外部管理器消费导出 JSON，完成校验、KJS/Excel 生成和 diff；
- 不写入 `.minecraft/kubejs`、Loot 表或任何部署文件；
- 本轮未提供服务端共享目录或多人权限，面向本地策划客户端；
- `status_manifest.json` 必须使用精确 assetKey 回写，禁止仅按基础 registry id 给 TaCZ/FPE variant 套状态。
