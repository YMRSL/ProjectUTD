# PORT_REPORT · FTB Quests Localizer

**结论:判废,不移植(2026-06-12,Opus 侦察 agent 调研 + 主线确认)**

## 形态
运行时 Java mod(进游戏把 FTB Quests 任务书内联文本抽成 lang 文件)。全渠道止步 **MC 1.20.4**(GitHub 分支 1.18.2/1.19.x/1.20.x/1.20.4;Modrinth/CurseForge 最新 = ftbquestlocalizer-1.20.4-neoforge-3.2.3)。无 1.21.x 任何构建。

## 为什么不移植
**功能已被 FTB Quests 官方原生取代**:FTB Quests 2100.1.0+(本包 = 2101.1.25)重写本地化——翻译文本独立存为 `config/ftbquests/quests/lang/<locale>.snbt`,加载旧任务书**自动迁移**生成 en_us.snbt,fallback locale 可在任务书属性配置。老 Localizer 假设 1.20.x 旧数据结构(文本内联),硬塞 1.21 **有损坏任务书的风险**。

## 替代路线(零 mod)
1. 进游戏打开一次任务书 → 自动生成 `config/ftbquests/quests/lang/en_us.snbt`;
2. 复制为 `zh_cn.snbt` 翻译(可用 mc-questing-mod-localizer.streamlit.app/ftbq_new 的 1.21+ 专版辅助);
3. 随包分发;主语言非英文记得改任务书 fallback locale。
4. 操作前备份 `config/ftbquests/`。

## 备选现成 mod(无需移植,装 jar,1.21.1 可用性需到页面核实)
FTB Quests Precision Localizer(TimErmolt,同源维护版)/ Re-FTBQLocalizationKeys(/ftbqkey 导出)/ FTB Quest Translator 1.4.0(注意:客户端即时翻译,不导出 lang,用途不同)。

证据链:GitHub 分支列表 + FTB 官方 CHANGELOG + FTB-Mods-Issues #1597/#647 + 多源版本上限一致。
