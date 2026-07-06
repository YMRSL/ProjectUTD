package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：请求打开 DayZ 玩家背包界面（无容器）。
 * 无负载。
 */
public record OpenDayZMenuC2S() implements CustomPacketPayload {
    public static final Type<OpenDayZMenuC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "open_dayz_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDayZMenuC2S> STREAM_CODEC =
            StreamCodec.unit(new OpenDayZMenuC2S());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDayZMenuC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!BlockZConfigs.isDayzInventoryEnabled()) return;

            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new DayZInventoryMenu(id, inv),
                    Component.translatable("screen.blockz.dayz")
            ), buf -> {
                buf.writeInt(BlockZConfigs.getInitialPocketSlots());
                buf.writeBoolean(false); // 不是通过容器打开的
                buf.writeByte(0);
                CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
            });
        });
    }
}
