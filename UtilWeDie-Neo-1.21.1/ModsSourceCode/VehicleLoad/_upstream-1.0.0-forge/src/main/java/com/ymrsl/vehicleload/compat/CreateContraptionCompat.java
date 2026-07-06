package com.ymrsl.vehicleload.compat;

import com.ymrsl.vehicleload.VehicleLoadMod;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

public final class CreateContraptionCompat {
    private static final String CONTRAPTION_ENTITY_CLASS =
        "com.simibubi.create.content.contraptions.AbstractContraptionEntity";
    private static final String ORIENTED_CONTRAPTION_ENTITY_CLASS =
        "com.simibubi.create.content.contraptions.OrientedContraptionEntity";
    private static final String ROTATION_STATE_CLASS =
        "com.simibubi.create.content.contraptions.AbstractContraptionEntity$ContraptionRotationState";
    private static final String CONTRAPTION_CLASS =
        "com.simibubi.create.content.contraptions.Contraption";

    private static boolean initAttempted;
    private static Class<?> contraptionEntityClass;
    private static Class<?> orientedContraptionEntityClass;
    private static Class<?> rotationStateClass;
    private static Class<?> contraptionClass;
    private static Method toGlobalVectorMethod;
    private static Method toLocalVectorMethod;
    private static Method getRotationStateMethod;
    private static Method getContraptionMethod;
    private static Method getSeatsMethod;
    private static Method getSeatMappingMethod;
    private static Method addSittingPassengerMethod;
    private static Method orientedYawMethod;
    private static Method orientedPitchMethod;
    private static Field rotationStateYawField;
    private static Field rotationStatePitchField;

