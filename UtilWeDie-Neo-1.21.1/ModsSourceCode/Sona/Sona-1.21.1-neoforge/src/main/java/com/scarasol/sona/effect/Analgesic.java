package com.scarasol.sona.effect;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class Analgesic extends MobEffectBase {

    public Analgesic() {
        super(MobEffectCategory.BENEFICIAL, -13369549);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.DIG_SLOWDOWN);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

}
