package com.ymrsl.vehicleload;

import com.ymrsl.vehicleload.compat.SableStructureCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Sable-structure counterpart of the Create-contraption seat attachment.
 *
 * Earlier attempts (proxy mounts, per-tick position pinning) fought
 * SuperbWarfare's own physics and muzzle math. This version does neither: it
 * only USHERS the vehicle into Create's native seat mechanism —
 * {@code SeatBlock.sitDown(level, plotPos, vehicle)}, the exact code path a
 * player triggers by right-clicking the seat. Create spawns its SeatEntity at
 * the seat block (plot space, where it is STATIC — the structure's motion is a
 * pose, not entity movement), the vehicle rides it, and sable's own
 * aboard-riding systems present and move the rider with the structure, same as
 * a seated player. After sitDown this mod does nothing per-tick at all.
 *
 * Detach: crowbar (see CrowbarInteractHandler) or breaking the seat (Create
 * handles that natively). Persistence is Create's own (riding NBT).
 */
public class SableSeatLockManager {
    private static final double SEAT_SCAN_RADIUS = 10.0D;
    private static final double BASE_ATTACH_RADIUS = 6.0D;
    private static final long SEAT_RESCAN_INTERVAL = 100L;
    private static final long ATTRACT_INTERVAL = 20L;
    /** Tag of the v2 proxy armor stands — cleaned up on sight. */
    private static final String LEGACY_PROXY_TAG = "vehicleload_sable_seat";

    private record SeatCacheEntry(long scannedAt, List<BlockPos> seats) {}

    /** Heading link of a seated vehicle: keep worldYaw = structYaw + offset every tick. */
    private record Yoke(UUID subLevelId, float yawOffset) {}

    /** Live reference to one seated vehicle, consumed by SableSeatPersistence. */
    public record SeatRef(UUID subLevelId, BlockPos seatPos, UUID vehicleId) {}

    private static SableSeatLockManager instance;

    private final Map<UUID, SeatCacheEntry> seatCacheBySubLevel = new HashMap<>();
    private final Map<UUID, Yoke> yokesByVehicle = new HashMap<>();
    /** Persisted seat records awaiting their structure to reappear after load. */
    private final List<SableSeatPersistence.Entry> pendingRestores = new ArrayList<>();
    private long pendingRestoresDeadline;
    /** Levels already initialized after load (ghost scan + restore kickoff). */
    private final java.util.Set<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>> rebootedLevels =
            new java.util.HashSet<>();

    public SableSeatLockManager() {
        instance = this;
    }

    /** Seated vehicles of {@code level}, resolved live from the yoke table. */
    public static List<SeatRef> currentSeats(ServerLevel level) {
        List<SeatRef> out = new ArrayList<>();
        SableSeatLockManager m = instance;
        if (m == null) {
            return out;
        }
        for (Map.Entry<UUID, Yoke> entry : m.yokesByVehicle.entrySet()) {
            Entity vehicle = level.getEntity(entry.getKey());
            if (vehicle == null || !vehicle.isAlive()) {
                continue;
            }
            Entity mount = vehicle.getVehicle();
            if (mount == null || !SableStructureCompat.isSeatEntity(mount)) {
                continue;
            }
            out.add(new SeatRef(entry.getValue().subLevelId(), mount.blockPosition(), entry.getKey()));
        }
        return out;
    }

    /** Crowbar hook: release the vehicle if it sits on a Create seat entity. */
    public static boolean unlock(Entity vehicle) {
        if (vehicle == null) {
            return false;
        }
        Entity mount = vehicle.getVehicle();
        if (mount != null && SableStructureCompat.isSeatEntity(mount)) {
            vehicle.stopRiding();
            return true;
        }
        // v2 leftovers: proxy armor stands.
        if (mount instanceof ArmorStand && mount.getTags().contains(LEGACY_PROXY_TAG)) {
            vehicle.stopRiding();
            mount.discard();
            return true;
        }
        return false;
    }

