package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import com.scarasol.sona.manager.SoundManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: 客户端请求服务端生成声音诱饵。
 */
public record SoundDecoyPacket(double x, double y, double z, int amplifier) implements CustomPacketPayload {

    public static final Type<SoundDecoyPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "sound_decoy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundDecoyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, SoundDecoyPacket::x,
            ByteBufCodecs.DOUBLE, SoundDecoyPacket::y,
            ByteBufCodecs.DOUBLE, SoundDecoyPacket::z,
            ByteBufCodecs.INT, SoundDecoyPacket::amplifier,
            SoundDecoyPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoundDecoyPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                SoundManager.spawnSoundDecoy(player.level(), msg.x(), msg.y(), msg.z(), msg.amplifier());
            }
        });
    }
}
