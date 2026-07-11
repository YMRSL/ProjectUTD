# ProjectUTD｜誓死坚守

Minecraft 1.21.1 社会模拟玩法“誓死坚守”的开发工作区，包含 Social Will、模组移植、本体脚本、物品/配方/Loot、CNPC 内容与设计资料。

## 项目入口

- [两周冲刺总控台](docs/PROJECT_STATUS.md)：优先级、状态、阻塞、负责人和验收标准。
- [项目文件与成熟度总清单](docs/PROJECT_INVENTORY.md)：文件角色、五维进度、数据流、已知漂移与安全整理顺序。
- [安全快照清单](docs/SNAPSHOT_MANIFEST.md)：公开快照包含/排除范围和恢复原则。
- [可读物品与合成工作簿 v2](outputs/projectutd-sprint-20260711/ItemNameCatch_物品与合成设计_v2.xlsx)：策划与审计视图；不是最终权威源。

## 当前恢复基线

公开安全快照位于 `codex/sprint-baseline-20260711`。Picasso 源码/测试、Chess 与 MiroFish 规格、项目治理文档和经审查的工作簿已分批保存。

本仓库是源码与小型设计数据仓库，不是完整 Minecraft 客户端或世界备份。`.minecraft`、世界/地图、JAR/压缩包、构建缓存，以及来源或许可未确认的 CNPC/图片/音频素材均保持在公开 Git 之外。

## 当前执行原则

- 项目级状态只在总控台维护；模块技术设计仍以各模块架构与交接文档为准。
- 权威源到游戏部署采用 `validate → generate → diff → deploy → receipt` 单向链。
- 不把生成物、运行日志或部署文件当作权威源。
- 未跟踪/忽略区域逐批审查，不整目录暂存；世界和素材在有可验证备份前不移动、不删除。
