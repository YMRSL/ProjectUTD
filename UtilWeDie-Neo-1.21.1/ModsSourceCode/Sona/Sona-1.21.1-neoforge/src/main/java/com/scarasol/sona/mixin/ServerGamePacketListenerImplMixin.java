package com.scarasol.sona.mixin;

import com.scarasol.sona.manager.ChatManager;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerPlayerConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Routes player chat broadcast through {@link ChatManager} for the proximity/voice-range
 * ("循声"/sound) chat-distance feature.
 *
 * NeoForge 1.21.1 STRUCTURAL REWRITE (needs compile-time verification):
 * The 1.20.1 mixin targeted {@code handleChat} and the old
 * {@code tryHandleChat(String,Instant,Update):Optional} return + a {@code @ModifyVariable} on the
 * captured {@code Optional<LastSeenMessages>}. In 1.21.1 {@code tryHandleChat} is
 * {@code (String, Runnable):void} and the broadcast is funnelled through the new
 * {@code broadcastChatMessage(PlayerChatMessage)} method (which internally binds {@code ChatType.CHAT}
 * and fires NeoForge's ServerChatEvent). We therefore inject at the HEAD of
 * {@code broadcastChatMessage} and hand off to {@link ChatManager}, cancelling the vanilla
 * full-server broadcast. The old Forge {@code ForgeHooks.getServerChatSubmittedDecorator()} call is
 * dropped (NeoForge fires its own decorator/event inside the chat pipeline).
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin implements ServerPlayerConnection, TickablePacketListener, ServerGamePacketListener {

    @Shadow public ServerPlayer player;

    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    private void onBroadcastChatMessage(PlayerChatMessage playerChatMessage, CallbackInfo ci) {
        if (ChatManager.isChatLimit()) {
            ChatManager.broadcastMessage(this.player, playerChatMessage);
            ci.cancel();
        }
    }
}
