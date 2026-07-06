package com.scarasol.tud.compat;

import com.scarasol.tud.manager.AmmoManager;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Warium (crusty_chunks) casing-ejection compat.
 * <p>
 * When a TaCZ gun chambered with one of the tacz_unidict general ammo types is fired, eject the
 * matching crusty_chunks casing item. Ported from the user's 1.20.1 tacz_unidict 1.5.0-YMRFixed
 * build (the casing feature is absent from upstream 2.0.1) and extended per-firearm-type:
 * <ul>
 *     <li>Normal guns (and semi/full-auto shotguns): eject one casing immediately on fire.</li>
 *     <li>Bolt-action rifles/snipers and pump-action shotguns ({@link Category#BOLT_ACTION}): eject
 *         one casing after a 1 second delay (mimics cycling the bolt / pump).</li>
 *     <li>Revolvers and break-action (double / single barrel) shotguns ({@link Category#REVOLVER}):
 *         hold spent casings in the gun and eject all of them at once on the next reload.</li>
 * </ul>
 */
public final class WariumCasingCompat {

    public static final String MOD_CRUSTY = "crusty_chunks";
    public static final String MOD_TACZ = "tacz";

    /** Ticks to wait before a bolt-action gun ejects its casing (1 second). */
    public static final int BOLT_ACTION_DELAY_TICKS = 20;
    /** Max magazine capacity an OPEN_BOLT pistol-caliber gun may have to count as a revolver. */
    public static final int REVOLVER_MAX_CAPACITY = 12;
    /** Max capacity a shotgun may have to count as a break-action (double / single barrel). */
    public static final int BREAK_ACTION_MAX_CAPACITY = 2;
    /** Safety cap on how many casings a single revolver reload may eject. */
    public static final int REVOLVER_EJECT_CAP = 24;

    private static final String NBT_CASING_COUNT = "TudCasingCount";
    private static final String NBT_CASING_ID = "TudCasingId";

    private static final boolean HAS_CRUSTY = ModList.get().isLoaded(MOD_CRUSTY);
    private static final boolean HAS_TACZ = ModList.get().isLoaded(MOD_TACZ);

    // tacz_unidict general ammo item ids (set by the "Ammo of Gun" config mapping).
    public static final ResourceLocation TUD_PISTOL = ResourceLocation.fromNamespaceAndPath("tacz_unidict", "pistol");
    public static final ResourceLocation TUD_RIFLE = ResourceLocation.fromNamespaceAndPath("tacz_unidict", "rifle");
    public static final ResourceLocation TUD_SNIPER = ResourceLocation.fromNamespaceAndPath("tacz_unidict", "sniper");
    public static final ResourceLocation TUD_SHOT = ResourceLocation.fromNamespaceAndPath("tacz_unidict", "shot");

    // crusty_chunks casing item ids.
    public static final ResourceLocation SMALL_CASING = ResourceLocation.fromNamespaceAndPath(MOD_CRUSTY, "small_casing");
    public static final ResourceLocation MEDIUM_CASING = ResourceLocation.fromNamespaceAndPath(MOD_CRUSTY, "medium_casing");
    public static final ResourceLocation LARGE_CASING = ResourceLocation.fromNamespaceAndPath(MOD_CRUSTY, "large_casing");
    public static final ResourceLocation SHOTGUN_CASING = ResourceLocation.fromNamespaceAndPath(MOD_CRUSTY, "shotgun_casing");

    public enum Category {
        NORMAL,
        BOLT_ACTION,
        REVOLVER
    }

    private WariumCasingCompat() {
    }

    public static boolean isActive() {
        return HAS_CRUSTY && HAS_TACZ;
    }

    /**
     * The tacz_unidict general ammo id currently chambered in the gun, or {@code null} if the gun is
     * not firing a recognised general ammo type.
     */
    public static ResourceLocation getGeneralAmmoId(ItemStack gunStack) {
        if (gunStack == null || gunStack.isEmpty()) {
            return null;
        }
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) {
            return null;
        }
        if (!AmmoManager.canUseGeneralAmmo(iGun.getGunId(gunStack).toString(), null)) {
            return null;
        }
        Tuple<ResourceLocation, Boolean> ammo = AmmoManager.getAmmo(gunStack);
        if (ammo == null) {
            return null;
        }
        ResourceLocation id = ammo.getA();
        return isGeneralAmmoId(id) ? id : null;
    }

    private static boolean isGeneralAmmoId(ResourceLocation id) {
        return TUD_PISTOL.equals(id) || TUD_RIFLE.equals(id) || TUD_SNIPER.equals(id) || TUD_SHOT.equals(id);
    }

    private static ResourceLocation casingIdForAmmo(ResourceLocation ammoId) {
        if (TUD_PISTOL.equals(ammoId)) {
            return SMALL_CASING;
        }
        if (TUD_RIFLE.equals(ammoId)) {
            return MEDIUM_CASING;
        }
        if (TUD_SNIPER.equals(ammoId)) {
            return LARGE_CASING;
        }
        if (TUD_SHOT.equals(ammoId)) {
            return SHOTGUN_CASING;
        }
        return null;
    }

    /** A single casing stack for the given general ammo id, or {@link ItemStack#EMPTY}. */
    public static ItemStack casingForAmmoId(ResourceLocation ammoId) {
        return stackOf(casingIdForAmmo(ammoId));
    }

    private static ItemStack stackOf(ResourceLocation itemId) {
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return (item == null || item == Items.AIR) ? ItemStack.EMPTY : new ItemStack(item, 1);
    }

    private static GunData getTaczGunData(ItemStack gunStack) {
        IGun iGun = IGun.getIGunOrNull(gunStack);
        if (iGun == null) {
            return null;
        }
        return TimelessAPI.getCommonGunIndex(iGun.getGunId(gunStack)).map(CommonGunIndex::getGunData).orElse(null);
    }

    /**
     * Classify the firearm by its TaCZ bolt type and the general ammo class. Evaluated at fire time
     * (when the gun is loaded), so {@code ammoId} is the live chambered general ammo id.
     */
    public static Category categorize(ItemStack gunStack, ResourceLocation ammoId) {
        GunData gunData = getTaczGunData(gunStack);
        if (gunData == null) {
            return Category.NORMAL;
        }
        Bolt bolt = gunData.getBolt();
        int capacity = gunData.getAmmoAmount();
        if (bolt == Bolt.MANUAL_ACTION && (TUD_RIFLE.equals(ammoId) || TUD_SNIPER.equals(ammoId))) {
            // Bolt-action rifle / sniper.
            return Category.BOLT_ACTION;
        }
        if (bolt == Bolt.OPEN_BOLT && TUD_PISTOL.equals(ammoId) && capacity <= REVOLVER_MAX_CAPACITY) {
            // Revolver: open-bolt pistol-caliber with a small cylinder. Semi-auto pistols and SMGs
            // are CLOSED_BOLT, so they fall through to NORMAL.
            return Category.REVOLVER;
        }
        if (TUD_SHOT.equals(ammoId)) {
            // Break-action shotguns (double / single barrel) hold their hulls until the gun is
            // broken open to reload, so they reuse the revolver behaviour. Pump-action shotguns are
            // MANUAL_ACTION and cycle a hull out with a short delay, like a bolt gun. Semi/full-auto
            // shotguns eject immediately. Break actions are the only shotguns with <= 2 capacity
            // (pumps/autos hold 5+), so capacity cleanly separates them.
            if (capacity <= BREAK_ACTION_MAX_CAPACITY) {
                return Category.REVOLVER;
            }
            if (bolt == Bolt.MANUAL_ACTION) {
                return Category.BOLT_ACTION;
            }
        }
        return Category.NORMAL;
    }

    /** Eject the casing item entity to the shooter's right, mirroring the 1.5.0 ejection arc. */
    public static void spawnCasingToRight(Level level, LivingEntity shooter, ItemStack casing) {
        if (level == null || shooter == null || casing.isEmpty()) {
            return;
        }
        Vec3 forward = shooter.getLookAngle();
        Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 spawnPos = shooter.position()
                .add(0.0, shooter.getBbHeight() * 0.6, 0.0)
                .add(right.scale(0.35));
        ItemEntity entity = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z, casing);
        entity.setPickUpDelay(10);
        entity.setDeltaMovement(right.scale(0.2).add(0.0, 0.05, 0.0));
        level.addFreshEntity(entity);
    }

    // --- Delayed ejection (bolt-action) --------------------------------------------------------
    // A self-managed queue ticked once per server tick. MinecraftServer#tell with a future-tick
    // TickTask is unreliable for this (its task queue is FIFO, so a future-dated task stalls at the
    // head instead of firing on schedule), so we count the delay down ourselves.

    private static final List<PendingEject> PENDING_EJECTS = new ArrayList<>();

    private static final class PendingEject {
        private final LivingEntity shooter;
        private final ItemStack casing;
        private int ticksLeft;

        private PendingEject(LivingEntity shooter, ItemStack casing, int ticksLeft) {
            this.shooter = shooter;
            this.casing = casing;
            this.ticksLeft = ticksLeft;
        }
    }

    /** Queue a casing to be ejected {@code delayTicks} server ticks from now (bolt-action cycling). */
    public static void scheduleDelayedEject(LivingEntity shooter, ItemStack casing, int delayTicks) {
        if (shooter == null || casing.isEmpty()) {
            return;
        }
        PENDING_EJECTS.add(new PendingEject(shooter, casing.copy(), Math.max(1, delayTicks)));
    }

    /** Advance every pending delayed ejection by one tick; call once per server tick. */
    public static void tickPendingEjects() {
        if (PENDING_EJECTS.isEmpty()) {
            return;
        }
        Iterator<PendingEject> it = PENDING_EJECTS.iterator();
        while (it.hasNext()) {
            PendingEject pending = it.next();
            if (--pending.ticksLeft <= 0) {
                if (pending.shooter.isAlive() && pending.shooter.level() instanceof ServerLevel level) {
                    spawnCasingToRight(level, pending.shooter, pending.casing);
                }
                it.remove();
            }
        }
    }

    // --- Revolver: spent casings held in the gun until reload ---------------------------------

    /** Record one more spent casing of the given type on the gun (revolvers only). */
    public static void addStoredCasing(ItemStack gun, ResourceLocation casingId) {
        if (gun == null || gun.isEmpty() || casingId == null) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> {
            tag.putInt(NBT_CASING_COUNT, tag.getInt(NBT_CASING_COUNT) + 1);
            tag.putString(NBT_CASING_ID, casingId.toString());
        });
    }

    public static int getStoredCasingCount(ItemStack gun) {
        if (gun == null || gun.isEmpty()) {
            return 0;
        }
        CustomData data = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getInt(NBT_CASING_COUNT);
    }

    public static ItemStack getStoredCasing(ItemStack gun) {
        if (gun == null || gun.isEmpty()) {
            return ItemStack.EMPTY;
        }
        CompoundTag tag = gun.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String raw = tag.getString(NBT_CASING_ID);
        return raw.isEmpty() ? ItemStack.EMPTY : stackOf(ResourceLocation.tryParse(raw));
    }

    public static void clearStoredCasings(ItemStack gun) {
        if (gun == null || gun.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, gun, tag -> {
            tag.remove(NBT_CASING_COUNT);
            tag.remove(NBT_CASING_ID);
        });
    }
}
