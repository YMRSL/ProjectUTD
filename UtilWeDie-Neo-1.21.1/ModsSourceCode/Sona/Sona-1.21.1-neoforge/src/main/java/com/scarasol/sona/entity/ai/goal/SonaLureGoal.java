package com.scarasol.sona.entity.ai.goal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * 让原版敌对生物在「无真实目标」时,寻路去 {@code FungalLureManager} 广播写入 persistentData 的枪声点。
 *
 * <p>纯读 NBT 坐标,<b>不扫描实体</b>,空闲开销=一次 NBT 取值。真实索敌永远优先:
 * {@link #canUse()} 检查 {@link #hasRealTarget()} → 有目标即让位,所以无需额外打断逻辑。
 */
public class SonaLureGoal extends Goal {

    private static final int REPATH_INTERVAL = 10;
    private static final double CLOSE_ENOUGH_SQR = 4.0D;

    private final Mob mob;
    private final double speed;
    private int nextRepath;
    private double tx, ty, tz;

    public SonaLureGoal(Mob mob, double speed) {
        this.mob = mob;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean hasLure() {
        CompoundTag pd = mob.getPersistentData();
        return pd.contains("SonaLureUntil") && mob.level().getGameTime() < pd.getLong("SonaLureUntil");
    }

    private boolean hasRealTarget() {
        LivingEntity t = mob.getTarget();
        return t != null && t.isAlive();
    }

    private void readPoint() {
        CompoundTag pd = mob.getPersistentData();
        tx = pd.getDouble("SonaLureX");
        ty = pd.getDouble("SonaLureY");
        tz = pd.getDouble("SonaLureZ");
    }

    @Override
    public boolean canUse() {
        if (hasRealTarget() || !hasLure()) {
            return false;
        }
        readPoint();
        return mob.distanceToSqr(tx, ty, tz) > CLOSE_ENOUGH_SQR;
    }

    @Override
    public boolean canContinueToUse() {
        return !hasRealTarget() && hasLure() && mob.distanceToSqr(tx, ty, tz) > CLOSE_ENOUGH_SQR;
    }

    @Override
    public void start() {
        nextRepath = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        readPoint();
        mob.getLookControl().setLookAt(tx, ty, tz);
        if (nextRepath > 0) {
            nextRepath--;
            return;
        }
        nextRepath = REPATH_INTERVAL;
        mob.getNavigation().moveTo(tx, ty, tz, speed);
    }
}
