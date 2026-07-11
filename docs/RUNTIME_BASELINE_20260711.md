# Runtime KubeJS 基线（2026-07-11）

在资产管理器、配方与 Loot 修改开始前保存的只读本地恢复点。

- 客户端：`UtilWeDie-Neo-1.21.1/.minecraft/versions/1.21.1-NeoForge_21.1.233`
- 本地快照：`_local_snapshots/runtime-kubejs-baseline-20260711.zip`
- SHA-256：`DED3D9882C5EA821E7FFBD602BB137AE9F0BF4784331951CA548569A1874042A`
- 压缩包大小：567,616 bytes
- KubeJS 文件：404
- KubeJS 原始逻辑大小：4,921,595 bytes

快照包含完整 `kubejs/`，以及本次审计使用的 `latest.log`、`customnpcs-latest.log` 和 `crafttweaker.log`。压缩包仅保存在本机并由 `.gitignore` 排除；本文件只记录恢复证据，不包含运行时内容。

后续部署前必须满足：

1. 生成结果可与本基线做文件级 diff。
2. 新版 KJS/JAR 通过离线校验和测试客户端验证。
3. 未通过验收时能够恢复本快照。
