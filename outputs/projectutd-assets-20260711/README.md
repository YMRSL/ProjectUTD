# 誓死坚守资产 / 配方 / Loot MVP 交付包

- `mods/`：可安装的 `utd_asset_manager-0.1.4-test4.jar` 与未改动的 UTD Loot Core JAR。
- `workbench/workbench.json`：网页工作台的规范项目。
- `workbench/status_manifest.json`：复制到游戏 `config/utd_asset_manager/` 的状态清单。
- `workbench/utd_recipe_data.generated.js`：可重复生成的配方候选；本轮未自动覆盖运行时配方。
- `workbench/utd_loot_registry_data.generated.js`：Loot registry 的 KJS 兼容输出。
- `workbench/utd_item_presentations.json` 与 `utd_lang_overlays.json`：名称/多行介绍草稿及按 namespace 生成的中文语言覆盖接口。
- `workbench/utd_block_transforms.json`：与 Mod 运行器直接兼容的方块右键替换制造规则；当前为零条启用规则。
- `excel/誓死坚守_资产配方与Loot管理_20260711.xlsx`：独立审阅工作簿。
- `inputs/legacy_whitelist_ready.json`：从旧工作簿保守提取的 316 条初始人工根。
- `inputs/whitelist_union_20260712.json`：将上述历史标注与最新 14 条游戏导出按精确身份合并后的 327 条人工根。

test4 规范目录共 890 个精确身份、960 条配方、798 条 Loot 策略；56 个 FPE `food_id` 均带完整预览组件且都正确计入回收配方材料，目录内不存在重复的 `registry_id + variant_discriminator`。运行中的 Loot JAR 与 KubeJS 配方/Loot 脚本未被本轮覆盖。

双击 `Tools/UTDAssetWorkbench/打开UTD资产工作台.cmd` 可打开真实项目。test4 发布哈希、部署回执、已知边界与实机步骤见 `docs/ASSET_MANAGER_TEST_0.1.4_TEST4.md`；此前资产/Loot MVP 的发布记录仍保留在 `docs/RELEASE_20260711_ASSET_LOOT.md`。
