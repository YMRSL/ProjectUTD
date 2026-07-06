package net.mcreator.survivalinstinct.procedures;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class MedkitBagPlayerFinishesUsingItemProcedure {
    public static void execute(Entity entity) {
        LivingEntity _entity;
        if (entity == null) {
            return;
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1, true, false));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1, true, false));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0, true, false));
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(SurvivalInstinctModMobEffects.BLEEDING);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.CONFUSION);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.POISON);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.HUNGER);
        }
    }
}

