package com.scarasol.zombiekit.network;

import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client sync of the {@link MapVariables} saved data.
 * The payload carries the raw NBT; the client rebuilds its {@link MapVariables#clientSide} from it.
 */
public record SavedDataSyncPacket(CompoundTag data) implements CustomPacketPayload {

    public static final Type<SavedDataSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "saved_data_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SavedDataSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, SavedDataSyncPacket::data,
            SavedDataSyncPacket::new
    );

    public SavedDataSyncPacket(MapVariables mapVariables) {
        this(mapVariables.save(new CompoundTag()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(SavedDataSyncPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            MapVariables mapVariables = new MapVariables();
            mapVariables.read(message.data());
            MapVariables.clientSide = mapVariables;
        });
    }
}
