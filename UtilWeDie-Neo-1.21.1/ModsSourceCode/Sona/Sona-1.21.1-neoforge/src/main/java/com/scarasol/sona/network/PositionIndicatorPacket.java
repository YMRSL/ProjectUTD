package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: 位置指示器（声音诱饵/暴露渲染）。客户端处理逻辑由 client 代理补上
 * （上游走 ClientRenderDispatcher.handlePositionIndicator）。
 */
public record PositionIndicatorPacket(double x, double y, double z, double renderRange, int duration) implements CustomPacketPayload {

    public static final Type<PositionIndicatorPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "position_indicator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PositionIndicatorPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, PositionIndicatorPacket::x,
            ByteBufCodecs.DOUBLE, PositionIndicatorPacket::y,
            ByteBufCodecs.DOUBLE, PositionIndicatorPacket::z,
            ByteBufCodecs.DOUBLE, PositionIndicatorPacket::renderRange,
            ByteBufCodecs.INT, PositionIndicatorPacket::duration,
            PositionIndicatorPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
