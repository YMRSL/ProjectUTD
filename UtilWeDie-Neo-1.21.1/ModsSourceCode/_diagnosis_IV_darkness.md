# 诊断报告：Immersive Vehicles（MTS）平地驾驶画面变黑

- 日期：2026-06-12
- 环境：NeoForge 1.21.1（`1.21.1-NeoForge_21.1.233`）
- 涉事 mod：Immersive Vehicles `1.21.1-24.0.0`（mts），车包 Kaminari Motor Work `KMW 2.6.1`（gvp），另装 OAMP / MTS Official Pack 29
- 渲染栈：sodium-neoforge 0.6.13 + iris 1.8.12 + lambdynamiclights 4.8.8 + chloride 1.7.8 + EnhancedVisuals 1.8.29 + GallantTirelessAltruist + **Better Darkness 1.0.0**
- 游戏目录：`D:\MC\ProjectUTD\UtilWeDie-Neo-1.21.1\.minecraft\versions\1.21.1-NeoForge_21.1.233\`

---

## 一、症状
玩家驾驶 IV 载具：
- **平地行驶时画面整体异常变黑**（载具与座舱仍在渲染，只是亮度被压到接近全黑——是"变暗"而非"消失/闪烁"）。
- **开上斜坡亮度恢复正常**。
- 徒步在同一平地上不黑——只有"坐进载具驾驶"时才触发。

这是"亮度曲线随相机采样到的光照值变化"的问题，**不是剔除/不可见问题**。

---

## 二、与原始假设的核对（实测修正）

用户给出的环境描述与实际安装状态有两处出入，已在 `mods\` 内核对：

1. **Create Sable Dynamic Lights 实际处于禁用**：文件名为 `create-dyn-light-2.3.1-sodium-sable.jar.disabled`。当前**唯一启用的动态光 mod 是 LambDynamicLights 4.8.8**（外加 chloride 自带的处理）。`sodiumdynamiclights` 也禁用。
2. 包列表已确认：mts 24.0.0 + gvp(KMW) 2.6.1 + oamp + mtsofficialpack 29（见 `logs\latest.log` L420-423）。

此外发现一个关键的、用户未列入的 mod：**Better Darkness 1.0.0**（`betterdarkness-1.21.1-neoforge-1.0.0.jar`），它正以 **darknessLevel = 80**（接近全黑，100 为纯黑）生效。

---

## 三、逐一论证候选成因

### 候选①（采纳·首要）：Better Darkness × IV 内饰光照 的交互 —— 高置信
**结论：这是最可能的主因。**

- Better Darkness 的作用是：**把最终渲染亮度按"采样到的光照等级低于满值的程度"整体压暗**。世界 gamerule `darknessLevel` 实测为 **80**（很激进），三个存档都是 80，包括当前在用的存档 `新的世界 (2)`（今日 19:50 写入）。
- IV 的载具/座舱**不走 Minecraft 常规 lightmap 光照**，而是用自己的一套 OpenGL 混合光束（config 里的 `blendedLights` / `brightLights`）+ 自定义灯光渲染 pass（jar 内 `assets/mts/shaders/core/mts_entity_lights`）。坐进座舱后，相机眼位被座舱模型件包住，采样到的方块光偏低。
- **平地**：眼位采样到的 sky+block 光值偏低 → 被 Better Darkness 压到接近全黑。
- **斜坡**：载具倾斜 / 眼位采样点平移到一个更亮的方块格，采样光值越过 Better Darkness 的压暗阈值 → 亮度"恢复"。
- 这恰好解释"平地黑 / 斜坡正常"且"只有开车时黑、徒步不黑"——因为必须 IV 座舱把相机采样光压低、再叠 Better Darkness 放大，才会到全黑。

**外部佐证**：lumien231/Hardcore-Darkness issue #48「Conflicts with Immersive Vehicle's headlights」——同一类"黑暗 mod"与 IV 灯光系统的已知冲突；社区原话："Immersive Vehicles uses a different lighting system... OpenGL to generate headlight beam models that light up your view, but they don't work well with Hardcore Darkness." Better Darkness 1.0.0 是该思路在 1.21.1 的重写版，机制同源。

### 候选②（部分采纳·次要）：IV 内饰光照本身在 Sodium/iris 管线下采样偏暗 —— 中置信
- 即便没有 Better Darkness，IV 在 sodium/iris 重写的核心着色器管线下，座舱内饰确实可能比原版偏暗。IV 为此提供了 `lightsTransp` 开关（官方/社区给出的"内饰变暗/亮起的贴图发黑"修复就是把它开成 true）。
- 列为次要：它能让内饰更亮，但单凭它通常不会把整屏压到"全黑"。作为候选①修完后若仍偏暗的第二档调整。

### 候选③（排除）：iris 光影 / EnhancedVisuals 后处理误判座舱视角
- **iris**：`config\iris.properties` 为 `enableShaders=true` 但 `shaderPack=`（**空**）。即没有加载任何光影包，iris 只是接管原版渲染，不存在光影自带的曝光/暗角 PostChain 在跑。**排除"光影后处理压黑"。**
- **EnhancedVisuals**：`config\enhancedvisuals-client.json` 里启用的效果全是**血量/受伤驱动**（damage、heartbeat、saturation 按血量、slender 按血量），`damage.hitEffectIntensity=0.0`，**没有任何"按移动/按载具/按平地"触发的全屏压暗或暗角**。`underwater`、`health` 已 `enabled:false`。**EnhancedVisuals 不是本症的触发源。**
- 但 EnhancedVisuals 与 GallantTirelessAltruist 确有 **PostChain mixin 冲突**（`logs\latest.log` L1398：`enhancedvisuals ... PostChainAccessor` vs `gallanttirelessaltruist PostChainMixin`，`getPasses` 被 Skipping）。这会影响受击/击杀的全屏后处理效果是否正常，**与本"平地变黑"无因果**，单独记录备查。

### 候选④（排除）：动态光 mod（LambDynamicLights）与 IV 灯光实体交互
- LambDynamicLights 4.8.8 `self=true`（第一人称玩家自发光）。它只会**增加**光照、不会压暗，不会造成"变黑"。`config\lambdynlights.toml` 正常。
- Sable / sodiumdynamiclights 均已禁用，不参与。
- **排除动态光为致黑主因。** （若候选①②都修完仍有零星异常，再单独临时禁 LambDynamicLights 验证其与 IV 灯光实体是否有边角交互。）

### 关于 `mts_entity_lights` 着色器告警（澄清，非故障）
`logs\latest.log` L2239-2243 有一串 `Shader mts:mts_entity_lights could not find sampler named Sampler1/Sampler2 / uniform Light0_Direction/Light1_Direction`。
已核对 jar 内实际着色器：`mts_entity_lights.json` 声明了 Sampler1/2、Light0/1_Direction，但配套的 `mts_entity_lights.fsh` 只用到 `Sampler0`+`ColorModulator`+fog，**根本没用这几个 uniform**。这是 MTS 在 JSON 里多声明、片元里不消费导致的**无害告警**，每个版本都有，**与本症无关**，不要据此去改/换 IV。

---

## 四、已落地的改动（仅改 config，全部可回滚）

> 改动前已在同目录留 `.bak` 备份。**mods\ 未做任何改动。**

### 改动 1（首要修复）：关闭 Better Darkness 的全局压暗
文件：`config\betterdarkness-client.toml`
| 键 | 原值 | 新值 |
|---|---|---|
| `defaultDarknessLevel` | `80` | `0` |
| `useGamerule` | `true` | `false` |

说明：原本 `useGamerule=true` 时，配置值被世界 gamerule（=80）覆盖。改成 `useGamerule=false` 后忽略 gamerule，强制用配置里的 `defaultDarknessLevel=0`，即**全局停用 Better Darkness 的压暗**作为验证。备份：`betterdarkness-client.toml.bak`。

### 备份（未改，仅预留）
`mtsconfigclient.json.bak` 已生成，供"改动 2"使用。

---

## 五、待用户验证的步骤

### 第 1 步：验证主因（先只测改动 1）
1. 启动游戏，进世界，开车在**平地**上行驶。
2. 看：是否还变黑？
   - **不再变黑** → 确诊主因是 Better Darkness×IV。进入"调优"：把 `defaultDarknessLevel` 从 0 调到你能接受的夜晚黑度（建议 30~50，别再用 80），保存重进确认平地不黑。完成。
   - **仍变黑（或仅减轻、内饰仍偏暗）** → 主因部分在 IV 侧，做第 2 步。

### 第 2 步：叠加 MTS 内饰光照修复（改动 2，手动或让我改）
编辑 `config\mtsconfigclient.json`，在 `renderingSettings` 段把下面两个键的 `value` 改为 true：
- `"lightsTransp"`：`false` → `true`（让会发光的贴图走透明 pass；这是 IV 在 sodium/光影式管线下"内饰/灯光贴图发黑"的官方向修复）
- 如仍偏暗，再把 `"brightLights"`：`true` 保持、并尝试 `"blendedLights"`：`true` → `false`（关掉对世界做 OpenGL 提亮混合，避免与压暗类后处理打架）

改完**重启游戏**（着色器/渲染开关需重载），再开车看平地。

> 注：游戏内也可直接按 **P** 打开 IV 配置 GUI 调 `lightsTransp` 等，免重启可即时观察。

### 第 3 步（仅在 1、2 都无效时）：排除动态光
临时把 `mods\lambdynamiclights-4.8.8+1.21.1.jar` 改名加 `.disabled`（属于 mods\ 操作，需你本人执行），进游戏开车验证。若变黑消失则为 IV×动态光边角交互；否则改回。

---

## 六、回滚方法

- **回滚改动 1**：把 `config\betterdarkness-client.toml.bak` 覆盖回 `betterdarkness-client.toml`（或手动改回 `defaultDarknessLevel=80`、`useGamerule=true`）。
- **回滚改动 2（若做了）**：把 `config\mtsconfigclient.json.bak` 覆盖回 `mtsconfigclient.json`。
- 两个 `.bak` 均在各自 config 同目录下。
- 第 3 步的 `.disabled` 改名还原即可。

---

## 附：关键证据位置
- IV 渲染开关：`config\mtsconfigclient.json` → `renderingSettings`（`brightLights`/`blendedLights`/`lightsTransp`/`renderingMode`/`renderFlares`/`renderBeams`）
- Better Darkness：`config\betterdarkness-client.toml`；世界 gamerule `darknessLevel=80`（`saves\*\level.dat`，三存档均 80）
- iris 无光影包：`config\iris.properties`（`enableShaders=true`，`shaderPack=` 空）
- EnhancedVisuals 仅血量/受伤效果：`config\enhancedvisuals-client.json`
- PostChain mixin 冲突（无关本症）：`logs\latest.log` L1398
- IV 灯光着色器无害告警：`logs\latest.log` L2239-2243；jar 内 `assets/mts/shaders/core/mts_entity_lights.{json,fsh,vsh}`
- 动态光实况：`mods\create-dyn-light-2.3.1-sodium-sable.jar.disabled`（Sable 已禁）、`config\lambdynlights.toml`、`config\sodiumdynamiclights-client.toml`