    public static boolean isLocked(Entity vehicle) {
        if (vehicle == null) {
            return false;
        }
        Entity mount = vehicle.getVehicle();
        return mount != null && (SableStructureCompat.isSeatEntity(mount)
                || (mount instanceof ArmorStand && mount.getTags().contains(LEGACY_PROXY_TAG)));
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!VehicleLoadConfig.SABLE_SEAT_ATTACH.get() || !SableStructureCompat.isLoaded()) {
            return;
        }
        tickYokes(level);
        if (level.getGameTime() % ATTRACT_INTERVAL == 0L) {
            attractVehicles(level);
        }
    }

    /**
     * Keep each seated vehicle's world-frame heading glued to the structure
     * (worldYaw = structYaw + boarding offset), every tick and AFTER the
     * vehicle's own tick — SW vehicles rewrite their own yRot, so a one-shot
     * conversion does not survive. Model rendering uses this world yaw directly
     * because SableEntityRotationExemptMixin stops sable from re-rotating it.
     */
    private void tickYokes(ServerLevel level) {
        if (yokesByVehicle.isEmpty()) {
            return;
        }
        var it = yokesByVehicle.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            Entity vehicle = level.getEntity(entry.getKey());
            if (vehicle == null) {
                continue;   // other dimension or unloaded; keep the yoke
            }
            Entity mount = vehicle.getVehicle();
            if (!vehicle.isAlive() || mount == null || !SableStructureCompat.isSeatEntity(mount)) {
                it.remove();
                continue;
            }
            Object subLevel = SableStructureCompat.subLevelAt(level, mount.blockPosition());
            if (subLevel == null) {
                continue;
            }
            Float structYaw = SableStructureCompat.structureYaw(subLevel);
            if (structYaw == null) {
                continue;
            }
            float yaw = net.minecraft.util.Mth.wrapDegrees(structYaw + entry.getValue().yawOffset());
            vehicle.setYRot(yaw);
            vehicle.setYHeadRot(yaw);
        }
    }

    private void attractVehicles(ServerLevel level) {
        boolean firstPassAfterLoad = rebootedLevels.add(level.dimension());
        if (firstPassAfterLoad) {
            pendingRestores.addAll(SableSeatPersistence.get(level).takeLoadedEntries());
            pendingRestoresDeadline = level.getGameTime() + 1200L;   // give structures 60s to reappear
            if (!pendingRestores.isEmpty()) {
                VehicleLoadMod.LOGGER.info("[GHOST] {} persisted seat record(s) queued for restore", pendingRestores.size());
            }
        }
        if (!pendingRestores.isEmpty()) {
            processPendingRestores(level);
        }
        for (Object subLevel : SableStructureCompat.getSubLevels(level)) {
            UUID id = SableStructureCompat.getUniqueId(subLevel);
            if (id == null) {
                continue;
            }
            if (firstPassAfterLoad) {
                ghostScan(level, subLevel, id);
            }
            for (BlockPos seat : getSeats(level, subLevel, id)) {
                if (SableStructureCompat.isSeatOccupied(level, seat)) {
                    adoptYoke(level, subLevel, id, seat);
                    continue;
                }
                Vec3 plotSeat = Vec3.atCenterOf(seat);
                Vec3 worldSeat = SableStructureCompat.localToWorld(subLevel, plotSeat);
                if (worldSeat == null) {
                    continue;
                }
                cleanupLegacyProxies(level, worldSeat);
                // Boarding from outside: candidates near the seat's rendered world
                // position. Already aboard (e.g. crowbar-dropped onto the deck, which
                // lives in plot space): candidates near the seat block itself.
                Entity vehicle = findNearestFreeVehicle(level, worldSeat);
                if (vehicle == null) {
                    vehicle = findNearestFreeVehicle(level, plotSeat);
                }
                if (vehicle == null) {
                    continue;
                }
                if (SableStructureCompat.sitDown(level, seat, vehicle)) {
                    Float structYaw = SableStructureCompat.structureYaw(subLevel);
                    float offset = structYaw == null ? 0.0F
                            : net.minecraft.util.Mth.wrapDegrees(vehicle.getYRot() - structYaw);
                    yokesByVehicle.put(vehicle.getUUID(), new Yoke(id, offset));
                    if (VehicleLoadConfig.DEBUG_LOG.get()) {
                        VehicleLoadMod.LOGGER.info(
                                "sable seat sitDown: vehicle={} subLevel={} seat={} world={} yawOffset={}",
                                vehicle.getId(), id, seat, worldSeat, offset);
                    }
                } else if (VehicleLoadConfig.DEBUG_LOG.get()) {
                    VehicleLoadMod.LOGGER.info("sable seat sitDown failed: vehicle={} seat={}",
                            vehicle.getId(), seat);
                }
            }
        }
    }

    /**
     * Recreate persisted seated vehicles once their structure is back. Sable's
     * plot serialization stores no entities, so the SeatEntity + nested vehicle
     * NBT never survive a reload on their own — SableSeatPersistence snapshots
     * them at save time and this pass rebuilds them through the live sitDown
     * path. Structures may restore lazily, so records are retried until the
     * deadline.
     */
    private void processPendingRestores(ServerLevel level) {
        var it = pendingRestores.iterator();
        while (it.hasNext()) {
            SableSeatPersistence.Entry entry = it.next();
            if (level.getEntity(entry.vehicleId()) != null) {
                VehicleLoadMod.LOGGER.info("[GHOST] restore skipped, vehicle {} already exists (adopt will yoke it)",
                        entry.vehicleId());
                it.remove();
                continue;
            }
            Object subLevel = SableStructureCompat.getSubLevel(level, entry.subLevelId());
            if (subLevel == null) {
                if (level.getGameTime() > pendingRestoresDeadline) {
                    VehicleLoadMod.LOGGER.warn("[GHOST] restore abandoned, structure {} never reappeared (vehicle {})",
                            entry.subLevelId(), entry.vehicleId());
                    it.remove();
                }
                continue;   // structure not restored yet, retry next pass
            }
            if (SableStructureCompat.isSeatOccupied(level, entry.seatPos())) {
                VehicleLoadMod.LOGGER.info("[GHOST] restore skipped, seat {} already occupied", entry.seatPos());
                it.remove();
                continue;
            }
            SableSeatPersistence.restore(level, subLevel, entry);
            it.remove();
        }
    }

    /**
     * Post-load diagnostics ([GHOST] marker): for every seat of every restored
     * structure, log occupancy, the block at the seat, and any entities in the
     * plot cell — answers "what is actually blocking placement / where did the
     * vehicle go" from a single latest.log.
     */
    private void ghostScan(ServerLevel level, Object subLevel, UUID id) {
        for (BlockPos seat : getSeats(level, subLevel, id)) {
            AABB box = new AABB(Vec3.atCenterOf(seat), Vec3.atCenterOf(seat)).inflate(2.0D);
            List<Entity> nearby = level.getEntitiesOfClass(Entity.class, box, e -> true);
            StringBuilder ent = new StringBuilder();
            for (Entity e : nearby) {
                ent.append(e.getType().toShortString()).append(e.isPassenger() ? "(riding)" : "").append(' ');
            }
            VehicleLoadMod.LOGGER.info("[GHOST] scan: structure={} seat={} occupied={} entitiesNearby=[{}]",
                    id, seat, SableStructureCompat.isSeatOccupied(level, seat),
                    ent.length() == 0 ? "none" : ent.toString().trim());
        }
    }

    /** A seated vehicle without a yoke (e.g. after reload): link its heading to the structure. */
    private void adoptYoke(ServerLevel level, Object subLevel, UUID id, BlockPos seat) {
        AABB box = new AABB(Vec3.atCenterOf(seat), Vec3.atCenterOf(seat)).inflate(2.0D);
        for (Entity vehicle : level.getEntitiesOfClass(Entity.class, box, VehicleCompat::isTargetVehicle)) {
            Entity mount = vehicle.getVehicle();
            if (mount == null || !SableStructureCompat.isSeatEntity(mount)
                    || yokesByVehicle.containsKey(vehicle.getUUID())) {
                continue;
            }
            Float structYaw = SableStructureCompat.structureYaw(subLevel);
            float offset = structYaw == null ? 0.0F
                    : net.minecraft.util.Mth.wrapDegrees(vehicle.getYRot() - structYaw);
            yokesByVehicle.put(vehicle.getUUID(), new Yoke(id, offset));
            if (VehicleLoadConfig.DEBUG_LOG.get()) {
                VehicleLoadMod.LOGGER.info("sable seat yoke adopt: vehicle={} seat={} yawOffset={}",
                        vehicle.getId(), seat, offset);
            }
        }
    }

    /** Remove v2's proxy armor stands left in the world (dismount, then discard). */
    private void cleanupLegacyProxies(ServerLevel level, Vec3 worldSeat) {
        AABB box = new AABB(worldSeat, worldSeat).inflate(4.0D);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, box,
                s -> s.getTags().contains(LEGACY_PROXY_TAG))) {
            stand.ejectPassengers();
            stand.discard();
        }
    }

    private Entity findNearestFreeVehicle(ServerLevel level, Vec3 seatPos) {
        AABB box = new AABB(seatPos, seatPos).inflate(SEAT_SCAN_RADIUS);
        Entity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        for (Entity vehicle : level.getEntitiesOfClass(Entity.class, box, VehicleCompat::isTargetVehicle)) {
            if (vehicle == null || !vehicle.isAlive() || vehicle.isPassenger()) {
                continue;
            }
            if (!vehicle.getPassengers().isEmpty()) {
                continue;
            }
            if (SeatCooldowns.isBlocked(vehicle, level.getGameTime())) {
                continue;
            }
            double radius = Math.max(BASE_ATTACH_RADIUS, Math.max(vehicle.getBbWidth(), vehicle.getBbHeight()));
            double dist = vehicle.position().distanceToSqr(seatPos);
            if (dist <= radius * radius && dist < nearestDist) {
                nearestDist = dist;
                nearest = vehicle;
            }
        }
        return nearest;
    }

    private List<BlockPos> getSeats(ServerLevel level, Object subLevel, UUID id) {
        long now = level.getGameTime();
        SeatCacheEntry cached = seatCacheBySubLevel.get(id);
        if (cached != null && now - cached.scannedAt() < SEAT_RESCAN_INTERVAL) {
            return cached.seats();
        }
        List<BlockPos> seats = new ArrayList<>();
        int[] b = SableStructureCompat.getPlotBounds(subLevel);
        if (b != null) {
            for (int cx = b[0] >> 4; cx <= b[3] >> 4; cx++) {
                for (int cz = b[2] >> 4; cz <= b[5] >> 4; cz++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) {
                        continue;
                    }
                    LevelChunkSection[] sections = chunk.getSections();
                    for (int i = 0; i < sections.length; i++) {
                        LevelChunkSection section = sections[i];
                        if (section == null || section.hasOnlyAir()
                                || !section.maybeHas(SableStructureCompat::isSeatBlock)) {
                            continue;
                        }
                        int baseY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
                        for (int x = 0; x < 16; x++) {
                            for (int y = 0; y < 16; y++) {
                                for (int z = 0; z < 16; z++) {
                                    if (SableStructureCompat.isSeatBlock(section.getBlockState(x, y, z))) {
                                        BlockPos pos = new BlockPos(
                                                SectionPos.sectionToBlockCoord(cx) + x, baseY + y,
                                                SectionPos.sectionToBlockCoord(cz) + z);
                                        if (pos.getX() >= b[0] && pos.getX() <= b[3]
                                                && pos.getY() >= b[1] && pos.getY() <= b[4]
                                                && pos.getZ() >= b[2] && pos.getZ() <= b[5]) {
                                            seats.add(pos);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        seatCacheBySubLevel.put(id, new SeatCacheEntry(now, seats));
        if (VehicleLoadConfig.DEBUG_LOG.get() && !seats.isEmpty()) {
            VehicleLoadMod.LOGGER.info("sable seat scan: subLevel={} seats={}", id, seats.size());
        }
        return seats;
    }
}
