package net.mcreator.survivalinstinct.procedures;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class GasMaskHelmetTickEventProcedure {
    public static void execute(Entity entity) {
        LivingEntity _entity;
        if (entity == null) {
            return;
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.POISON);
        }
        if (entity instanceof LivingEntity) {
            _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.WITHER);
        }
    }
}

