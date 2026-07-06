package com.ymrsl.vehicleload.compat;

import com.ymrsl.vehicleload.VehicleLoadMod;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

/**
 * Reflection bridge to Sable physics structures (sub levels). A sub level is a
 * mini-world whose blocks live at "plot" coordinates inside the same dimension,
 * plus a pose (position/rotation) mapping plot space to world space. Aeronautics
 * airships are sable sub levels too, so this one bridge covers both.
 *
 * Mirrors the reflection style of {@link CreateContraptionCompat}: no compile
 * dependency, everything resolved lazily behind a ModList gate.
 */
public final class SableStructureCompat {
    private static final String CONTAINER_CLASS = "dev.ryanhcode.sable.api.sublevel.SubLevelContainer";
    private static final String SUBLEVEL_CLASS = "dev.ryanhcode.sable.sublevel.SubLevel";
    private static final String POSE_CLASS = "dev.ryanhcode.sable.companion.math.Pose3dc";
    private static final String PLOT_CLASS = "dev.ryanhcode.sable.sublevel.plot.LevelPlot";
    private static final String BOUNDS_CLASS = "dev.ryanhcode.sable.companion.math.BoundingBox3ic";
    private static final String CREATE_SEAT_CLASS = "com.simibubi.create.content.contraptions.actors.seat.SeatBlock";
    private static final String CREATE_SEAT_ENTITY_CLASS = "com.simibubi.create.content.contraptions.actors.seat.SeatEntity";

    private static boolean initAttempted;
    private static boolean initOk;
    private static Method getContainerMethod;      // SubLevelContainer.getContainer(ServerLevel)
    private static Method getContainerAnyMethod;   // SubLevelContainer.getContainer(Level) — works on client too
    private static Method getAllSubLevelsMethod;   // container.getAllSubLevels()
    private static Method getSubLevelByUuidMethod; // container.getSubLevel(UUID)
    private static Method getUniqueIdMethod;       // subLevel.getUniqueId()
    private static Method logicalPoseMethod;       // subLevel.logicalPose()
    private static Method getPlotMethod;           // subLevel.getPlot()
    private static Method transformPositionMethod; // pose.transformPosition(Vec3)
    private static Method transformNormalMethod;   // pose.transformNormal(Vec3)
    private static Method transformNormalInverseMethod; // pose.transformNormalInverse(Vec3)
    private static Method plotBoundingBoxMethod;   // plot.getBoundingBox()
    private static Method boundsMinX, boundsMinY, boundsMinZ, boundsMaxX, boundsMaxY, boundsMaxZ;

    private static boolean seatInitAttempted;
    private static Class<?> createSeatBlockClass;
    private static Class<?> createSeatEntityClass;
    private static Method seatOccupiedMethod;
    private static Method sitDownMethod;

