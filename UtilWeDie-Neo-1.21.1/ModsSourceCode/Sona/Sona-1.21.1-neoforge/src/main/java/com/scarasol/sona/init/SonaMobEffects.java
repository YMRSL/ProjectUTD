package com.scarasol.sona.init;

import com.scarasol.sona.SonaMod;
import com.scarasol.sona.effect.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SonaMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, SonaMod.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> ANALGESIC = REGISTRY.register("analgesic", Analgesic::new);
    public static final DeferredHolder<MobEffect, MobEffect> CAMOUFLAGE = REGISTRY.register("camouflage", () -> new PhysicalEffect(MobEffectCategory.BENEFICIAL, -13408768));
    public static final DeferredHolder<MobEffect, MobEffect> CONFUSION = REGISTRY.register("confusion", Confusion::new);
    public static final DeferredHolder<MobEffect, MobEffect> CORROSION = REGISTRY.register("corrosion", Corrosion::new);
    public static final DeferredHolder<MobEffect, MobEffect> EXPOSURE = REGISTRY.register("exposure", () -> new PhysicalEffect(MobEffectCategory.HARMFUL, -3407872));
    public static final DeferredHolder<MobEffect, MobEffect> FRAGILITY = REGISTRY.register("fragility", Fragility::new);
    public static final DeferredHolder<MobEffect, MobEffect> FROST = REGISTRY.register("frost", Frost::new);
    public static final DeferredHolder<MobEffect, MobEffect> IGNITION = REGISTRY.register("ignition", Ignition::new);
    public static final DeferredHolder<MobEffect, MobEffect> IMMUNITY = REGISTRY.register("immunity", Immunity::new);
    public static final DeferredHolder<MobEffect, MobEffect> INFECTION = REGISTRY.register("infection", Infection::new);
    public static final DeferredHolder<MobEffect, MobEffect> INSANE = REGISTRY.register("insane", Insane::new);
    public static final DeferredHolder<MobEffect, MobEffect> LACERATION = REGISTRY.register("laceration", () -> new PhysicalEffect(MobEffectCategory.HARMFUL, 16758465));
    public static final DeferredHolder<MobEffect, MobEffect> MAIM = REGISTRY.register("maim", () -> new PhysicalEffect(MobEffectCategory.HARMFUL, 7997962));
    public static final DeferredHolder<MobEffect, MobEffect> NEUTRALITY = REGISTRY.register("neutrality", () -> new MobEffectBase(MobEffectCategory.NEUTRAL, 0x7F7F7F));
    public static final DeferredHolder<MobEffect, MobEffect> SLIMINESS = REGISTRY.register("sliminess", Sliminess::new);
    public static final DeferredHolder<MobEffect, MobEffect> STUN = REGISTRY.register("stun", Stun::new);

}
