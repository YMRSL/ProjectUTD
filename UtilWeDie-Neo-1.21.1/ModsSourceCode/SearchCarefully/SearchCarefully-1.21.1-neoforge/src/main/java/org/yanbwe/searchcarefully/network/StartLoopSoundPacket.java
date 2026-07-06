package org.yanbwe.searchcarefully.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import org.yanbwe.searchcarefully.SearchCarefully;
import org.yanbwe.searchcarefully.client.SearchSoundManager;

public class StartLoopSoundPacket implements CustomPacketPayload {

    public static final Type<StartLoopSoundPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(SearchCarefully.MODID, "start_loop_sound")
    );

    // MUST NOT use StreamCodec.unit(new StartLoopSoundPacket()): unit's encode asserts the
    // outgoing object is the SAME instance registered here (via equals == reference identity).
    // The sender creates a fresh StartLoopSoundPacket(), so unit throws
    // "Can't encode X, expected Y" and drops the connection. This is a no-field payload, so use
    // a real codec that writes nothing on encode and constructs a new instance on decode.
    public static final StreamCodec<RegistryFriendlyByteBuf, StartLoopSoundPacket> STREAM_CODEC =
        StreamCodec.of((buf, packet) -> {}, buf -> new StartLoopSoundPacket());

    public StartLoopSoundPacket() {
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                SearchSoundManager.startSearchLoopSound(mc.player);
            }
        });
    }

    @Override
    public Type<StartLoopSoundPacket> type() {
        return TYPE;
    }
}
