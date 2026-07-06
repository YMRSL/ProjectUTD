// ============================================================================
//  lurker（小蜘蛛/跳扑） —— 接近玩家时起跳扑过去
//  两段：
//    1) tick(e)   → 贴进【Tick / Update】框：玩家进入扑击距离带时，放 start_jump 并给一个朝玩家的冲量。
//    2) target(e) → 贴进【Target】框（可选）：刚锁定目标时也起一次跳，更有“突袭”感。
//  可用动画（lurker）：idle walk jumping start_jump start_riding riding death1 death2。
//  说明：start_jump 是“起跳前摇/扑出”动画；jumping 是滞空。这里用 start_jump 作扑击演出，
//        同时用 setVelocity/addVelocity 给真实位移（演出+实际扑过去）。
//  骑乘(riding)：lurker 设计上可“扑上去骑乘玩家”。脚本里做骑乘需 entity.mountEntity，
//        CNPC API 对“骑到玩家头上”支持有限，这里默认只做扑击位移；想要骑乘见文末可选段。
// ============================================================================

// ============ 事件：Tick / Update ============
var POUNCE_MIN   = 3.0;   // 进入扑击距离带的近端（太近不扑，直接咬）
var POUNCE_MAX   = 7.0;   // 远端（太远不扑，先走近）
var POUNCE_CD    = 45;    // 扑击冷却(tick)，~2.25s
var POUNCE_UP    = 0.45;  // 起跳竖直冲量
var POUNCE_FWD   = 0.9;   // 朝目标水平冲量强度

function tick(e) {
    var npc = e.npc;

    // 冷却递减
    var cd = npc.getTempdata().get("pounceCd");
    if (cd == null) cd = 0;
    if (cd > 0) { npc.getTempdata().put("pounceCd", cd - 1); return; }

    // 找最近玩家（也可改成 npc.getAttackTarget() 只扑当前目标）
    var world = npc.getWorld();
    var pos   = world.getIPos(npc.getX(), npc.getY(), npc.getZ());
    var player = world.getClosestEntity(pos, POUNCE_MAX, 1); // 1 = 玩家
    if (player == null) return;

    var dx = player.getX() - npc.getX();
    var dy = player.getY() - npc.getY();
    var dz = player.getZ() - npc.getZ();
    var distFlat = Math.sqrt(dx*dx + dz*dz);

    // 只在“中距离带”里扑：太近(<MIN)留给普通近战，太远(>MAX)先靠近
    if (distFlat < POUNCE_MIN || distFlat > POUNCE_MAX) return;

    // 1) 演出：起跳动画
    npc.playGeckoAnim("start_jump");

    // 2) 实际位移：朝玩家给一个抛物冲量（扑过去）
    var len = (distFlat == 0) ? 1 : distFlat;
    var vx = (dx / len) * POUNCE_FWD;
    var vz = (dz / len) * POUNCE_FWD;
    var mc = npc.getMCEntity();
    try {
        mc.setDeltaMovement(vx, POUNCE_UP, vz);  // NeoForge LivingEntity
        mc.hasImpulse = true;
        mc.hurtMarked = true;                    // 强制同步速度到客户端
    } catch (err) {
        // 退路：没有 setDeltaMovement 时用 CNPC 的前进量近似
        try { npc.setMoveForward(1.0); } catch (e2) {}
    }

    npc.getTempdata().put("pounceCd", POUNCE_CD);
}

// ============ 事件：Target（刚选定目标，可选）============
// 锁定瞬间也来一次突袭起跳，制造“从暗处扑出”的第一下。
function target(e) {
    var npc = e.npc;
    var cd = npc.getTempdata().get("pounceCd");
    if (cd != null && cd > 0) return;
    npc.playGeckoAnim("start_jump");
    npc.getTempdata().put("pounceCd", 25);
}

// ---------------------------------------------------------------------------
// 可选：扑上去“骑乘”玩家（手感更恶心，按需启用，写进上面的 tick 扑中后）
//   var mc = npc.getMCEntity();
//   var pmc = player.getMCEntity();
//   try { mc.startRiding(pmc, true); npc.playGeckoAnim("start_riding"); } catch(e){}
//   注意：骑到玩家身上原版不一定允许（玩家不是可骑乘载具），多数情况需要额外 mixin 支持；
//   若 start_riding 不生效，就保留纯扑击位移即可。
// ---------------------------------------------------------------------------
