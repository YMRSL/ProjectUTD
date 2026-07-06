// ============================================================================
//  infected / sporer —— 集群（聚团 + 跟首领）
//  挂法：贴进 CNPC 脚本面板的【Tick / Update】事件框。
//  原理：每隔若干 tick，在附近找“同名的同类 NPC”，选其中坐标最靠左上(取一个稳定的)的当 leader，
//        其余成员把 leader 设成“攻击目标”——CNPC 自带 AI 会自动寻路靠拢，于是自然聚团/跟随。
//        当附近有玩家/真目标时，让位给正常索敌（不抢战斗）。
//  注意：同类怪必须【同名】（都叫 "infected" 或都叫 "sporer"），否则认不出彼此。
//  数值可调：CLUSTER_RADIUS 聚团半径、PERIOD 重算周期、SPREAD 容许的松散距离。
// ============================================================================

// === 事件：Tick / Update ===
var CLUSTER_RADIUS = 16;   // 多远内算“同群”（格）
var PERIOD         = 20;   // 每多少 tick 重算一次首领/聚团（20=1s，省性能）
var SPREAD         = 3;    // 离首领超过这个距离才往回靠（避免原地抖动）
var PLAYER_RADIUS  = 12;   // 这个范围内有玩家就交给正常战斗 AI，不执行集群

function tick(e) {
    var npc = e.npc;

    // ——节流：用 tempdata 存一个计数器，不到周期直接返回——
    var t = npc.getTempdata().get("clusterTick");
    if (t == null) t = 0;
    t = t + 1;
    if (t < PERIOD) { npc.getTempdata().put("clusterTick", t); return; }
    npc.getTempdata().put("clusterTick", 0);

    var world = npc.getWorld();
    var pos   = world.getIPos(npc.getX(), npc.getY(), npc.getZ());

    // 1) 附近有玩家 → 让正常索敌接管，集群不插手
    var player = world.getClosestEntity(pos, PLAYER_RADIUS, 1); // 1 = 玩家
    if (player != null) return;

    // 2) 找附近所有 NPC，筛出“同名同类”
    var myName = npc.getName();
    var nearby = world.getNearbyEntities(pos, CLUSTER_RADIUS, 2); // 2 = NPC
    if (nearby == null || nearby.length == 0) return;

    var kin = [];
    for (var i = 0; i < nearby.length; i++) {
        var en = nearby[i];
        if (en == null) continue;
        if (en.getName() != myName) continue;            // 只认同名
        if (en.getUUID() == npc.getUUID()) continue;     // 排除自己
        kin.push(en);
    }
    if (kin.length == 0) return; // 落单，无群可跟

    // 3) 选 leader：用 UUID 字典序最小的成员当首领（确定性、全群一致，无需通信）。
    //    把自己也算进候选，这样首领自己不会去跟别人。
    var leader = npc;
    var leaderId = npc.getUUID();
    for (var j = 0; j < kin.length; j++) {
        var id = kin[j].getUUID();
        if (id < leaderId) { leaderId = id; leader = kin[j]; }
    }

    // 4) 我是首领 → 不动（或可在此放游荡逻辑）；我不是首领 → 离远了就向首领靠拢
    if (leader.getUUID() == npc.getUUID()) return;

    var dx = leader.getX() - npc.getX();
    var dz = leader.getZ() - npc.getZ();
    var distFlat = Math.sqrt(dx * dx + dz * dz);
    if (distFlat > SPREAD) {
        // setAttackTarget 会驱动 CNPC 寻路走向 leader；leader 不是敌人，到附近就停（不会真打，
        // 因为同阵营/同名通常不互伤；若你的配置会互伤，改用下面的 setMoveForward 朝向法）。
        try { npc.setAttackTarget(leader); } catch (err) {}
    }
}
