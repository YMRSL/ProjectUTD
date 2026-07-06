package io.github.ymrsl.firstpersonfoodeating.item;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.registry.ModMobEffects;
import java.util.ArrayList;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = FirstPersonFoodEatingMod.MOD_ID)
public final class CustomStatusEffectController {
    private static final String ROOT_KEY = "firstpersonfoodeating_custom_status";
    private static final String HEAL_GRADUAL_COUNTER_KEY = "heal_gradual_counter";
    private static final String BANDAGE_COUNTER_KEY = "bandage_counter";
    private static final String PAINKILLER_COUNTER_KEY = "painkiller_counter";
    private static final String PAINKILLER_POOL_KEY = "painkiller_pool";

    private CustomStatusEffectController() {
    }

    public static void applyEmergencyPainkiller(Player player, float amount) {
        if (player == null || player.level().isClientSide() || amount <= 0.0f) {
            return;
        }
        float currentPool = getPainkillerPool(player);
        float targetPool = Math.max(currentPool, amount);
        float delta = targetPool - currentPool;
        if (delta > 0.0f) {
            player.setAbsorptionAmount(player.getAbsorptionAmount() + delta);
        }
        setPainkillerPool(player, targetPool);
        clearCounter(player, PAINKILLER_COUNTER_KEY);
        int amplifier = Mth.clamp(Math.max(Math.round(targetPool) - 1, 0), 0, 254);
        Holder<MobEffect> effect = ModMobEffects.EMERGENCY_PAINKILLER;
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() >= amplifier && current.getDuration() <= 0) {
            return;
        }
        MobEffectInstance infinite = new MobEffectInstance(
                effect,
                MobEffectInstance.INFINITE_DURATION,
                amplifier,
                false,
                true,
                true
        );
        player.addEffect(infinite, player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide()) {
            return;
        }

        boolean healthyActive = hasEffect(player, ModMobEffects.HEALTHY);
        if (healthyActive) {
            freezePositiveEffects(player);
        }
        if (hasEffect(player, ModMobEffects.IMMUNE)) {
            clearHarmfulEffects(player);
        }

