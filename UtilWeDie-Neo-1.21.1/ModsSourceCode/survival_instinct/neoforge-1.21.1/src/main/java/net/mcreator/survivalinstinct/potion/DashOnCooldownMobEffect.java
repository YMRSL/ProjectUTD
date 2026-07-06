package net.mcreator.survivalinstinct.potion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class DashOnCooldownMobEffect
extends MobEffect {
    public DashOnCooldownMobEffect() {
        super(MobEffectCategory.NEUTRAL, -1781788);
    }

    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}

