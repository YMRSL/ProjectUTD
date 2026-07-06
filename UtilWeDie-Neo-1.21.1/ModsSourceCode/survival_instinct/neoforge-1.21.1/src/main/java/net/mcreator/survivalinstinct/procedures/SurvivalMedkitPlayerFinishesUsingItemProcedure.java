package net.mcreator.survivalinstinct.procedures;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class SurvivalMedkitPlayerFinishesUsingItemProcedure {
    public static void execute(Entity entity) {
        LivingEntity _entity;
        if (entity == null) {
            return;
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2000, 2, true, true));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2000, 2, true, false));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2000, 1, true, false));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2000, 1, true, false));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2000, 1, true, false));
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.POISON);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.WITHER);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.BLINDNESS);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.DARKNESS);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.LEVITATION);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.CONFUSION);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.WEAKNESS);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.HUNGER);
        }
    }
}

