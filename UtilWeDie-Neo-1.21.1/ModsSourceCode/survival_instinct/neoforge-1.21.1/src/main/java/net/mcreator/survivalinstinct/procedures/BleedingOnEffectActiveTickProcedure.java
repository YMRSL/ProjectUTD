package net.mcreator.survivalinstinct.procedures;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class BleedingOnEffectActiveTickProcedure {
    public static void execute(LevelAccessor world, Entity entity) {
        if (entity == null) {
            return;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity _entity = (LivingEntity)entity;
            _entity.removeEffect(MobEffects.REGENERATION);
        }
        entity.getPersistentData().putDouble("da\u00f1o", entity.getPersistentData().getDouble("da\u00f1o") + 1.0);
        if (entity.getPersistentData().getDouble("da\u00f1o") == 30.0) {
            entity.hurt(new DamageSource((Holder)world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 1.0f);
            entity.getPersistentData().putDouble("da\u00f1o", 0.0);
        }
    }
}

