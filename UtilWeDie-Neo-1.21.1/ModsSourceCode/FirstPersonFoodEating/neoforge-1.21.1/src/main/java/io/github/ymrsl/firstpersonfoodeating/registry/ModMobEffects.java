package io.github.ymrsl.firstpersonfoodeating.registry;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    private static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, FirstPersonFoodEatingMod.MOD_ID);

    public static final Holder<MobEffect> HEAL_GRADUAL =
            register("heal_gradual", MobEffectCategory.BENEFICIAL, 0xE36D6D);
    public static final Holder<MobEffect> BANDAGE =
            register("bandage", MobEffectCategory.BENEFICIAL, 0xC9A77A);
    public static final Holder<MobEffect> IMMUNE =
            register("immune", MobEffectCategory.BENEFICIAL, 0x77D2C6);
    public static final Holder<MobEffect> EMERGENCY_PAINKILLER =
            register("emergency_painkiller", MobEffectCategory.BENEFICIAL, 0xD98EFF);
    public static final Holder<MobEffect> HEALTHY =
            register("healthy", MobEffectCategory.BENEFICIAL, 0x7FD66A);

    private ModMobEffects() {
    }

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    private static Holder<MobEffect> register(String path, MobEffectCategory category, int color) {
        return MOB_EFFECTS.register(path, () -> new MobEffect(category, color) {
        });
    }
}
