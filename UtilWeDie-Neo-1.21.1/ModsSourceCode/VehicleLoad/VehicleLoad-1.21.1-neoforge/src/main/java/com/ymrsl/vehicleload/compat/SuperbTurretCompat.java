package com.ymrsl.vehicleload.compat;

import com.ymrsl.vehicleload.VehicleLoadMod;
import java.lang.reflect.Method;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

public final class SuperbTurretCompat {
    private static final String VEHICLE_ENTITY_CLASS =
        "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity";

    private static boolean initAttempted;
    private static Class<?> vehicleClass;
    private static Method hasTurretMethod;
    private static Method getTurretYRotMethod;
    private static Method setTurretYRotMethod;
    private static Method getTurretXRotMethod;
    private static Method setTurretXRotMethod;
    private static Method getTurretControllerIndexMethod;
    private static Method getNthEntityMethod;
    private static Method getShootVecMethod;
    private static Method setTurretYRotOMethod;
    private static Method setTurretXRotOMethod;

    private SuperbTurretCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("superbwarfare") && init();
    }

    public static boolean isVehicle(Entity entity) {
        if (!isLoaded() || entity == null) {
            return false;
        }
        return vehicleClass != null && vehicleClass.isInstance(entity);
    }

    public static boolean hasTurret(Entity entity) {
        if (!isVehicle(entity)) {
            return false;
        }
        Boolean value = castBoolean(invoke(hasTurretMethod, entity));
        return value == null || value;
    }

    public static Float getTurretYRot(Entity entity) {
        if (!isVehicle(entity)) {
            return null;
        }
        return castFloat(invoke(getTurretYRotMethod, entity));
    }

    public static void setTurretYRot(Entity entity, float yaw) {
        if (!isVehicle(entity)) {
            return;
        }
        invoke(setTurretYRotMethod, entity, yaw);
    }

    public static void setTurretXRot(Entity entity, float pitch) {
        if (!isVehicle(entity)) {
            return;
        }
        invoke(setTurretXRotMethod, entity, pitch);
    }

    public static void setTurretYRotO(Entity entity, float yaw) {
        if (!isVehicle(entity)) {
            return;
        }
        invoke(setTurretYRotOMethod, entity, yaw);
    }

    public static void setTurretXRotO(Entity entity, float pitch) {
        if (!isVehicle(entity)) {
            return;
        }
        invoke(setTurretXRotOMethod, entity, pitch);
    }

    public static Entity getTurretController(Entity entity) {
        if (!isVehicle(entity)) {
            return null;
        }
        Integer index = castInt(invoke(getTurretControllerIndexMethod, entity));
        if (index == null) {
            return null;
        }
        Object controller = invoke(getNthEntityMethod, entity, index.intValue());
        if (controller instanceof Entity result) {
            return result;
        }
        return null;
    }

    public static net.minecraft.world.phys.Vec3 getShootVec(Entity entity, Entity passenger, float partialTicks) {
        if (!isVehicle(entity) || passenger == null) {
            return null;
        }
        Object value = invoke(getShootVecMethod, entity, passenger, partialTicks);
        if (value instanceof net.minecraft.world.phys.Vec3 vec) {
            return vec;
        }
        return null;
    }

    private static Object invoke(Method method, Object target, Object... args) {
        if (method == null || target == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.debug("SuperbTurretCompat: reflection invoke failed", e);
            return null;
        }
    }

    private static Boolean castBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return null;
    }

    private static Float castFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return null;
    }

    private static Integer castInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static boolean init() {
        if (initAttempted) {
            return vehicleClass != null;
        }
        initAttempted = true;
        try {
            vehicleClass = Class.forName(VEHICLE_ENTITY_CLASS);
            hasTurretMethod = vehicleClass.getMethod("hasTurret");
            getTurretYRotMethod = vehicleClass.getMethod("getTurretYRot");
            setTurretYRotMethod = vehicleClass.getMethod("setTurretYRot", float.class);
            getTurretXRotMethod = vehicleClass.getMethod("getTurretXRot");
            setTurretXRotMethod = vehicleClass.getMethod("setTurretXRot", float.class);
            getTurretControllerIndexMethod = vehicleClass.getMethod("getTurretControllerIndex");
            getNthEntityMethod = vehicleClass.getMethod("getNthEntity", int.class);
            getShootVecMethod = vehicleClass.getMethod("getShootVec", Entity.class, float.class);
            // 0.8.9: turretYRotO/turretXRotO fields are private; use public accessors.
            setTurretYRotOMethod = vehicleClass.getMethod("setTurretYRotO", float.class);
            setTurretXRotOMethod = vehicleClass.getMethod("setTurretXRotO", float.class);
            return true;
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.warn("SuperbTurretCompat init failed", e);
        }
        return false;
    }
}
