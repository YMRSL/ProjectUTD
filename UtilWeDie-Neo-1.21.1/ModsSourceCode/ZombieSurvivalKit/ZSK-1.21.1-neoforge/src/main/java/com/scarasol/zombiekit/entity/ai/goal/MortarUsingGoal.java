package com.scarasol.zombiekit.entity.ai.goal;

import com.scarasol.sona.util.SonaMath;
import com.scarasol.zombiekit.api.MortarLevel;
import com.scarasol.zombiekit.data.LaunchSchedule;
import com.scarasol.zombiekit.entity.ai.control.MortarLookControl;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class MortarUsingGoal<T extends MortarEntity> extends Goal {
    private final T mob;
    private int attackDelay;
    private MortarState mortarState = MortarState.WAITING;

    private final Queue<LaunchSchedule> toDoLaunchSchedules = new LinkedList<>();
    private LaunchSchedule currentSchedule;


    public MortarUsingGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }


    @Override
    public boolean canUse() {
        return this.mob.getRack() != null && this.mob.isVehicle() && !(this.mob.getPassengers().get(0) instanceof Player);
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getRack() != null && this.mob.isVehicle() && !(this.mob.getPassengers().get(0) instanceof Player);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        if (this.mob.level() instanceof MortarLevel mortarLevel) {
            mortarLevel.getMortarManager().subscribe(this, this.mob.level().getGameTime());
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (this.mob.level() instanceof MortarLevel mortarLevel) {
            mortarLevel.getMortarManager().unsubscribe(this, this.mob.level().getGameTime());
        }
    }

    @Override
    public void tick() {
        if (this.mortarState == MortarState.WAITING) {
            LaunchSchedule newSchedule = toDoLaunchSchedules.poll();
            if (newSchedule != null) {
                if (this.isValidSchedule(newSchedule)) {
                    newSchedule.acceptSchedule();
                    float angle = (float) SonaMath.parabolaAngleCalculate(this.mob, newSchedule.getCoordinate().getCenter(), MortarEntity.VELOCITY);
                    if (angle != -1) {
                        this.currentSchedule = newSchedule;
                        this.attackDelay = this.mob.getRandom().nextInt(100);
                        BlockPos pos = this.currentSchedule.getCoordinate();
                        double x = pos.getX() - this.mob.getX();
                        double z = pos.getZ() - this.mob.getZ();
                        float azimuth = (float)(Mth.atan2(z, x) * (double)(180F / (float)Math.PI)) - 90.0F;
                        ((MortarLookControl) this.mob.getLookControl()).setLookView(azimuth, (float) -Math.toDegrees(angle));
                        this.mortarState = MortarState.RELOAD;
                    }
                }
            }
        } else if (this.mortarState == MortarState.RELOAD && !this.mob.getLookControl().isLookingAtTarget()) {
            if (this.attackDelay-- <= 0) {
                if (this.mob.reload()) {
                    this.mob.setCurrentSchedule(this.currentSchedule);
                    this.mortarState = MortarState.READY_TO_ATTACK;
                }else {
                    this.currentSchedule.failSchedule();
                    this.currentSchedule = null;
                    this.mortarState = MortarState.WAITING;
                }

            }
        } else if (this.mortarState == MortarUsingGoal.MortarState.READY_TO_ATTACK) {
            if (this.mob.getShell() == null)
                this.mortarState = MortarState.WAITING;
        }
    }

    public void pushSchedule(LaunchSchedule launchSchedule) {
        if (isValidSchedule(launchSchedule))
            toDoLaunchSchedules.offer(launchSchedule);
    }

    public boolean isValidSchedule(LaunchSchedule launchSchedule) {
        return launchSchedule.getCoordinate().distSqr(this.mob.getOnPos()) < 220 * 220 && !launchSchedule.isTimeout(this.mob.level().getGameTime());
    }

    public void syncSchedule(List<LaunchSchedule> launchSchedules) {
        launchSchedules.stream().filter(this::isValidSchedule).forEach(this.toDoLaunchSchedules::offer);
    }

    public T getMob() {
        return mob;
    }

    enum MortarState {
        WAITING,
        RELOAD,
        READY_TO_ATTACK
    }

}
