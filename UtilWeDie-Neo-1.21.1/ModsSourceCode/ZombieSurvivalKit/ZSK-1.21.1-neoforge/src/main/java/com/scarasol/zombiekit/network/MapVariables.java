package com.scarasol.zombiekit.network;

import com.google.common.collect.Maps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;

/**
 * @author Scarasol
 */
public class MapVariables extends SavedData {
    public static final String DATA_NAME = "zombiekit_mapvars";
    public String radioLocation = "";
    public Map<UUID, ChunkPos> mortarPos = Maps.newHashMap();
    static MapVariables clientSide = new MapVariables();

    /**
     * 1.21 SavedData factory: constructor + (tag, registries) deserializer.
     */
    public static final SavedData.Factory<MapVariables> FACTORY =
            new SavedData.Factory<>(MapVariables::new, (tag, registries) -> MapVariables.load(tag));

    public static MapVariables load(CompoundTag tag) {
        MapVariables data = new MapVariables();
        data.read(tag);
        return data;
    }

    public void read(CompoundTag nbt) {
        radioLocation = nbt.getString("radio_location");
        if (nbt.contains("MortarPos")) {
            mortarPos.clear();
            ListTag contentList = nbt.getList("MortarPos", 8);
            for (Tag tag : contentList) {
                String[] info = tag.getAsString().split("; ");
                if (info.length > 1) {
                    UUID uuid = UUID.fromString(info[0]);
                    String[] chunkStr = info[1].split(", ");
                    if (chunkStr.length > 1) {
                        ChunkPos chunkPos = new ChunkPos(Integer.parseInt(chunkStr[0]), Integer.parseInt(chunkStr[1]));
                        mortarPos.put(uuid, chunkPos);
                    }

                }
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        return save(nbt);
    }

    public CompoundTag save(CompoundTag nbt) {
        nbt.putString("radio_location", radioLocation);
        if (!mortarPos.isEmpty()) {
            ListTag contentList = new ListTag();
            for (Map.Entry<UUID, ChunkPos> mortar : mortarPos.entrySet()) {
                ChunkPos chunkPos = mortar.getValue();
                contentList.add(StringTag.valueOf(mortar.getKey() + "; " + chunkPos.x + ", " + chunkPos.z));
            }
            nbt.put("MortarPos", contentList);
        }
        return nbt;
    }

    public void syncData(LevelAccessor world) {
        this.setDirty();
        if (world instanceof ServerLevel serverLevel) {
            PacketDistributor.sendToPlayersInDimension(serverLevel, new SavedDataSyncPacket(this));
        }
    }

    public static MapVariables get(LevelAccessor world) {
        if (world instanceof ServerLevel serverLevelAcc) {
            return serverLevelAcc.getServer().getLevel(serverLevelAcc.dimension()).getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        } else {
            return clientSide;
        }
    }

}
