package com.scarasol.sona.effect;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Insane extends MobEffectBase {

    private final Map<Mob, NearestAttackableTargetGoal<LivingEntity>> crazyAttackGoals = Maps.newHashMap();

    public Insane() {
        super(MobEffectCategory.HARMFUL, -6750208);
        addAttributeModifier(Attributes.ARMOR, ResourceLocation.fromNamespaceAndPath("sona", "insane_armor"), -0.8, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath("sona", "insane_movement_speed"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath("sona", "insane_attack_damage"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        if (entity instanceof Mob mob && !crazyAttackGoals.containsKey(mob)) {
            for (WrappedGoal goal : mob.targetSelector.getAvailableGoals()) {
                if (goal.getGoal() instanceof TargetGoal) {
                    goal.stop();
                }
            }
            NearestAttackableTargetGoal<LivingEntity> crazyAttackGoal = new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 5, false, false, livingEntity -> !livingEntity.equals(mob));
            mob.targetSelector.addGoal(1, crazyAttackGoal);
            mob.setTarget(null);
            crazyAttackGoals.put(mob, crazyAttackGoal);
        }
        return true;
    }

    @Override
    public void onMobRemoved(@NotNull LivingEntity entity, int amplifier, @NotNull Entity.RemovalReason reason) {
        super.onMobRemoved(entity, amplifier, reason);
        if (entity instanceof Mob mob) {
            mob.targetSelector.removeGoal(crazyAttackGoals.get(mob));
            crazyAttackGoals.remove(mob);
        }
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }
}
