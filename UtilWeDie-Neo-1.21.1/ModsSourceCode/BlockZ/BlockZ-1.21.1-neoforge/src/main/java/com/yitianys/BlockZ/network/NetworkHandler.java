package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 网络层注册（NeoForge CustomPacketPayload 体系）。
 *
 * <p>监听 modBus 上的 {@link RegisterPayloadHandlersEvent}，
 * 用 {@link PayloadRegistrar} 注册全部 KEEP 包：
 * C2S 用 {@code playToServer}，S2C 用 {@code playToClient}。
 *
 * <p>无需主类显式调用 {@code init()}——本类用 {@code @EventBusSubscriber(bus = MOD)}
 * 自动订阅 modBus。若主类更倾向显式注册，可改为
 * {@code modBus.addListener(NetworkHandler::register)} 并去掉注解。
 */
@EventBusSubscriber(modid = BlockZ.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkHandler {
    public static final String PROTOCOL = "1";

    private NetworkHandler() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        // ---- C2S ----
        registrar.playToServer(OpenDayZMenuC2S.TYPE, OpenDayZMenuC2S.STREAM_CODEC, OpenDayZMenuC2S::handle);
        registrar.playToServer(OpenDayZContainerC2S.TYPE, OpenDayZContainerC2S.STREAM_CODEC, OpenDayZContainerC2S::handle);
        registrar.playToServer(RotateItemC2S.TYPE, RotateItemC2S.STREAM_CODEC, RotateItemC2S::handle);
        registrar.playToServer(LootPickupC2S.TYPE, LootPickupC2S.STREAM_CODEC, LootPickupC2S::handle);
        registrar.playToServer(DayzToggleRequestC2S.TYPE, DayzToggleRequestC2S.STREAM_CODEC, DayzToggleRequestC2S::handle);
        registrar.playToServer(RequestSwitchToDayZMenuC2S.TYPE, RequestSwitchToDayZMenuC2S.STREAM_CODEC, RequestSwitchToDayZMenuC2S::handle);

        // ---- S2C ----
        registrar.playToClient(SyncBackpackS2C.TYPE, SyncBackpackS2C.STREAM_CODEC, SyncBackpackS2C::handle);
        registrar.playToClient(DayzToggleStateS2C.TYPE, DayzToggleStateS2C.STREAM_CODEC, DayzToggleStateS2C::handle);
        registrar.playToClient(DayzTogglePermissionS2C.TYPE, DayzTogglePermissionS2C.STREAM_CODEC, DayzTogglePermissionS2C::handle);
        registrar.playToClient(SyncGridRulesS2C.TYPE, SyncGridRulesS2C.STREAM_CODEC, SyncGridRulesS2C::handle);
        registrar.playToClient(SyncConfigS2C.TYPE, SyncConfigS2C.STREAM_CODEC, SyncConfigS2C::handle);
        registrar.playToClient(PacketReloadConfigS2C.TYPE, PacketReloadConfigS2C.STREAM_CODEC, PacketReloadConfigS2C::handle);
    }
}
