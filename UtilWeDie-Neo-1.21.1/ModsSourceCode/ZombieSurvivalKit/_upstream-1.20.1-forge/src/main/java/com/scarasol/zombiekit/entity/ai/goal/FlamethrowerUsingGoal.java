package com.scarasol.zombiekit.entity.ai.goal;

import com.scarasol.zombiekit.init.ZombieKitItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class FlamethrowerUsingGoal<T extends Mob> extends Goal {
    private final T mob;
    private int seeTime;
    private int attackDelay;
    private int attackTime;
    private FlamethrowerUsingGoal.FlamethrowerState flamethrowerState = FlamethrowerUsingGoal.FlamethrowerState.UNLOCKED;


    public FlamethrowerUsingGoal(T mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.isValidTarget() && this.mob.getMainHandItem().is(ZombieKitItems.FLAMETHROWER.get());
    }

    @Override
    public boolean canContinueToUse() {
        return this.isValidTarget() && this.mob.getMainHandItem().is(ZombieKitItems.FLAMETHROWER.get());
    }

    private boolean isValidTarget() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void stop() {
        super.stop();
        if (this.flamethrowerState == FlamethrowerState.FIRE) {
            this.mob.stopUsingItem();
            this.flamethrowerState = FlamethrowerState.UNLOCKED;
            this.attackTime = 0;
            this.mob.getNavigation().stop();
        }
        this.mob.setAggressive(false);
        this.mob.setTarget(null);
        this.seeTime = 0;
    }

    public void tick() {
        LivingEntity livingEntity = this.mob.getTarget();
        if (livingEntity != null) {
            this.mob.setAggressive(true);
            boolean flag = this.mob.getSensing().hasLineOfSight(livingEntity);
            boolean flag1 = this.seeTime > 0;
            if (flag != flag1) {
                this.seeTime = 0;
            }

            if (flag) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }
            double distance = this.mob.distanceTo(livingEntity);
            boolean flag2 = this.seeTime < 5 && this.attackDelay == 0;
            this.mob.getLookControl().setLookAt(livingEntity, 30.0F, 30.0F);
            if (this.flamethrowerState == FlamethrowerState.UNLOCKED) {
                if (!flag2) {
                    this.flamethrowerState = FlamethrowerState.LOCKED;
                    this.attackDelay = 30 + this.mob.getRandom().nextInt(40);
                }
            }else if (this.flamethrowerState == FlamethrowerState.LOCKED) {

                --this.attackDelay;
                if (distance < 15) {
                    this.mob.getNavigation().stop();
                }else {
                    this.mob.getNavigation().moveTo(livingEntity, 1.0F);
                }
                if (this.attackDelay == 0) {
                    this.flamethrowerState = FlamethrowerUsingGoal.FlamethrowerState.READY_TO_ATTACK;
                }
            }else if (this.flamethrowerState == FlamethrowerUsingGoal.FlamethrowerState.READY_TO_ATTACK && flag) {
                this.attackTime = 100 + this.mob.getRandom().nextInt(60);
                this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                this.flamethrowerState = FlamethrowerState.FIRE;
            }else if (this.flamethrowerState == FlamethrowerState.FIRE) {
                if (flag && attackTime > 0) {
                    attackTime--;
                    if (distance < 15) {
                        this.mob.getNavigation().stop();
                    }else {
                        this.mob.getNavigation().moveTo(livingEntity, 0.5F);
                    }
                }else {
                    this.mob.stopUsingItem();
                    this.flamethrowerState = FlamethrowerState.UNLOCKED;
                }
            }
        }
    }

    enum FlamethrowerState {
        LOCKED,
        UNLOCKED,
        READY_TO_ATTACK,
        FIRE;
    }

}
