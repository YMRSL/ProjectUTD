package net.mcreator.survivalinstinct.procedures;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class AntibioticsPlayerFinishesUsingItemProcedure {
    public static void execute(Entity entity) {
        LivingEntity _entity;
        if (entity == null) {
            return;
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
            _entity.removeEffect(MobEffects.HUNGER);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.WEAKNESS);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.LEVITATION);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.WITHER);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.POISON);
        }
    }
}

