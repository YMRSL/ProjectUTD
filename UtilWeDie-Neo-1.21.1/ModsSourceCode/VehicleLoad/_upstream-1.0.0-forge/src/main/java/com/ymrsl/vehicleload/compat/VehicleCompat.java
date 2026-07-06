package com.ymrsl.vehicleload.compat;

import com.ymrsl.vehicleload.VehicleLoadMod;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

public final class VehicleCompat {
    private static final String SW_VEHICLE_CLASS = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity";
    private static final String SW_ARTILLERY_CLASS = "com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity";
    private static final String SW_AUTOAIMABLE_CLASS = "com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity";

    private static final String AM_AUTOMOBILE_CLASS = "io.github.foundationgames.automobility.entity.AutomobileEntity";

    private static boolean swInitAttempted;
    private static Class<?> swVehicleClass;
    private static Class<?> swArtilleryClass;
    private static Class<?> swAutoAimableClass;

    private static boolean amInitAttempted;
    private static Class<?> amAutomobileClass;

    private VehicleCompat() {
    }

    public static boolean isTargetVehicle(Entity entity) {
        if (isAutomobilityVehicle(entity)) {
            return true;
        }
        if (!ModList.get().isLoaded("superbwarfare")) {
            return false;
        }
        initSuperbwarfare();
        return (swVehicleClass != null && swVehicleClass.isInstance(entity))
            || (swArtilleryClass != null && swArtilleryClass.isInstance(entity))
            || (swAutoAimableClass != null && swAutoAimableClass.isInstance(entity));
    }

    public static boolean isSuperbwarfareVehicle(Entity entity) {
        if (!ModList.get().isLoaded("superbwarfare")) {
            return false;
        }
        initSuperbwarfare();
        return swVehicleClass != null && swVehicleClass.isInstance(entity);
    }

    public static boolean isAutomobilityVehicle(Entity entity) {
        if (!ModList.get().isLoaded("automobility")) {
            return false;
        }
        initAutomobility();
        return amAutomobileClass != null && amAutomobileClass.isInstance(entity);
    }

    public static boolean isImmobile(Entity entity) {
        initSuperbwarfare();
        if (swArtilleryClass != null && swArtilleryClass.isInstance(entity)) {
            return true;
        }
        if (swAutoAimableClass != null && swAutoAimableClass.isInstance(entity)) {
            return true;
        }
        return false;
    }

    private static void initSuperbwarfare() {
        if (swInitAttempted) {
            return;
        }
        swInitAttempted = true;
        if (!ModList.get().isLoaded("superbwarfare")) {
            return;
        }
        try {
            swVehicleClass = Class.forName(SW_VEHICLE_CLASS);
            swArtilleryClass = Class.forName(SW_ARTILLERY_CLASS);
            swAutoAimableClass = Class.forName(SW_AUTOAIMABLE_CLASS);
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.warn("VehicleCompat: SuperbWarfare reflection init failed", e);
        }
    }

    private static void initAutomobility() {
        if (amInitAttempted) {
            return;
        }
        amInitAttempted = true;
        if (!ModList.get().isLoaded("automobility")) {
            return;
        }
        try {
            amAutomobileClass = Class.forName(AM_AUTOMOBILE_CLASS);
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.warn("VehicleCompat: Automobility reflection init failed", e);
        }
    }
}
