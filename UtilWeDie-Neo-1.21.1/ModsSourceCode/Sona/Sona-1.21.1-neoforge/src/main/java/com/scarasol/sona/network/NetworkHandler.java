package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge payload 注册（替换 1.20.1 的 SimpleChannel）。
 *
 * 服务端代理负责：所有 payload 的类型注册 + C2S 处理（SoundDecoy/Lock）。
 * S2C 客户端显示处理（ChunkDataSync 刷渲染、Rot 写槽位、SyncSound/SyncChat 写客户端状态、
 * SavedDataSync 写 MapVariables.clientSide、PositionIndicator 渲染）由 client 代理实现：
 * 在 {@code com.scarasol.sona.client.network.SonaClientPayloadHandler} 提供静态 handle 方法，
 * 并在 client 侧的 RegisterPayloadHandlersEvent（或本类经 FMLEnvironment 判断）里绑定 playToClient。
 *
 * 为避免与 client 代理重复注册冲突：本类只注册类型与 C2S。client 代理用单独的
 * {@code registrar.playToClient(...)} 在其自己的 @EventBusSubscriber 里补 S2C handler，
 * 或直接编辑本方法把下面 TODO 行替换为 SonaClientPayloadHandler 调用。
 */
@EventBusSubscriber(modid = SonaMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class NetworkHandler {

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // C2S（服务端处理，本代理所有）
        registrar.playToServer(SoundDecoyPacket.TYPE, SoundDecoyPacket.STREAM_CODEC, SoundDecoyPacket::handle);
        registrar.playToServer(LockPacket.TYPE, LockPacket.STREAM_CODEC, LockPacket::handle);

        // S2C（客户端显示处理由 client 代理补；此处仅声明 codec 以打通协议）。
        // client 代理应将下面这些替换为 playToClient(TYPE, STREAM_CODEC, SonaClientPayloadHandler::handleXxx)。
        registrar.playToClient(ChunkDataSyncPacket.TYPE, ChunkDataSyncPacket.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(RotPacket.TYPE, RotPacket.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(SyncSoundPacket.TYPE, SyncSoundPacket.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(SyncChatPacket.TYPE, SyncChatPacket.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(SavedDataSyncPacket.TYPE, SavedDataSyncPacket.STREAM_CODEC, (payload, context) -> {});
        registrar.playToClient(PositionIndicatorPacket.TYPE, PositionIndicatorPacket.STREAM_CODEC, (payload, context) -> {});
    }
}
