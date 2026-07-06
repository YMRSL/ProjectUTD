package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.BlockZAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：玩家请求开关 DayZ 界面。服务端校验权限后回送 DayzToggleStateS2C。
 */
public record DayzToggleRequestC2S(boolean enabled) implements CustomPacketPayload {
    public static final Type<DayzToggleRequestC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "dayz_toggle_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DayzToggleRequestC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DayzToggleRequestC2S::enabled,
            DayzToggleRequestC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DayzToggleRequestC2S msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            boolean allowed = BlockZConfigs.getAllowPlayerToggleDayz() || player.hasPermissions(2);
            if (!allowed) {
                player.sendSystemMessage(Component.translatable("msg.blockz.dayz_toggle_denied"));
                PacketDistributor.sendToPlayer(player, new DayzToggleStateS2C(true));
                return;
            }

            PlayerBackpack cap = player.getData(BlockZAttachments.PLAYER_BACKPACK);
            cap.setDayzEnabled(msg.enabled);

            PacketDistributor.sendToPlayer(player, new DayzToggleStateS2C(msg.enabled));
        });
    }
}
