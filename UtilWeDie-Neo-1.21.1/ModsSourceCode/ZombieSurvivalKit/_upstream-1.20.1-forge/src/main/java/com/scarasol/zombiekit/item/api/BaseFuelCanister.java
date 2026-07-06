package com.scarasol.zombiekit.item.api;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public interface BaseFuelCanister {
    void canisterEffect(LivingEntity target);
    @NotNull
    ParticleType<?> getParticleType();
    @NotNull
    String getTexture();
}
