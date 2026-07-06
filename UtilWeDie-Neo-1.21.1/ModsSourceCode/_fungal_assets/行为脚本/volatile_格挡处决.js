// ============================================================================
//  volatile（精英） —— 格挡 / 处决
//  三段，分别贴进三个事件框：
//    1) damaged(e) → 贴进【Damaged】框：被攻击时概率举盾 guard / parry。
//    2) tick(e)    → 贴进【Tick / Update】框：低血时举盾防守 + 抓机会蓄处决。
//    3) killed(e)  → 贴进【Killed Entity】框：击杀目标时放处决动画 execution。
//  可用动画（volatile）：guard guard_walk parry start_dodge end_dodge stagger
//                       execution execution_ground …（见配置卡）。
//  注：playGeckoAnim 只播“表演动画”，不改 CNPC 实际伤害/无敌帧；这里是手感/演出层。
//      若要“格挡真减伤”，在 damaged 框里把 e.damage 调小（见下，已给可选行）。
// ============================================================================

// ============ 事件：Damaged（受击）============
var PARRY_CHANCE   = 0.35;  // 被打时举盾/招架的概率
var PARRY_COOLDOWN = 25;    // 招架动画冷却(tick)，防止每帧狂闪

function damaged(e) {
    var npc = e.npc;

    // 冷却判定（tempdata 存上次招架的剩余冷却）
    var cd = npc.getTempdata().get("parryCd");
    if (cd == null) cd = 0;
    if (cd > 0) return;

    if (Math.random() < PARRY_CHANCE) {
        // 招架：近距离来袭用 parry，否则 guard
        var anim = (e.source != null) ? "parry" : "guard";
        npc.playGeckoAnim(anim);
        npc.getTempdata().put("parryCd", PARRY_COOLDOWN);

        // —可选：让这次招架真减伤（砍一半）。不想要就删掉下一行—
        // e.damage = e.damage * 0.4;
    }
}

// ============ 事件：Tick / Update ============
var LOW_HP_RATIO   = 0.35;  // 血量低于此比例进入“防守姿态”
var GUARD_PERIOD   = 40;    // 防守姿态下每多少 tick 重举一次盾
var EXEC_RANGE     = 2.5;   // 目标在这么近、且自己满状态时，抓机会放处决

function tick(e) {
    var npc = e.npc;

    // 递减招架冷却
    var cd = npc.getTempdata().get("parryCd");
    if (cd != null && cd > 0) npc.getTempdata().put("parryCd", cd - 1);

    // —— 低血防守姿态：周期性举盾 ——
    var hp  = npc.getHealth();
    var max = 1;
    try { max = npc.getMaxHealth(); } catch (err) { max = 20; }
    if (max <= 0) max = 20;

    var gt = npc.getTempdata().get("guardTick");
    if (gt == null) gt = 0;
    gt = gt + 1;

    if (hp / max <= LOW_HP_RATIO) {
        if (gt >= GUARD_PERIOD) {
            npc.playGeckoAnim("guard");
            gt = 0;
        }
    }
    npc.getTempdata().put("guardTick", gt);

    // —— 抓机会处决：有攻击目标且贴脸时，偶尔放 execution（演出，伤害仍走正常攻击）——
    var tgt = null;
    try { tgt = npc.getAttackTarget(); } catch (err2) {}
    if (tgt != null) {
        var dx = tgt.getX() - npc.getX();
        var dy = tgt.getY() - npc.getY();
        var dz = tgt.getZ() - npc.getZ();
        var d  = Math.sqrt(dx*dx + dy*dy + dz*dz);
        var ecd = npc.getTempdata().get("execCd");
        if (ecd == null) ecd = 0;
        if (ecd > 0) { npc.getTempdata().put("execCd", ecd - 1); }
        else if (d <= EXEC_RANGE && Math.random() < 0.04) { // 约每秒~0.8次机会判定
            npc.playGeckoAnim("execution");
            npc.getTempdata().put("execCd", 60); // 处决后冷却 3s
        }
    }
}

// ============ 事件：Killed Entity（击杀目标）============
function killed(e) {
    // 真的杀掉了一个目标 → 放一记处决/收尾动画，强化“处决”观感
    e.npc.playGeckoAnim("execution");
}
