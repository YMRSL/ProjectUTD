package com.scarasol.zombiekit.network;

import com.scarasol.zombiekit.ZombieKitMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge payload registration (replaces the 1.20.1 SimpleChannel).
 *
 * Self-registers on the MOD bus via {@link EventBusSubscriber}; the main class no longer needs to call it.
 *
 * Server / network agent owns: all type registration + every handler. The S2C handlers here touch no client-only
 * classes directly ({@link SavedDataSyncPacket} writes NBT; {@link SyncBlockPacket} isolates its {@code Minecraft}
 * access behind a dist-gated nested class), so this single registration is safe on the dedicated server.
 */
@EventBusSubscriber(modid = ZombieKitMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        // C2S
        registrar.playToServer(KeyBindPacket.TYPE, KeyBindPacket.STREAM_CODEC, KeyBindPacket::handler);
        registrar.playToServer(CoverFirePacket.TYPE, CoverFirePacket.STREAM_CODEC, CoverFirePacket::handler);
        registrar.playToServer(MouseInputPacket.TYPE, MouseInputPacket.STREAM_CODEC, MouseInputPacket::handler);
        registrar.playToServer(DoubleJumpPacket.TYPE, DoubleJumpPacket.STREAM_CODEC, DoubleJumpPacket::handler);

        // S2C
        registrar.playToClient(SavedDataSyncPacket.TYPE, SavedDataSyncPacket.STREAM_CODEC, SavedDataSyncPacket::handler);

        // Bidirectional (radio block content sync, both C2S and S2C)
        registrar.playBidirectional(SyncBlockPacket.TYPE, SyncBlockPacket.STREAM_CODEC, SyncBlockPacket::handler);
    }
}
