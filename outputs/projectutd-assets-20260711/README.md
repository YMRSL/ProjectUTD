# 誓死坚守资产 / 配方 / Loot MVP 交付包

- `mods/`：可安装的 UTD Asset Manager 与 UTD Loot Core JAR。
- `workbench/workbench.json`：网页工作台的规范项目。
- `workbench/status_manifest.json`：复制到游戏 `config/utd_asset_manager/` 的状态清单。
- `workbench/utd_recipe_data.generated.js`：可重复生成的配方候选；本轮未自动覆盖运行时配方。
- `workbench/utd_loot_registry_data.generated.js`：Loot registry 的 KJS 兼容输出。
- `excel/誓死坚守_资产配方与Loot管理_20260711.xlsx`：独立审阅工作簿。
- `inputs/legacy_whitelist_ready.json`：从旧工作簿保守提取的 316 条初始人工根；后续由游戏内真实标注替换。

双击 `Tools/UTDAssetWorkbench/打开UTD资产工作台.cmd` 可打开真实项目。完整发布哈希、部署回执、已知内容问题与实机步骤见 `docs/RELEASE_20260711_ASSET_LOOT.md`。
