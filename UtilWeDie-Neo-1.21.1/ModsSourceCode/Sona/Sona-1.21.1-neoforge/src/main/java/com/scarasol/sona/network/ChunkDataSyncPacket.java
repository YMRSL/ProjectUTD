package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: 区块感染数据同步。客户端处理逻辑由 client 代理在 NetworkHandler 注册时补上
 * （刷区块渲染脏标记，见上游 client 侧）。本 record 仅定义负载与编解码。
 */
public record ChunkDataSyncPacket(int x, int z, CompoundTag tag) implements CustomPacketPayload {

    public static final Type<ChunkDataSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "chunk_data_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkDataSyncPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ChunkDataSyncPacket::x,
            ByteBufCodecs.INT, ChunkDataSyncPacket::z,
            ByteBufCodecs.TRUSTED_COMPOUND_TAG, ChunkDataSyncPacket::tag,
            ChunkDataSyncPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
