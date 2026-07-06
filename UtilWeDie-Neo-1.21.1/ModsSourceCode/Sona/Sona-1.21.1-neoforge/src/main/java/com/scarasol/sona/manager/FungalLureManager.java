package com.scarasol.sona.manager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 枪声引怪(尸潮友好版)。
 *
 * <p>玩家开枪 → 在枪声点把附近敌对生物 / Fungal 感染者的「无目标时寻路终点」临时改为枪声点,
 * 维持 {@link #LURE_DURATION} tick。<b>默认索敌永远优先</b>(原版怪靠 {@code SonaLureGoal} 的
 * {@code hasRealTarget} 让位;Fungal 靠脚本的索敌优先),无需额外打断逻辑。
 *
 * <p><b>性能模型</b>:纯推送——开枪时<em>扫一次</em>范围内的怪、给每只写一个寻路终点(NBT),
 * 怪本身<b>不主动扫描</b>。每玩家 {@link #COOLDOWN_TICKS} 冷却;范围内 &gt; {@link #MAX_MOBS} 只
 * 直接放弃(尸潮零开销)。被影响数 &gt; {@link #STAGGER_THRESHOLD} 时,较近的先收到、较远的按每
 * {@link #STAGGER_GROUP} 只一组、每组 {@link #STAGGER_DELAY} tick 延迟收到({@link #tick()} 排空),
 * 把初始寻路尖峰摊平。
 *
 * <p><b>===== 给 CNPC 脚本/其它 NPC 复用的接收 API =====</b>
 * <p>谁会被广播:① 原版敌对生物({@link Enemy},无需 tag);② <b>任何带 scoreboard tag
 * {@code "sona_lure_listener"} 的实体</b>(CNPC NPC 这样接入)。
 * <p>接入步骤(CNPC 脚本):
 * <pre>
 * // 1) 开局自打 tag,让广播找得到它:
 * if(!sd.has('lureTag')){npc.getMCEntity().addTag('sona_lure_listener');sd.put('lureTag',1);}
 * // 2) 无攻击目标时,读 persistentData 里被写入的枪声点并寻路过去:
 * var pd=npc.getMCEntity().getPersistentData();
 * if(pd.contains('SonaLureUntil') &amp;&amp; npc.getWorld().getTotalTime() &lt; pd.getLong('SonaLureUntil')){
 *   npc.navigateTo(pd.getDouble('SonaLureX'), pd.getDouble('SonaLureY'), pd.getDouble('SonaLureZ'), 1.0);
 * }
 * </pre>
 * <p>写入的 NBT:{@code SonaLureX/Y/Z}(double,枪声点坐标)、{@code SonaLureUntil}(long,失效的
 * gameTime)。约定:接收方应让自身正常索敌优先,仅在「无真实目标」时才走向枪声点。
 */
public final class FungalLureManager {

    private static final long COOLDOWN_TICKS = 120L;     // 6s 每玩家冷却
    private static final double LURE_RADIUS = 64.0D;     // 影响半径(格)
    private static final int MAX_MOBS = 48;              // 范围内超过此数 -> 放弃(尸潮)
    private static final long LURE_DURATION = 200L;      // 硬控 10s

    private static final int STAGGER_THRESHOLD = 20;     // 被影响数超过此值才错峰
    private static final int STAGGER_GROUP = 5;          // 错峰每组只数
    private static final long STAGGER_DELAY = 10L;       // 每组延迟(0.5s)

    /** 接收广播的通用 tag(原版敌对生物无需此 tag;其它 NPC 自打此 tag 即可接入)。 */
    private static final String LISTENER_TAG = "sona_lure_listener";

    private static final Map<UUID, Long> LAST_SHOT = new HashMap<>();

    /** 延迟广播队列(错峰用)。 */
    private record Pending(ServerLevel level, Mob mob, double x, double y, double z, long applyTick, long until) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private FungalLureManager() {
    }

    public static void onGunShot(ServerLevel level, UUID shooterId, double x, double y, double z) {
        long now = level.getGameTime();
        Long last = LAST_SHOT.get(shooterId);
        if (last != null && (now - last) < COOLDOWN_TICKS) {
            return; // 冷却中
        }
        LAST_SHOT.put(shooterId, now);
        if (LAST_SHOT.size() > 256) {
            LAST_SHOT.entrySet().removeIf(e -> (now - e.getValue()) > COOLDOWN_TICKS * 4);
        }

        AABB box = new AABB(x - LURE_RADIUS, y - LURE_RADIUS, z - LURE_RADIUS,
                x + LURE_RADIUS, y + LURE_RADIUS, z + LURE_RADIUS);
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, box, FungalLureManager::isLureTarget);
        if (mobs.size() > MAX_MOBS) {
            return; // 尸潮:放弃广播(冷却已消耗)
        }
        if (mobs.size() <= STAGGER_THRESHOLD) {
            long until = now + LURE_DURATION;
            for (Mob mob : mobs) {
                apply(mob, x, y, z, until);
            }
            return;
        }
        // 错峰:按距枪声点由近到远排序,前 STAGGER_THRESHOLD 只立刻,其余每 STAGGER_GROUP 只一组、每组延迟 STAGGER_DELAY
        mobs.sort(Comparator.comparingDouble(m -> m.distanceToSqr(x, y, z)));
        for (int i = 0; i < mobs.size(); i++) {
            Mob mob = mobs.get(i);
            if (i < STAGGER_THRESHOLD) {
                apply(mob, x, y, z, now + LURE_DURATION);
            } else {
                int group = (i - STAGGER_THRESHOLD) / STAGGER_GROUP + 1;
                long delay = group * STAGGER_DELAY;
                PENDING.add(new Pending(level, mob, x, y, z, now + delay, now + delay + LURE_DURATION));
            }
        }
    }

    /** 每服务器 tick 调用一次,排空到期的延迟广播。 */
    public static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.level.getGameTime() >= p.applyTick) {
                if (p.mob.isAlive()) {
                    apply(p.mob, p.x, p.y, p.z, p.until);
                }
                it.remove();
            }
        }
    }

    private static void apply(Mob mob, double x, double y, double z, long until) {
        CompoundTag pd = mob.getPersistentData();
        pd.putDouble("SonaLureX", x);
        pd.putDouble("SonaLureY", y);
        pd.putDouble("SonaLureZ", z);
        pd.putLong("SonaLureUntil", until);
    }

    private static boolean isLureTarget(Mob mob) {
        return mob instanceof Enemy || mob.getTags().contains(LISTENER_TAG);
    }
}
