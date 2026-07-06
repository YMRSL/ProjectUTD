package net.mcreator.survivalinstinct.potion;

import net.mcreator.survivalinstinct.procedures.BleedingOnEffectActiveTickProcedure;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class BleedingMobEffect
extends MobEffect {
    public BleedingMobEffect() {
        super(MobEffectCategory.HARMFUL, -3407872);
    }

    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        BleedingOnEffectActiveTickProcedure.execute((LevelAccessor)entity.level(), (Entity)entity);
        return true;
    }

    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}

