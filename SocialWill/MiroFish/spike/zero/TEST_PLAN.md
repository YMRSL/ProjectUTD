# Spike Zero 游戏内验证手册（6 项，配套 spike_zero_cnpc.md §10）

> **只在测试世界/测试存档做，先备份。** 目标目录：单机 `<存档>\customnpcs\`，
> 服务器 `<世界名>\customnpcs\`。测试完把每项结果（PASS/FAIL+现象）发回编剧会话，
> 由它更新 spike 报告并解锁 `content_pack_format.md`。

## 测试 0 — 基准采集（最重要，先做）

1. 游戏内用 CNPC GUI 随手建：一个最小对话（任意分类、带一个选项）+ 一个物品收集任务。
2. 退出世界，在 `customnpcs\dialogs\` 和 `customnpcs\quests\` 里找到游戏自己写出的
   `.json`，**整个文件拷进本目录的 `reference\` 文件夹**。
3. 作用：这是 1.21.1 端的格式真值。我拿它 diff 校准假设文件（重点：物品列表
   `NpcMiscInv` 里 `Slot` 是 `0b` 还是 `0`、Availability 键是否必须写全、
   ModRev 是否仍为 18）。**如果测试 1/3 失败，答案多半就在这个 diff 里。**

## 测试 1 — 对话树外部投放 + 热加载

1. 把 `game_files\customnpcs\dialogs\mirofish\`（整个 `mirofish` 文件夹）拷进存档的
   `customnpcs\dialogs\` 下。
2. 进游戏执行 `/noppes dialog reload`。
3. 执行 `/noppes dialog show @p 100001 "MiroFish Dialect Probe"`（探针 NPC 未刷出时
   名字随便填，只是显示名）。
4. **预期**：打开对话 100001；选项一跳到 100002（树状跳转 ✓）；100002 声明发放任务。
   分类 `mirofish` 是文件夹名自动发现的（源码已证 1.20.1 如此）——若 reload 后
   看不到，在 GUI 里手建同名分类再 reload 一次，并记录"需要预建分类"。

## 测试 2 — 探针 NPC（真换行脚本 + clone 投放）

1. 把 `game_files\customnpcs\clones\2\MiroFish Dialect Probe.json` 拷进存档的
   `customnpcs\clones\2\`（没有 `2` 文件夹就新建；**别改文件名**）。
2. 需要 Fungal 资源包已启用（探针以感染者模型渲染，无害：阵营已改 0=友好）。
3. 执行 `/noppes clone spawn "MiroFish Dialect Probe" 2`。
4. **预期**：约每 10 秒喊一次 `[probe] slot-2 tick ok @ <时间>`（第二脚本槽 + 真换行
   存活 ✓）；右键出现 `[probe] slot-2 interact ok...` 和交互台词
   `[probe] interact line ok`；原 Fungal 迁徙/挖墙行为仍在（第一脚本槽方言在
   1.21.1 正常 ✓）。任何 `[ERR ...]` / `[ERR2 ...]` 原样抄回来。

## 测试 3 — 物品收集任务（原版物品）

1. 把 `game_files\customnpcs\quests\mirofish\100001.json` 拷进 `customnpcs\quests\mirofish\`。
2. `/noppes quest reload`（**记录这条命令是否存在**——源码只确证了 dialog reload，
   quest 侧是待验项；不存在就重进世界代替）。
3. 走测试 1 流程读到对话 100002 → 接到任务「[MF探针] 腐肉回收」。
4. 集 3 块腐肉，右键探针 NPC 交付。
5. **预期**：任务完成，腐肉被收走，得 1 面包 + 10 经验。

## 测试 4 — 模组物品匹配

1. 编辑 `quests\mirofish\100002.json`：把 `REPLACE_WITH_MODDED_ITEM_ID` 换成一个
   实际会掉落的模组物品 id（F3+H 开高级提示后看物品栏；例如 superb/sable/golem
   残骸类，任选一个好获取的）。
2. 拷入 + reload，同测试 3 流程（这个任务需要 GUI 或对话另挂一次——最简单：
   GUI 里把探针 NPC 的另一对话槽挂上它，或直接在 GUI 给玩家发任务
   `/noppes quest start` 类命令若存在，记录之）。
3. **预期**：模组物品被识别、可交付（`IgnoreNBT/IgnoreDamage` 已设 1b）。

## 测试 5 — 回执通道探针（ask-8 前置）

1. 先执行 `/scoreboard objectives add mf_records dummy`。
2. 把 `player_script_probe.js` 的内容粘进 CNPC **全局玩家脚本**（GUI 路径类似
   Global → Player Script，语言选 ECMAScript；**具体入口在哪也是记录项**）。
3. 再交付一次任意探针任务（任务可重复测试就重接，或用 100002）。
4. **预期**：聊天栏出现 `[probe] questTurnIn fired, quest=…` 与
   `questCompleted fired`；若 `executeCommand ok` 出现，则计分板
   `mf_records +1` —— 回执通道全通。若提示 `executeCommand unavailable`，
   把报错原文抄回（说明要换 API 写法，钩子本身仍算通）。

## 结果回报格式

| # | 项 | PASS/FAIL | 现象/报错原文 |
|---|---|---|---|
| 0 | 基准文件已拷入 reference\ | | |
| 1 | 对话投放+跳转 | | |
| 2 | 探针 NPC 双槽脚本 | | |
| 3 | 原版物品任务 | | |
| 4 | 模组物品任务 | | |
| 5 | questTurnIn 钩子 | | |

全 PASS ⇒ spike zero 关闭，`content_pack_format.md` 开写；任何 FAIL ⇒ 带上
reference\ 里的基准文件回来，我改格式再发一版。
