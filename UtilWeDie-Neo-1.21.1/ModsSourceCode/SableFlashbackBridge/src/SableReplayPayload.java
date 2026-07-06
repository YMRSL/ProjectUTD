package com.utdpatch.doomsday.compat;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Carrier payload for the sable x Flashback bridge.
 *
 * Carries one fully serialized clientbound game packet (game protocol codec
 * bytes). Flashback's replay server forwards ALL custom payloads raw to the
 * viewer client, but interprets vanilla packets (chunks!) into its own server
 * world where plot chunks are beyond view distance and never re-streamed —
 * so the bridge wraps every packet of a structure's full sync in this carrier
 * and the client side ({@link SableReplayClientHandler}) unwraps, decodes and
 * dispatches them locally.
 */
public record SableReplayPayload(byte[] data) implements CustomPacketPayload {
    public static final Type<SableReplayPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("utd_doomsday_patch", "sable_replay"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, SableReplayPayload> STREAM_CODEC =
            ByteBufCodecs.byteArray(Integer.MAX_VALUE).map(SableReplayPayload::new, SableReplayPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