    private SableStructureCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("sable") && init();
    }

    private static synchronized boolean init() {
        if (initAttempted) {
            return initOk;
        }
        initAttempted = true;
        try {
            Class<?> containerClass = Class.forName(CONTAINER_CLASS);
            Class<?> subLevelClass = Class.forName(SUBLEVEL_CLASS);
            Class<?> poseClass = Class.forName(POSE_CLASS);
            Class<?> plotClass = Class.forName(PLOT_CLASS);
            Class<?> boundsClass = Class.forName(BOUNDS_CLASS);
            getContainerMethod = containerClass.getMethod("getContainer", ServerLevel.class);
            getContainerAnyMethod = containerClass.getMethod("getContainer", net.minecraft.world.level.Level.class);
            getAllSubLevelsMethod = containerClass.getMethod("getAllSubLevels");
            getSubLevelByUuidMethod = containerClass.getMethod("getSubLevel", UUID.class);
            getUniqueIdMethod = subLevelClass.getMethod("getUniqueId");
            logicalPoseMethod = subLevelClass.getMethod("logicalPose");
            getPlotMethod = subLevelClass.getMethod("getPlot");
            transformPositionMethod = poseClass.getMethod("transformPosition", Vec3.class);
            transformNormalMethod = poseClass.getMethod("transformNormal", Vec3.class);
            transformNormalInverseMethod = poseClass.getMethod("transformNormalInverse", Vec3.class);
            plotBoundingBoxMethod = plotClass.getMethod("getBoundingBox");
            boundsMinX = boundsClass.getMethod("minX");
            boundsMinY = boundsClass.getMethod("minY");
            boundsMinZ = boundsClass.getMethod("minZ");
            boundsMaxX = boundsClass.getMethod("maxX");
            boundsMaxY = boundsClass.getMethod("maxY");
            boundsMaxZ = boundsClass.getMethod("maxZ");
            initOk = true;
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.warn("SableStructureCompat: reflection init failed", e);
            initOk = false;
        }
        return initOk;
    }

    /** All active sub levels of {@code level}, or empty on any failure. */
    public static List<?> getSubLevels(ServerLevel level) {
        if (!isLoaded()) {
            return Collections.emptyList();
        }
        try {
            Object container = getContainerMethod.invoke(null, level);
            if (container == null) {
                return Collections.emptyList();
            }
            Object list = getAllSubLevelsMethod.invoke(container);
            return list instanceof List<?> l ? l : Collections.emptyList();
        } catch (ReflectiveOperationException e) {
            warnOnce("getSubLevels", e);
            return Collections.emptyList();
        }
    }

    /**
     * Client/side-agnostic: presentation yaw of the sub level whose plot bounds
     * contain {@code pos} (plot coordinates), or null. Used by the turret model
     * fix — sable rotates the RENDERING of entities aboard by the structure's
     * yaw, which SuperbWarfare's turret-relative-to-body math doesn't know about.
     */
    public static Float structureYawAt(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        if (!isLoaded()) {
            return null;
        }
        try {
            Object container = getContainerAnyMethod.invoke(null, level);
            if (container == null) {
                return null;
            }
            Object list = getAllSubLevelsMethod.invoke(container);
            if (!(list instanceof List<?> subLevels)) {
                return null;
            }
            for (Object subLevel : subLevels) {
                int[] b = getPlotBounds(subLevel);
                if (b != null && pos.getX() >= b[0] && pos.getX() <= b[3]
                        && pos.getY() >= b[1] && pos.getY() <= b[4]
                        && pos.getZ() >= b[2] && pos.getZ() <= b[5]) {
                    // On the client prefer the frame-interpolated render pose so the
                    // correction matches the rotation sable applied to this exact frame.
                    Float renderYaw = renderPoseYaw(subLevel);
                    return renderYaw != null ? renderYaw : structureYaw(subLevel);
                }
            }
            return null;
        } catch (ReflectiveOperationException e) {
            warnOnce("structureYawAt", e);
            return null;
        }
    }

    /** Server-side: the sub level whose plot bounds contain {@code pos} (plot coords), or null. */
    public static Object subLevelAt(ServerLevel level, net.minecraft.core.BlockPos pos) {
        for (Object subLevel : getSubLevels(level)) {
            int[] b = getPlotBounds(subLevel);
            if (b != null && pos.getX() >= b[0] && pos.getX() <= b[3]
                    && pos.getY() >= b[1] && pos.getY() <= b[4]
                    && pos.getZ() >= b[2] && pos.getZ() <= b[5]) {
                return subLevel;
            }
        }
        return null;
    }

    public static Object getSubLevel(ServerLevel level, UUID id) {
        if (!isLoaded()) {
            return null;
        }
        try {
            Object container = getContainerMethod.invoke(null, level);
            return container == null ? null : getSubLevelByUuidMethod.invoke(container, id);
        } catch (ReflectiveOperationException e) {
            warnOnce("getSubLevel", e);
            return null;
        }
    }

    public static UUID getUniqueId(Object subLevel) {
        try {
            return (UUID) getUniqueIdMethod.invoke(subLevel);
        } catch (ReflectiveOperationException e) {
            warnOnce("getUniqueId", e);
            return null;
        }
    }

    /** Plot-space position -> world-space position via the sub level's pose. */
    public static Vec3 localToWorld(Object subLevel, Vec3 local) {
        try {
            Object pose = logicalPoseMethod.invoke(subLevel);
            return (Vec3) transformPositionMethod.invoke(pose, local);
        } catch (ReflectiveOperationException e) {
            warnOnce("localToWorld", e);
            return null;
        }
    }

    /**
     * Convert an entity's rotations between world frame and the sub level's
     * local frame — the exact conversion sable's LocalPlayerMixin performs for
     * players when they start/stop riding aboard, which plain entities never
     * get. Without it a seated vehicle keeps world-frame rotations while the
     * renderer adds the structure rotation on top: the whole model shows offset
     * by the structure yaw from where the entity (and its aim frame) really points.
     */
    public static boolean convertEntityRotation(Object subLevel, net.minecraft.world.entity.Entity entity,
                                                boolean worldToLocal) {
        try {
            Object pose = logicalPoseMethod.invoke(subLevel);
            Vec3 look = entity.getLookAngle();
            Vec3 converted = (Vec3) (worldToLocal
                    ? transformNormalInverseMethod.invoke(pose, look)
                    : transformNormalMethod.invoke(pose, look));
            entity.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.FEET,
                    entity.position().add(converted));
            return true;
        } catch (ReflectiveOperationException e) {
            warnOnce("convertEntityRotation", e);
            return false;
        }
    }

    /** World-space yaw (degrees, MC convention) of the structure's local +Z axis. */
    public static Float structureYaw(Object subLevel) {
        try {
            Object pose = logicalPoseMethod.invoke(subLevel);
            Vec3 forward = (Vec3) transformNormalMethod.invoke(pose, new Vec3(0.0D, 0.0D, 1.0D));
            return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        } catch (ReflectiveOperationException e) {
            warnOnce("structureYaw", e);
            return null;
        }
    }

    /** Yaw from the client-side interpolated render pose, or null (server sub levels). */
    private static Float renderPoseYaw(Object subLevel) {
        try {
            Method m = subLevel.getClass().getMethod("renderPose");
            Object pose = m.invoke(subLevel);
            Vec3 forward = (Vec3) transformNormalMethod.invoke(pose, new Vec3(0.0D, 0.0D, 1.0D));
            return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /** Plot-space block bounds {minX,minY,minZ,maxX,maxY,maxZ}, or null. */
    public static int[] getPlotBounds(Object subLevel) {
        try {
            Object plot = getPlotMethod.invoke(subLevel);
            if (plot == null) {
                return null;
            }
            Object bounds = plotBoundingBoxMethod.invoke(plot);
            if (bounds == null) {
                return null;
            }
            return new int[]{
                (int) boundsMinX.invoke(bounds), (int) boundsMinY.invoke(bounds), (int) boundsMinZ.invoke(bounds),
                (int) boundsMaxX.invoke(bounds), (int) boundsMaxY.invoke(bounds), (int) boundsMaxZ.invoke(bounds)
            };
        } catch (ReflectiveOperationException e) {
            warnOnce("getPlotBounds", e);
            return null;
        }
    }

    /** True if {@code state} is a Create seat block (the attach point marker). */
    public static boolean isSeatBlock(BlockState state) {
        initSeat();
        return createSeatBlockClass != null && createSeatBlockClass.isInstance(state.getBlock());
    }

    /** True if {@code entity} is Create's SeatEntity (spawned by sitDown). */
    public static boolean isSeatEntity(net.minecraft.world.entity.Entity entity) {
        initSeat();
        return createSeatEntityClass != null && createSeatEntityClass.isInstance(entity);
    }

    /** Create's occupancy check for a seat block position. */
    public static boolean isSeatOccupied(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        initSeat();
        if (seatOccupiedMethod == null) {
            return true;
        }
        try {
            return (boolean) seatOccupiedMethod.invoke(null, level, pos);
        } catch (ReflectiveOperationException e) {
            warnOnce("isSeatOccupied", e);
            return true;
        }
    }

    /**
     * Seat an entity through Create's native path — identical to a player
     * right-clicking the seat: spawns a SeatEntity at the block and mounts the
     * entity on it. On sable structures the pos is in plot space; sable's own
     * riding/presentation systems then carry the rider with the structure.
     */
    public static boolean sitDown(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                  net.minecraft.world.entity.Entity entity) {
        initSeat();
        if (sitDownMethod == null) {
            return false;
        }
        try {
            sitDownMethod.invoke(null, level, pos, entity);
            return entity.isPassenger();
        } catch (ReflectiveOperationException e) {
            warnOnce("sitDown", e);
            return false;
        }
    }

    private static synchronized void initSeat() {
        if (seatInitAttempted) {
            return;
        }
        seatInitAttempted = true;
        if (!ModList.get().isLoaded("create")) {
            return;
        }
        try {
            createSeatBlockClass = Class.forName(CREATE_SEAT_CLASS);
            createSeatEntityClass = Class.forName(CREATE_SEAT_ENTITY_CLASS);
            seatOccupiedMethod = createSeatBlockClass.getMethod("isSeatOccupied",
                    net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class);
            sitDownMethod = createSeatBlockClass.getMethod("sitDown",
                    net.minecraft.world.level.Level.class, net.minecraft.core.BlockPos.class,
                    net.minecraft.world.entity.Entity.class);
        } catch (ReflectiveOperationException e) {
            VehicleLoadMod.LOGGER.warn("SableStructureCompat: Create seat reflection init failed", e);
        }
    }

    private static boolean warned;

    private static void warnOnce(String where, Exception e) {
        if (!warned) {
            warned = true;
            VehicleLoadMod.LOGGER.warn("SableStructureCompat: reflection call failed at {} (further errors muted)", where, e);
        }
    }
}
