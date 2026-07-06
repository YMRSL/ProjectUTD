package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: 同步声音白名单与开关到客户端。客户端处理逻辑由 client 代理补上。
 */
public record SyncSoundPacket(String soundWhiteList, boolean soundOpen) implements CustomPacketPayload {

    public static final Type<SyncSoundPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "sync_sound"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSoundPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SyncSoundPacket::soundWhiteList,
            ByteBufCodecs.BOOL, SyncSoundPacket::soundOpen,
            SyncSoundPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
