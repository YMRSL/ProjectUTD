package com.ymrsl.vehicleload;

import com.ymrsl.vehicleload.compat.SableStructureCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Own persistence for vehicles seated on sable structures.
 *
 * Sable's plot serialization (ServerLevelPlot.save) stores block states,
 * light, block entities and ticks — NO entities. A seated vehicle is nested
 * as passenger NBT inside Create's SeatEntity at plot coordinates, so on
 * world reload the whole chain (seat entity, vehicle, its inventory) simply
 * never comes back; sable's LoginPoint machinery re-anchors PLAYERS aboard,
 * plain entities get nothing. We therefore snapshot every seated vehicle
 * (full NBT, inventory included) into level SavedData on each world save,
 * and SableSeatLockManager recreates + reseats them after load.
 */
public class SableSeatPersistence extends SavedData {
    private static final String NAME = "vehicleload_sable_seats";

    public record Entry(UUID subLevelId, BlockPos seatPos, UUID vehicleId, CompoundTag vehicleNbt) {}

    /** Entries loaded from disk, consumed by the post-load restore pass. */
    private final List<Entry> loadedEntries = new ArrayList<>();
    /** Level this instance belongs to (transient, set on access). */
    private ServerLevel level;

    public static SableSeatPersistence get(ServerLevel level) {
        SableSeatPersistence data = level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(SableSeatPersistence::new, SableSeatPersistence::load, null), NAME);
        data.level = level;
        return data;
    }

    public List<Entry> takeLoadedEntries() {
        List<Entry> out = new ArrayList<>(loadedEntries);
        loadedEntries.clear();
        return out;
    }

    private static SableSeatPersistence load(CompoundTag tag, HolderLookup.Provider registries) {
        SableSeatPersistence data = new SableSeatPersistence();
        ListTag list = tag.getList("seats", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            data.loadedEntries.add(new Entry(
                    e.getUUID("subLevel"),
                    NbtUtils.readBlockPos(e, "seat").orElse(BlockPos.ZERO),
                    e.getUUID("vehicleId"),
                    e.getCompound("vehicle")));
        }
        VehicleLoadMod.LOGGER.info("[GHOST] persistence loaded: {} seated vehicle record(s)", data.loadedEntries.size());
        return data;
    }

    /**
     * Snapshot the CURRENT seated vehicles at save time. Always dirty, so every
     * world save (autosave included) refreshes the records — crowbar'd or dead
     * vehicles fall out naturally on the next save.
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        int count = 0;
        if (level != null) {
            for (SableSeatLockManager.SeatRef ref : SableSeatLockManager.currentSeats(level)) {
                Entity vehicle = level.getEntity(ref.vehicleId());
                if (vehicle == null || !vehicle.isAlive()) {
                    continue;
                }
                CompoundTag vehicleNbt = new CompoundTag();
                if (!vehicle.save(vehicleNbt)) {
                    VehicleLoadMod.LOGGER.warn("[GHOST] persistence: vehicle {} refused to save, skipped", ref.vehicleId());
                    continue;
                }
                // Strip the riding context: the vehicle is recreated standalone and
                // re-seated through the live sitDown path on load.
                vehicleNbt.remove("Pos");
                CompoundTag e = new CompoundTag();
                e.putUUID("subLevel", ref.subLevelId());
                e.put("seat", NbtUtils.writeBlockPos(ref.seatPos()));
                e.putUUID("vehicleId", ref.vehicleId());
                e.put("vehicle", vehicleNbt);
                list.add(e);
                count++;
            }
        }
        tag.put("seats", list);
        if (count > 0) {
            VehicleLoadMod.LOGGER.info("[GHOST] persistence saved: {} seated vehicle(s)", count);
        }
        return tag;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    /** Recreate one persisted vehicle at its seat. Returns true when restored. */
    public static boolean restore(ServerLevel level, Object subLevel, Entry entry) {
        CompoundTag nbt = entry.vehicleNbt().copy();
        net.minecraft.world.phys.Vec3 world = SableStructureCompat.localToWorld(
                subLevel, net.minecraft.world.phys.Vec3.atCenterOf(entry.seatPos()).add(0, 0.5, 0));
        if (world == null) {
            return false;
        }
        Entity vehicle = net.minecraft.world.entity.EntityType.loadEntityRecursive(nbt, level, e -> {
            e.setPos(world.x, world.y, world.z);
            return e;
        });
        if (vehicle == null) {
            VehicleLoadMod.LOGGER.warn("[GHOST] restore: could not deserialize vehicle {} ({})",
                    entry.vehicleId(), entry.vehicleNbt().getString("id"));
            return false;
        }
        if (!level.addFreshEntity(vehicle)) {
            VehicleLoadMod.LOGGER.warn("[GHOST] restore: addFreshEntity refused for {}", entry.vehicleId());
            return false;
        }
        boolean seated = SableStructureCompat.sitDown(level, entry.seatPos(), vehicle);
        VehicleLoadMod.LOGGER.info("[GHOST] restore: vehicle {} ({}) recreated at {} seated={}",
                entry.vehicleId(), vehicle.getType(), entry.seatPos(), seated);
        return true;
    }
}
