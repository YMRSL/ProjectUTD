package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: 同步 MapVariables（零号区块）到客户端。
 * 上游负载是 SavedData 本体；payload 化后改为承载已序列化的 NBT，
 * 客户端处理（写入 MapVariables.clientSide）由 client 代理在 NetworkHandler 注册时补上。
 */
public record SavedDataSyncPacket(CompoundTag tag) implements CustomPacketPayload {

    public static final Type<SavedDataSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "saved_data_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SavedDataSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, SavedDataSyncPacket::tag,
            SavedDataSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
