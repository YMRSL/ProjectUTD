package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C: 同步单个槽位食物的腐烂值。客户端处理逻辑由 client 代理补上。
 */
public record RotPacket(double rotValue, int slotInt, boolean isInventory) implements CustomPacketPayload {

    public static final Type<RotPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "rot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RotPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, RotPacket::rotValue,
            ByteBufCodecs.INT, RotPacket::slotInt,
            ByteBufCodecs.BOOL, RotPacket::isInventory,
            RotPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
