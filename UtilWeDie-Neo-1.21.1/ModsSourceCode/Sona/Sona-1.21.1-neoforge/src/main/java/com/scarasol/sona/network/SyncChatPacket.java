package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: 同步聊天范围/限制设置到客户端。客户端处理逻辑由 client 代理补上。
 */
public record SyncChatPacket(String chatIncreaseItem, int chatRange, boolean chatLimit) implements CustomPacketPayload {

    public static final Type<SyncChatPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "sync_chat"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncChatPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncChatPacket::chatIncreaseItem,
            ByteBufCodecs.INT, SyncChatPacket::chatRange,
            ByteBufCodecs.BOOL, SyncChatPacket::chatLimit,
            SyncChatPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
