package com.scarasol.sona.effect;

import net.minecraft.world.effect.MobEffectCategory;

/**
 * Physical effects in 1.20.1 overrode {@code getCurativeItems()} to return an empty list so milk/
 * "clear all effects" could not remove them. In 1.21 NeoForge that hook moved off {@code MobEffect}.
 * The equivalent control is the {@code PHYSICAL_EFFECT_REMOVE} config consulted by the LivingEntity
 * mixin (mixin agent). This class is retained as a marker base so KEEP logic / {@code instanceof}
 * checks keep working.
 */
public class PhysicalEffect extends MobEffectBase {
    public PhysicalEffect(MobEffectCategory mobEffectCategory, int integer) {
        super(mobEffectCategory, integer);
    }
}
