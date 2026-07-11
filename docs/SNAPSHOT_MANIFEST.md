# 安全快照清单（2026-07-11）

分支：`codex/sprint-baseline-20260711`

远端 `YMRSL/ProjectUTD` 当前为公开仓库。本清单用于区分可以公开保存的自研成果与必须留在本地等待来源、许可或存储策略确认的内容。

## 本次公开快照范围

1. 项目管理：`.gitignore`、`README.md`、`docs/PROJECT_STATUS.md`、本清单。
2. Picasso：源码、测试、数据定义、生成脚本、锁文件和技术文档。
3. Chess：README、架构和技术文档；不含图片素材。
4. MiroFish：README、架构、技术文档和测试手册；暂不含运行态 CNPC fixture。
5. 物品/合成表：原始 v1 工作簿与后续生成的可读 v2 工作簿，二者都作为小型项目源文件跟踪。

## 明确排除

- `UtilWeDie-Neo-1.21.1/建筑存档/`：世界、Voxy、Distant Horizons 与地图缓存；本次按文件逻辑大小统计约 3.65 GB（GreenField 约 3.20 GB、Mosslorn 约 433.7 MB、Picasso 测试副本约 11.3 MB）。
- `.minecraft/`、PCL、版本库、日志、崩溃报告、构建输出、Gradle/Python 缓存。
- 根目录大型压缩包及 jar/zip/rar/7z 等分发物。
- `CNPCScripts/CnpcScript/`：混合 1.12.2、1.20.1、1.21.1 的参考库，含第三方署名和危险示例，需先分类与确认公开许可。
- CNPC Fungal 音频、贴图，以及 Chess 货币/品牌图片：来源与许可确认前不进入公开仓库。
- Word、视频和其他二进制参考资料。

## 仍保留在本地、后续处理

| 区域 | 后续动作 |
| --- | --- |
| CNPC 参考脚本库 | 分为 `archive/1.12.2`、`archive/1.20.1`、`active/1.21.1`、`experimental-dangerous`，补 manifest 与来源字段 |
| 世界与地图 | 定义“运行本 / 测试本 / 归档本”，使用私有备份或对象存储，不放普通 Git |
| 图片、音频、模型 | 建资产来源与许可证清单；确认后再决定 Git LFS、Release 或私有存储 |
| 运行态 CNPC fixture | 去除第三方派生字段、确认来源后再公开 |

## 恢复原则

- 每个模块独立提交，避免一次提交混入不同来源。
- 提交前检查暂存文件列表和最大文件；任何世界、缓存或秘密出现即停止。
- 功能快照不代表已完成；模块状态仍以 `docs/PROJECT_STATUS.md` 为准。
