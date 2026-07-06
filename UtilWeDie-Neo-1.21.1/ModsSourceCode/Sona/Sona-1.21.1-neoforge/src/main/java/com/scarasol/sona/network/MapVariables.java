package com.scarasol.sona.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * @author Scarasol
 */
public class MapVariables extends SavedData {
    public static final String DATA_NAME = "sona_mapvars";
    private BlockPos zeroInfectionZone;
    public static MapVariables clientSide = new MapVariables();

    public static MapVariables load(CompoundTag tag) {
        MapVariables data = new MapVariables();
        data.read(tag);
        return data;
    }

    public BlockPos getZeroChunk() {
        return zeroInfectionZone;
    }

    public void setZeroChunk(BlockPos zeroInfectionZone) {
        this.zeroInfectionZone = zeroInfectionZone;
        this.setDirty();
    }

    public void read(CompoundTag nbt) {
        if (nbt.contains("SonaZeroInfectionZoneX")) {
            zeroInfectionZone = new BlockPos(nbt.getInt("SonaZeroInfectionZoneX"), nbt.getInt("SonaZeroInfectionZoneY"), nbt.getInt("SonaZeroInfectionZoneZ"));
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
        if (zeroInfectionZone != null) {
            nbt.putInt("SonaZeroInfectionZoneX", zeroInfectionZone.getX());
            nbt.putInt("SonaZeroInfectionZoneY", zeroInfectionZone.getY());
            nbt.putInt("SonaZeroInfectionZoneZ", zeroInfectionZone.getZ());
        }
        return nbt;
    }

    public void syncData(LevelAccessor world) {
        this.setDirty();
        if (world instanceof Level level && !world.isClientSide()) {
            PacketDistributor.sendToPlayersInDimension((ServerLevel) level, new SavedDataSyncPacket(save(new CompoundTag(), level.registryAccess())));
        }
    }

    public static MapVariables get(LevelAccessor world) {
        if (world instanceof ServerLevel serverLevelAcc) {
            return serverLevelAcc.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(MapVariables::new, (tag, provider) -> MapVariables.load(tag)),
                    DATA_NAME);
        } else {
            return clientSide;
        }
    }
}
