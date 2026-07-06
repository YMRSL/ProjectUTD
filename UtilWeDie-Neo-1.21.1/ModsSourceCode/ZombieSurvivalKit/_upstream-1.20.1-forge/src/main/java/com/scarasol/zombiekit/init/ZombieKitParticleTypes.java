package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ZombieKitParticleTypes {
    public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(Registries.PARTICLE_TYPE, ZombieKitMod.MODID);

    public static final RegistryObject<ParticleType<SimpleParticleType>> LARGE_FLAME = REGISTRY.register("large_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<ParticleType<SimpleParticleType>> LARGE_NAPALM_FLAME = REGISTRY.register("large_napalm_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<ParticleType<SimpleParticleType>> LARGE_SOUL_FLAME = REGISTRY.register("large_soul_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<ParticleType<SimpleParticleType>> LARGE_SMOKE = REGISTRY.register("large_smoke", () -> new SimpleParticleType(false));



}
