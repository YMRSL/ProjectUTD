package com.ymrsl.vehicleload;

import com.ymrsl.vehicleload.compat.CreateContraptionCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import com.ymrsl.vehicleload.VehicleLoadMod;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AutoSeatHandler {
    private static final double BASE_ATTACH_RADIUS = 6.0D;
    private static final double SEAT_SCAN_RADIUS = 10.0D;
    private static final boolean DEBUG_ATTACH = false;

    private final Set<Entity> trackedContraptions = Collections.newSetFromMap(new WeakHashMap<>());
    private long debugTickCounter = 0L;
    private final Map<Level, Long> lastScanByLevel = new WeakHashMap<>();

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        if (CreateContraptionCompat.isContraptionEntity(entity)) {
            trackedContraptions.add(entity);
        }
    }

    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Level level = event.level;
        if (level.isClientSide) {
            return;
        }
        if (!CreateContraptionCompat.isLoaded()) {
            return;
        }
        pruneEntities(level);
        refreshContraptions(level);
        if (DEBUG_ATTACH) {
            debugTickCounter++;
            if (debugTickCounter % 200L == 0L) {
                VehicleLoadMod.LOGGER.info(
                    "vehicleload tick debug: level={} trackedContraptions={}",
                    level.dimension().location(),
                    trackedContraptions.size()
                );
            }
        }
        tickContraptions(level);
    }

    private void pruneEntities(Level level) {
        Iterator<Entity> contraptionIterator = trackedContraptions.iterator();
        while (contraptionIterator.hasNext()) {
            Entity contraption = contraptionIterator.next();
            if (contraption == null || !contraption.isAlive() || contraption.level() != level) {
                contraptionIterator.remove();
            }
        }
    }

    private void tickContraptions(Level level) {
        if (trackedContraptions.isEmpty()) {
            return;
        }
        Iterator<Entity> iterator = trackedContraptions.iterator();
        while (iterator.hasNext()) {
            Entity contraption = iterator.next();
            if (contraption == null || !contraption.isAlive() || contraption.level() != level) {
                iterator.remove();
                continue;
            }
            List<BlockPos> seats = CreateContraptionCompat.getSeatPositions(contraption);
            if (seats.isEmpty()) {
                continue;
            }
            Map<UUID, Integer> seatMapping = CreateContraptionCompat.getSeatMapping(contraption);
            syncMountedVehicles(level, contraption, seatMapping);
            Set<Integer> occupied = new HashSet<>(seatMapping.values());
            for (int i = 0; i < seats.size(); i++) {
                if (occupied.contains(i)) {
                    continue;
                }
                Vec3 worldSeat = getSeatWorldPos(contraption, seats.get(i));
                if (worldSeat == null) {
                    continue;
                }
                AABB seatBox = new AABB(worldSeat, worldSeat).inflate(SEAT_SCAN_RADIUS);
                List<Entity> candidates = level.getEntitiesOfClass(
                    Entity.class,
                    seatBox,
                    VehicleCompat::isTargetVehicle
                );
                Entity nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Entity vehicle : candidates) {
                    if (vehicle == null || !vehicle.isAlive() || vehicle.isPassenger()) {
                        continue;
                    }
                    if (!vehicle.getPassengers().isEmpty()) {
                        continue;
                    }
                    if (SeatCooldowns.isBlocked(vehicle, level.getGameTime())) {
                        continue;
                    }
                    double radius = getAttachRadius(vehicle);
                    double dist = vehicle.position().distanceToSqr(worldSeat);
                    if (dist > radius * radius) {
                        continue;
                    }
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = vehicle;
                    }
                }
                if (nearest == null) {
                    continue;
                }
                if (DEBUG_ATTACH) {
                    VehicleLoadMod.LOGGER.info(
                        "Seat attach candidate: contraption={} seatIndex={} vehicle={} dist={}",
                        contraption.getId(),
                        i,
                        nearest.getId(),
                        Math.sqrt(nearestDist)
                    );
                }
                if (CreateContraptionCompat.addSittingPassenger(contraption, nearest, i)) {
                    occupied.add(i);
                    syncVehicleRotation(nearest, contraption);
                    if (DEBUG_ATTACH) {
                        Entity mount = nearest.getVehicle();
                        VehicleLoadMod.LOGGER.info(
                            "Seat attach success: vehicle={} mount={} isPassenger={}",
                            nearest.getId(),
                            mount != null ? mount.getId() : null,
                            nearest.isPassenger()
                        );
                    }
                } else if (DEBUG_ATTACH) {
                    Entity mount = nearest.getVehicle();
                    VehicleLoadMod.LOGGER.info(
                        "Seat attach failed: vehicle={} mount={} isPassenger={}",
                        nearest.getId(),
                        mount != null ? mount.getId() : null,
                        nearest.isPassenger()
                    );
                }
            }
        }
    }

    private void refreshContraptions(Level level) {
        if (!Level.OVERWORLD.equals(level.dimension())) {
            return;
        }
        long gameTime = level.getGameTime();
        Long lastScan = lastScanByLevel.get(level);
        if (lastScan != null && lastScan == gameTime) {
            return;
        }
        if (gameTime % 20L != 0L) {
            return;
        }
        lastScanByLevel.put(level, gameTime);
        trackedContraptions.clear();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        for (Entity entity : serverLevel.getAllEntities()) {
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            if (CreateContraptionCompat.isContraptionEntity(entity)) {
                trackedContraptions.add(entity);
            }
        }
        if (DEBUG_ATTACH) {
            VehicleLoadMod.LOGGER.info(
                "vehicleload scan tick: level={} trackedContraptions={}",
                level.dimension().location(),
                trackedContraptions.size()
            );
        }
    }

    private double getAttachRadius(Entity vehicle) {
        double size = Math.max(vehicle.getBbWidth(), vehicle.getBbHeight());
        return Math.max(BASE_ATTACH_RADIUS, size);
    }

    private Vec3 getSeatWorldPos(Entity contraption, BlockPos seatPos) {
        Vec3 localSeat = Vec3.atLowerCornerOf(seatPos).add(0.5D, 0.5D, 0.5D);
        return CreateContraptionCompat.toGlobalVector(contraption, localSeat);
    }

    private void syncVehicleRotation(Entity vehicle, Entity contraption) {
        if (vehicle == null || contraption == null) {
            return;
        }
        Float contraptionYaw = CreateContraptionCompat.getContraptionYaw(contraption, 1.0f);
        Float contraptionPitch = CreateContraptionCompat.getContraptionPitch(contraption, 1.0f);
        if (contraptionYaw != null) {
            float facing = Mth.wrapDegrees(-contraptionYaw - 90.0f);
            vehicle.setYRot(facing);
        }
        if (contraptionPitch != null) {
            vehicle.setXRot(contraptionPitch);
        }
    }

    private void syncMountedVehicles(Level level, Entity contraption, Map<UUID, Integer> seatMapping) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (UUID id : seatMapping.keySet()) {
            Entity entity = serverLevel.getEntity(id);
            if (entity == null || !VehicleCompat.isTargetVehicle(entity)) {
                continue;
            }
            if (entity.getVehicle() != contraption) {
                continue;
            }
            syncVehicleRotation(entity, contraption);
        }
    }

    public void dumpDebug(CommandSourceStack source) {
        if (!(source.getLevel() instanceof ServerLevel)) {
            source.sendFailure(Component.literal("vehicleload: server level not available."));
            return;
        }
        ServerLevel level = (ServerLevel) source.getLevel();
        VehicleLoadMod.LOGGER.info("vehicleload debug summary: {}", CreateContraptionCompat.debugSummary());
        VehicleLoadMod.LOGGER.info(
            "vehicleload debug tracked contraptions: count={}",
            trackedContraptions.size()
        );
        for (Entity contraption : trackedContraptions) {
            if (contraption == null || !contraption.isAlive() || contraption.level() != level) {
                continue;
            }
            List<BlockPos> seats = CreateContraptionCompat.getSeatPositions(contraption);
            Map<UUID, Integer> mapping = CreateContraptionCompat.getSeatMapping(contraption);
            VehicleLoadMod.LOGGER.info(
                "vehicleload debug tracked contraption: id={} type={} seats={} mapping={}",
                contraption.getId(),
                contraption.getType().toString(),
                seats.size(),
                mapping.size()
            );
        }

        for (ServerPlayer player : level.players()) {
            Vec3 center = player.position();
            AABB scanBox = new AABB(center, center).inflate(128.0D);
            int contraptionCount = level.getEntitiesOfClass(
                Entity.class,
                scanBox,
                CreateContraptionCompat::isContraptionEntity
            ).size();
            int vehicleCount = level.getEntitiesOfClass(
                Entity.class,
                scanBox,
                VehicleCompat::isTargetVehicle
            ).size();
            VehicleLoadMod.LOGGER.info(
                "vehicleload debug player scan: player={} pos={} contraptions={} vehicles={}",
                player.getGameProfile().getName(),
                formatVec(center),
                contraptionCount,
                vehicleCount
            );
        }
        source.sendSuccess(
            () -> Component.literal("vehicleload debug dumped to latest.log"),
            false
        );
    }

    private String formatVec(Vec3 vec) {
        return String.format("%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
    }
}
