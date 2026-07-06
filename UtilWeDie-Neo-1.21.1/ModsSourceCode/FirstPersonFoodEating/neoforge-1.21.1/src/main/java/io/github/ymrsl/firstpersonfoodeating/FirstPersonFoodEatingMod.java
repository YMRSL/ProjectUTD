package io.github.ymrsl.firstpersonfoodeating;

import io.github.ymrsl.firstpersonfoodeating.config.ModCommonConfig;
import io.github.ymrsl.firstpersonfoodeating.diagnostic.BootTrace;
import io.github.ymrsl.firstpersonfoodeating.registry.ModItems;
import io.github.ymrsl.firstpersonfoodeating.registry.ModMobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(FirstPersonFoodEatingMod.MOD_ID)
public final class FirstPersonFoodEatingMod {
    public static final String MOD_ID = "firstpersonfoodeating";

    public FirstPersonFoodEatingMod(IEventBus modEventBus, ModContainer modContainer) {
        BootTrace.init("mod constructor entered");
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
        ModItems.register(modEventBus);
        ModMobEffects.register(modEventBus);
        BootTrace.event("mod.constructor", "common config + registries registered");
    }
}
