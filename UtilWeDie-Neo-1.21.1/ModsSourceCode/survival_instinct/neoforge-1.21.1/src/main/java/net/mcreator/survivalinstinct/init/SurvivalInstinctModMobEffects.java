package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.potion.BleedingMobEffect;
import net.mcreator.survivalinstinct.potion.DashOnCooldownMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SurvivalInstinctModMobEffects {
    public static final DeferredRegister<MobEffect> REGISTRY = DeferredRegister.create(Registries.MOB_EFFECT, (String)"survival_instinct");
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING = REGISTRY.register("bleeding", () -> new BleedingMobEffect());
    public static final DeferredHolder<MobEffect, MobEffect> DASH_ON_COOLDOWN = REGISTRY.register("dash_on_cooldown", () -> new DashOnCooldownMobEffect());
}

