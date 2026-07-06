package com.scarasol.tud.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * @author Scarasol
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playBidirectional(SwitchAmmoPacket.TYPE, SwitchAmmoPacket.STREAM_CODEC, SwitchAmmoPacket::handle);
    }
}