        tickHealGradual(player);
        tickBandage(player, healthyActive);
        tickEmergencyPainkiller(player, healthyActive);
    }

    @SubscribeEvent
    public static void onMobEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!hasEffect(player, ModMobEffects.IMMUNE)) {
            return;
        }
        MobEffectInstance incoming = event.getEffectInstance();
        if (incoming == null || incoming.getEffect() == null) {
            return;
        }
        if (incoming.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!hasEffect(player, ModMobEffects.BANDAGE)) {
            return;
        }
        if (hasEffect(player, ModMobEffects.HEALTHY)) {
            return;
        }
        player.removeEffect(ModMobEffects.BANDAGE);
        clearCounter(player, BANDAGE_COUNTER_KEY);
    }

    private static void tickHealGradual(Player player) {
        if (!hasEffect(player, ModMobEffects.HEAL_GRADUAL)) {
            clearCounter(player, HEAL_GRADUAL_COUNTER_KEY);
            return;
        }
        if (shouldTriggerCounter(player, HEAL_GRADUAL_COUNTER_KEY, 10) && player.getHealth() < player.getMaxHealth()) {
            player.heal(1.0f);
        }
    }

    private static void tickBandage(Player player, boolean healthyActive) {
        MobEffectInstance bandage = player.getEffect(ModMobEffects.BANDAGE);
        if (bandage == null) {
            clearCounter(player, BANDAGE_COUNTER_KEY);
            return;
        }
        if (player.getHealth() >= player.getMaxHealth()) {
            player.removeEffect(ModMobEffects.BANDAGE);
            clearCounter(player, BANDAGE_COUNTER_KEY);
            return;
        }
        int intervalTicks = Mth.clamp(bandage.getAmplifier() + 1, 1, 20 * 60);
        if (!healthyActive && shouldTriggerCounter(player, BANDAGE_COUNTER_KEY, intervalTicks)) {
            player.heal(1.0f);
        }
    }

    private static void tickEmergencyPainkiller(Player player, boolean healthyActive) {
        MobEffectInstance painkiller = player.getEffect(ModMobEffects.EMERGENCY_PAINKILLER);
        if (painkiller == null) {
            clearCounter(player, PAINKILLER_COUNTER_KEY);
            clearPainkillerPool(player);
            return;
        }
        float pool = getPainkillerPool(player);
        if (pool <= 0.0f) {
            player.removeEffect(ModMobEffects.EMERGENCY_PAINKILLER);
            clearCounter(player, PAINKILLER_COUNTER_KEY);
            clearPainkillerPool(player);
            return;
        }
        float absorptionNow = Math.max(player.getAbsorptionAmount(), 0.0f);
        if (pool > absorptionNow) {
            pool = absorptionNow;
            setPainkillerPool(player, pool);
        }
        if (pool <= 0.0f) {
            player.removeEffect(ModMobEffects.EMERGENCY_PAINKILLER);
            clearCounter(player, PAINKILLER_COUNTER_KEY);
            clearPainkillerPool(player);
            return;
        }
        if (healthyActive) {
            return;
        }
        if (!shouldTriggerCounter(player, PAINKILLER_COUNTER_KEY, 20)) {
            return;
        }
        float decay = Math.min(1.0f, pool);
        float nextPool = Math.max(pool - decay, 0.0f);
        setPainkillerPool(player, nextPool);
        float nextAbsorption = Math.max(player.getAbsorptionAmount() - decay, 0.0f);
        player.setAbsorptionAmount(nextAbsorption);
        if (nextPool <= 0.0f) {
            player.removeEffect(ModMobEffects.EMERGENCY_PAINKILLER);
            clearCounter(player, PAINKILLER_COUNTER_KEY);
            clearPainkillerPool(player);
        }
    }

    private static void freezePositiveEffects(Player player) {
        for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
            Holder<MobEffect> effect = instance.getEffect();
            if (effect == null || effect == ModMobEffects.HEALTHY) {
                continue;
            }
            if (effect.value().getCategory() != MobEffectCategory.BENEFICIAL) {
                continue;
            }
            int duration = instance.getDuration();
            if (duration <= 0 || duration >= 72_000) {
                continue;
            }
            MobEffectInstance frozen = new MobEffectInstance(
                    effect,
                    duration + 1,
                    instance.getAmplifier(),
                    instance.isAmbient(),
                    instance.isVisible(),
                    instance.showIcon()
            );
            player.addEffect(frozen, player);
        }
    }

    private static void clearHarmfulEffects(Player player) {
        for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
            Holder<MobEffect> effect = instance.getEffect();
            if (effect != null && effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect);
            }
        }
    }

    private static boolean hasEffect(Player player, Holder<MobEffect> effect) {
        return player != null && effect != null && player.hasEffect(effect);
    }

    private static boolean shouldTriggerCounter(Player player, String key, int intervalTicks) {
        int interval = Math.max(intervalTicks, 1);
        CompoundTag root = getRootTag(player);
        int counter = root.getInt(key) + 1;
        if (counter >= interval) {
            root.putInt(key, 0);
            return true;
        }
        root.putInt(key, counter);
        return false;
    }

    private static void clearCounter(Player player, String key) {
        CompoundTag root = getRootTag(player);
        root.remove(key);
    }

    private static float getPainkillerPool(Player player) {
        CompoundTag root = getRootTag(player);
        if (!root.contains(PAINKILLER_POOL_KEY, Tag.TAG_FLOAT)) {
            return 0.0f;
        }
        return Math.max(root.getFloat(PAINKILLER_POOL_KEY), 0.0f);
    }

    private static void setPainkillerPool(Player player, float value) {
        CompoundTag root = getRootTag(player);
        float normalized = Math.max(value, 0.0f);
        if (normalized <= 0.0f) {
            root.remove(PAINKILLER_POOL_KEY);
            return;
        }
        root.putFloat(PAINKILLER_POOL_KEY, normalized);
    }

    private static void clearPainkillerPool(Player player) {
        CompoundTag root = getRootTag(player);
        root.remove(PAINKILLER_POOL_KEY);
    }

    private static CompoundTag getRootTag(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            data.put(ROOT_KEY, new CompoundTag());
        }
        return data.getCompound(ROOT_KEY);
    }
}
