package com.scarasol.tud.event;

import com.scarasol.tud.TudMod;
import com.scarasol.tud.compat.WariumCasingCompat;
import com.scarasol.tud.data.*;
import com.scarasol.tud.util.data.DataManager;
import com.scarasol.tud.util.io.ModGson;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * @author Scarasol
 */
@EventBusSubscriber(modid = TudMod.MODID)
public class EventHandler {


    private static final int DEFAULT_DURATION_TICKS = 200;

    /**
     * Warium (crusty_chunks) compat: eject the matching casing item when a TaCZ gun firing
     * tacz_unidict general ammo is fired. Server side only. No-op unless crusty_chunks + tacz
     * are both loaded. Behaviour varies by firearm type (see {@link WariumCasingCompat}):
     * normal = immediate, bolt-action rifle/sniper = delayed 1s, revolver = held until reload.
     * Ported from the 1.20.1 1.5.0-YMRFixed casing feature (upstream 2.0.1 lacks it).
     */
    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!WariumCasingCompat.isActive() || event.getLogicalSide().isClient()) {
            return;
        }
        LivingEntity shooter = event.getShooter();
        if (shooter == null) {
            return;
        }
        ItemStack gun = event.getGunItemStack();
        ResourceLocation ammoId = WariumCasingCompat.getGeneralAmmoId(gun);
        if (ammoId == null) {
            return;
        }
        ItemStack casing = WariumCasingCompat.casingForAmmoId(ammoId);
        if (casing.isEmpty()) {
            return;
        }
        switch (WariumCasingCompat.categorize(gun, ammoId)) {
            // Revolver: keep spent casings in the cylinder, eject them all on the next reload.
            case REVOLVER -> WariumCasingCompat.addStoredCasing(gun, BuiltInRegistries.ITEM.getKey(casing.getItem()));
            // Bolt-action rifle/sniper: eject one casing after cycling the bolt (~1 second).
            case BOLT_ACTION -> WariumCasingCompat.scheduleDelayedEject(shooter, casing, WariumCasingCompat.BOLT_ACTION_DELAY_TICKS);
            // Everything else: eject one casing immediately.
            default -> WariumCasingCompat.spawnCasingToRight(shooter.level(), shooter, casing);
        }
    }

    /**
     * Warium compat (revolvers): on reload, eject all spent casings that accumulated in the
     * cylinder since the last reload. No-op for any gun that did not stash casings on fire.
     */
    @SubscribeEvent
    public static void onGunReload(GunReloadEvent event) {
        if (!WariumCasingCompat.isActive() || event.getLogicalSide().isClient()) {
            return;
        }
        LivingEntity shooter = event.getEntity();
        if (shooter == null) {
            return;
        }
        ItemStack gun = event.getGunItemStack();
        int count = WariumCasingCompat.getStoredCasingCount(gun);
        if (count <= 0) {
            return;
        }
        ItemStack casing = WariumCasingCompat.getStoredCasing(gun);
        if (!casing.isEmpty()) {
            int eject = Math.min(count, WariumCasingCompat.REVOLVER_EJECT_CAP);
            for (int i = 0; i < eject; i++) {
                WariumCasingCompat.spawnCasingToRight(shooter.level(), shooter, casing.copy());
            }
        }
        WariumCasingCompat.clearStoredCasings(gun);
    }

    /** Drives the bolt-action delayed casing ejections (counts each pending delay down by one tick). */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WariumCasingCompat.tickPendingEjects();
    }

    @SubscribeEvent
    public static void onEntityHurtByGunPost(EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }

        Entity hurt = event.getHurtEntity();
        if (hurt == null) {
            return;
        }

        Entity bullet = event.getBullet();
        String ammoId = bullet.getPersistentData().getString("TudAmmoId");

        AmmoData ammoData = DataManager.getSearchableModData(AmmoData.class, ammoId);
        if (ammoData == null) {
            return;
        }

        String modifierId = ammoData.getModifierId();
        if (modifierId == null) {
            return;
        }
        ModifierData modifierData = DataManager.getSearchableModData(ModifierData.class, modifierId);
        if (modifierData == null) {
            return;
        }
        event.setBaseAmount((float) (event.getBaseAmount() * modifierData.getModifier(hurt)));
    }


    @SubscribeEvent
    public static void onEntityHurtByGunPost(EntityHurtByGunEvent.Post event) {
        if (event.getLogicalSide().isClient()) {
            return;
        }

        Entity hurt = event.getHurtEntity();
        if (!(hurt instanceof LivingEntity living)) {
            return;
        }
        Entity bullet = event.getBullet();
        String ammoId = bullet.getPersistentData().getString("TudAmmoId");

        AmmoData ammoData = DataManager.getSearchableModData(AmmoData.class, ammoId);
        if (ammoData == null) {
            return;
        }

        List<EffectData> list = ammoData.getEffectDataList();
        if (list == null || list.isEmpty()) {
            return;
        }

        applyEffects(living, list);
    }

    private static void applyEffects(LivingEntity target, List<EffectData> list) {
        for (EffectData data : list) {
            if (data == null || data.resourceLocation() == null) {
                continue;
            }

            ResourceLocation effectId = data.resourceLocation();
            @Nullable Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT
                    .getHolder(ResourceKey.create(Registries.MOB_EFFECT, effectId)).orElse(null);
            if (effect == null) {
                continue;
            }

            int amplifier = data.amplifier() == null ? 0 : Math.max(0, data.amplifier());
            int duration = data.duration() == null ? DEFAULT_DURATION_TICKS : Math.max(1, data.duration() * 20);

            target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    @SubscribeEvent
    public static void loadData(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }
        loadModData();
    }

    @SubscribeEvent
    public static void loadData(ServerStartedEvent event) {
        loadModData();
    }

    private static void loadModData() {
        try {
            DataManager.clear(GunData.class);
            DataManager.clear(AmmoData.class);
            DataManager.clear(TaczGunDataMap.class);
            DataManager.clear(ModifierData.class);
            ModGson.INSTANCE.loadAll(FMLPaths.CONFIGDIR.get().resolve(TudMod.MODID).resolve("modifier_data"));
            ModGson.INSTANCE.loadAll(FMLPaths.CONFIGDIR.get().resolve(TudMod.MODID).resolve("gun_data"));
            ModGson.INSTANCE.loadAll(FMLPaths.CONFIGDIR.get().resolve(TudMod.MODID).resolve("ammo_data"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
