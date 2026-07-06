# Fungal 僵尸 CNPC 配置卡

地基已验证:CNPC-Gecko-Addon + 资源包 `fungal_cnpc_test`(已含 4 怪资产)。infected 单怪全通过。

## 通用配置步骤(每个怪一样)
1. 拿 NPC 万能棒(`/give @s customnpcs:npcwand`)右键 NPC → **外观设置**。
2. **模型** 行 → **编辑** → 实体列表滚到 **「Geckolib Model」** 单击选中。
3. 选中后右上出现三个按钮:
   - **Model** → 点开,双击选对应 `*.geo.json`
   - **Model Animation** → Edit → 三栏分别 select:Animation File / Idle / Walk(见下表)
   - **Extras** → 默认(Head Bone 若想跟视线可填 `Head`,本批模型多数没有头骨跟随,可不管)
4. **材质** 行(关键!):
   - 先点材质行**最右的三态按钮**,切到 **「材质」模式**(不能是 URL/玩家 —— 否则回退史蒂夫);
   - 在材质**文本框**手填路径(见下表),点别处失焦写入。
5. 关界面保存。

## 4 怪完整版配置表

| 怪 | Model(geo) | Animation File | Idle | Walk | 材质栏(切"材质"模式后填) |
|---|---|---|---|---|---|
| **infected**(普通杂兵) | `fungalcnpc:geo/infected.geo.json` | `fungalcnpc:animations/infected.animation.json` | `idle` | `walk` | `fungalcnpc:textures/entity/infected1.png` |
| **lurker**(小蜘蛛/跳扑骑乘) | `fungalcnpc:geo/lurker.geo.json` | `fungalcnpc:animations/lurker.animation.json` | `idle` | `walk` | `fungalcnpc:textures/entity/lurker.png` |
| **sporer**(孢子辅助) | `fungalcnpc:geo/sporer.geo.json` | `fungalcnpc:animations/sporer.animation.json` | `idle` | `walk` | `fungalcnpc:textures/entity/sporer.png` |
| **volatile**(精英) | `fungalcnpc:geo/volatile.geo.json` | `fungalcnpc:animations/volatile.animation.json` | `idle` | `walk` | `fungalcnpc:textures/entity/volatile.png` |

⚠️ 注意:**infected 贴图是 `infected1.png`(带个 1)**,其余三个是 `lurker/sporer/volatile.png`。

## 各怪可用动画名(后续脚本/精英技/断肢替身要用)
- **infected**:idle walk run stand attack attack2 death death2 death3 fall **fall_to_creep creep creep_idle creep_attack creep_death**
- **lurker**:idle walk jumping start_jump **start_riding riding** death1 death2
- **sporer**:idle walk attack attack2 death death2 fall **fall_to_creep creep creep_idle creep_attack creep_death**
- **volatile**:idle walk sprint attack attack2 **guard guard_walk parry start_dodge end_dodge stagger execution execution_ground** start_jump riding ground death

## 断肢爬行替身思路(下一步,你配完完整版后做)
完整僵尸 `died` 脚本 → `world.spawnClone` 一个"爬行替身"NPC:
- 替身用**同一个 geo 模型** + **idle 设 `creep_idle`、walk 设 `creep`**(creep 是趴下爬行姿态);
- 这样不用派生断肢模型就有"爬行僵尸"效果;若要缺肢视觉再单独派生断肢模型。
- 注:infected/sporer 的 `creep_idle` 在原 mod 里引用了两根不存在的骨骼(`bone20/22`),实测若爬行姿态怪异,我再修这两条动画。
