package net.tkg.ModernMayhem.server.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tkg.ModernMayhem.server.network.NVGAutoGainTogglePacket;
import net.tkg.ModernMayhem.server.network.NVGCotiTogglePacket;
import net.tkg.ModernMayhem.server.network.NVGIrTogglePacket;
import net.tkg.ModernMayhem.server.network.NVGSyncSwitchOffPacket;
import net.tkg.ModernMayhem.server.network.NVGSyncSwitchOnPacket;
import net.tkg.ModernMayhem.server.network.NVGTubeGainDownPacket;
import net.tkg.ModernMayhem.server.network.NVGTubeGainUpPacket;
import net.tkg.ModernMayhem.server.network.OpenBackpackKeyPacket;
import net.tkg.ModernMayhem.server.network.OpenRigKeyPacket;
import net.tkg.ModernMayhem.server.network.SwitchNVGStatusPacket;

public class PacketsRegistryMM {
    public static final String NETWORK_VERSION = "1";

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(PacketsRegistryMM::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(SwitchNVGStatusPacket.TYPE, SwitchNVGStatusPacket.STREAM_CODEC, SwitchNVGStatusPacket::handle);
        registrar.playToServer(NVGTubeGainUpPacket.TYPE, NVGTubeGainUpPacket.STREAM_CODEC, NVGTubeGainUpPacket::handle);
        registrar.playToServer(NVGTubeGainDownPacket.TYPE, NVGTubeGainDownPacket.STREAM_CODEC, NVGTubeGainDownPacket::handle);
        registrar.playToServer(OpenBackpackKeyPacket.TYPE, OpenBackpackKeyPacket.STREAM_CODEC, OpenBackpackKeyPacket::handle);
        registrar.playToServer(OpenRigKeyPacket.TYPE, OpenRigKeyPacket.STREAM_CODEC, OpenRigKeyPacket::handle);
        registrar.playToServer(NVGSyncSwitchOnPacket.TYPE, NVGSyncSwitchOnPacket.STREAM_CODEC, NVGSyncSwitchOnPacket::handle);
        registrar.playToServer(NVGSyncSwitchOffPacket.TYPE, NVGSyncSwitchOffPacket.STREAM_CODEC, NVGSyncSwitchOffPacket::handle);
        registrar.playToServer(NVGAutoGainTogglePacket.TYPE, NVGAutoGainTogglePacket.STREAM_CODEC, NVGAutoGainTogglePacket::handle);
        registrar.playToServer(NVGCotiTogglePacket.TYPE, NVGCotiTogglePacket.STREAM_CODEC, NVGCotiTogglePacket::handle);
        registrar.playToServer(NVGIrTogglePacket.TYPE, NVGIrTogglePacket.STREAM_CODEC, NVGIrTogglePacket::handle);
    }
}
