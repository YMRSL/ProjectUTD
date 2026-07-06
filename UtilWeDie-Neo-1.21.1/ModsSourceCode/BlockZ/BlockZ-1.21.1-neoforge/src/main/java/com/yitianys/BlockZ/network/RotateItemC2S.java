package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：旋转当前光标携带的占格物品。无负载。
 */
public record RotateItemC2S() implements CustomPacketPayload {
    public static final Type<RotateItemC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "rotate_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RotateItemC2S> STREAM_CODEC =
            StreamCodec.unit(new RotateItemC2S());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RotateItemC2S payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (player.containerMenu instanceof DayZInventoryMenu menu) {
                ItemStack carried = menu.getCarried();
                if (carried.isEmpty()) return;
                if (ItemSizeManager.toggleRotation(carried)) {
                    menu.setCarried(carried);
                    menu.broadcastChanges();
                }
            }
        });
    }
}