    private CreateContraptionCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("create") && init();
    }

    public static String debugSummary() {
        boolean createLoaded = ModList.get().isLoaded("create");
        if (createLoaded) {
            init();
        }
        return "createLoaded=" + createLoaded
            + ", initAttempted=" + initAttempted
            + ", contraptionEntityClass=" + classState(contraptionEntityClass)
            + ", orientedContraptionEntityClass=" + classState(orientedContraptionEntityClass)
            + ", rotationStateClass=" + classState(rotationStateClass)
            + ", contraptionClass=" + classState(contraptionClass)
            + ", toGlobalVector=" + methodState(toGlobalVectorMethod)
            + ", toLocalVector=" + methodState(toLocalVectorMethod)
            + ", getRotationState=" + methodState(getRotationStateMethod)
            + ", getContraption=" + methodState(getContraptionMethod)
            + ", addSittingPassenger=" + methodState(addSittingPassengerMethod)
            + ", getSeats=" + methodState(getSeatsMethod)
            + ", getSeatMapping=" + methodState(getSeatMappingMethod)
            + ", orientedYaw=" + methodState(orientedYawMethod)
            + ", orientedPitch=" + methodState(orientedPitchMethod)
            + ", rotationStateYaw=" + fieldState(rotationStateYawField)
            + ", rotationStatePitch=" + fieldState(rotationStatePitchField);
    }

    public static boolean isContraptionEntity(Entity entity) {
        if (!isLoaded() || contraptionEntityClass == null) {
            return false;
        }
        return contraptionEntityClass.isInstance(entity);
    }

    public static Vec3 toGlobalVector(Entity contraptionEntity, Vec3 localPos) {
        if (!isLoaded()) {
            return null;
        }
        return castVec3(invoke(toGlobalVectorMethod, contraptionEntity, localPos, 1.0f));
    }

    public static Vec3 toLocalVector(Entity contraptionEntity, Vec3 globalPos) {
        if (!isLoaded()) {
            return null;
        }
        return castVec3(invoke(toLocalVectorMethod, contraptionEntity, globalPos, 1.0f));
    }

    public static List<BlockPos> getSeatPositions(Entity contraptionEntity) {
        if (!isLoaded() || contraptionEntity == null || getContraptionMethod == null || getSeatsMethod == null) {
            return Collections.emptyList();
        }
        Object contraption = invoke(getContraptionMethod, contraptionEntity);
        if (contraption == null) {
            return Collections.emptyList();
        }
        Object seats = invoke(getSeatsMethod, contraption);
        if (seats instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<BlockPos> cast = (List<BlockPos>) list;
            return cast;
        }
        return Collections.emptyList();
    }

    public static Map<UUID, Integer> getSeatMapping(Entity contraptionEntity) {
        if (!isLoaded() || contraptionEntity == null || getContraptionMethod == null || getSeatMappingMethod == null) {
            return Collections.emptyMap();
        }
        Object contraption = invoke(getContraptionMethod, contraptionEntity);
        if (contraption == null) {
            return Collections.emptyMap();
        }
        Object mapping = invoke(getSeatMappingMethod, contraption);
        if (mapping instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<UUID, Integer> cast = (Map<UUID, Integer>) map;
            return cast;
        }
        return Collections.emptyMap();
    }

    public static boolean addSittingPassenger(Entity contraptionEntity, Entity passenger, int seatIndex) {
        if (!isLoaded() || contraptionEntity == null || passenger == null || addSittingPassengerMethod == null) {
            return false;
        }
        Object result = invoke(addSittingPassengerMethod, contraptionEntity, passenger, seatIndex);
        return result != null || passenger.isPassenger();
    }

    public static Float getContraptionYaw(Entity contraptionEntity, float partialTicks) {
        if (!isLoaded() || contraptionEntity == null || !isContraptionEntity(contraptionEntity)) {
            return null;
        }
        if (orientedContraptionEntityClass != null
            && orientedContraptionEntityClass.isInstance(contraptionEntity)
            && orientedYawMethod != null) {
            return castFloat(invoke(orientedYawMethod, contraptionEntity, partialTicks));
        }
        Object state = invoke(getRotationStateMethod, contraptionEntity);
        if (state != null && rotationStateYawField != null) {
            return getFieldFloat(rotationStateYawField, state);
        }
        return null;
    }

    public static Float getContraptionPitch(Entity contraptionEntity, float partialTicks) {
        if (!isLoaded() || contraptionEntity == null || !isContraptionEntity(contraptionEntity)) {
            return null;
        }
        if (orientedContraptionEntityClass != null
            && orientedContraptionEntityClass.isInstance(contraptionEntity)
            && orientedPitchMethod != null) {
            return castFloat(invoke(orientedPitchMethod, contraptionEntity, partialTicks));
        }
        Object state = invoke(getRotationStateMethod, contraptionEntity);
        if (state != null && rotationStatePitchField != null) {
            return getFieldFloat(rotationStatePitchField, state);
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
            VehicleLoadMod.LOGGER.debug("CreateContraptionCompat: reflection invoke failed", e);
            return null;
        }
    }

    private static Vec3 castVec3(Object value) {
        if (value instanceof Vec3) {
            return (Vec3) value;
        }
        return null;
    }

    private static Float castFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return null;
    }

    private static Float getFieldFloat(Field field, Object target) {
        if (field == null || target == null) {
            return null;
        }
        try {
            return field.getFloat(target);
        } catch (IllegalAccessException e) {
            VehicleLoadMod.LOGGER.debug("CreateContraptionCompat: reflection field access failed", e);
            return null;
        }
    }

    private static boolean init() {
        if (initAttempted) {
            return contraptionEntityClass != null;
        }
        initAttempted = true;
        try {
            contraptionEntityClass = Class.forName(CONTRAPTION_ENTITY_CLASS);
            toGlobalVectorMethod = contraptionEntityClass.getMethod("toGlobalVector", Vec3.class, float.class);
            toLocalVectorMethod = contraptionEntityClass.getMethod("toLocalVector", Vec3.class, float.class);
            getRotationStateMethod = contraptionEntityClass.getMethod("getRotationState");
            getContraptionMethod = contraptionEntityClass.getMethod("getContraption");
            addSittingPassengerMethod = contraptionEntityClass.getMethod("addSittingPassenger", Entity.class, int.class);
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.warn("CreateContraptionCompat init failed", e);
            return false;
        }
        try {
            orientedContraptionEntityClass = Class.forName(ORIENTED_CONTRAPTION_ENTITY_CLASS);
            orientedYawMethod = orientedContraptionEntityClass.getMethod("m_5675_", float.class);
            orientedPitchMethod = orientedContraptionEntityClass.getMethod("m_5686_", float.class);
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.debug("CreateContraptionCompat: oriented reflection init failed", e);
        }
        try {
            rotationStateClass = Class.forName(ROTATION_STATE_CLASS);
            rotationStateYawField = rotationStateClass.getField("yRotation");
            rotationStatePitchField = rotationStateClass.getField("zRotation");
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.debug("CreateContraptionCompat: rotation state reflection init failed", e);
        }
        try {
            contraptionClass = Class.forName(CONTRAPTION_CLASS);
            getSeatsMethod = contraptionClass.getMethod("getSeats");
            getSeatMappingMethod = contraptionClass.getMethod("getSeatMapping");
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.debug("CreateContraptionCompat: seat reflection init failed", e);
        }
        return true;
    }

    private static String classState(Class<?> type) {
        return type == null ? "missing" : type.getName();
    }

    private static String methodState(Method method) {
        return method == null ? "missing" : method.getName();
    }

    private static String fieldState(Field field) {
        return field == null ? "missing" : field.getName();
    }
}
