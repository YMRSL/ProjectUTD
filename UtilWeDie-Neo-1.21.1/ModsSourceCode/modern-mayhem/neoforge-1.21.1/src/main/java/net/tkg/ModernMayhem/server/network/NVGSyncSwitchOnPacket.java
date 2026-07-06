package net.tkg.ModernMayhem.server.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

public class NVGSyncSwitchOnPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NVGSyncSwitchOnPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModernMayhemMod.ID, "nvg_sync_switch_on"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NVGSyncSwitchOnPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {}, buf -> new NVGSyncSwitchOnPacket());

    public NVGSyncSwitchOnPacket() {
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NVGSyncSwitchOnPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemStack facewearItem;
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && (facewearItem = CuriosUtil.getFaceWearItem((Player)player)).getItem() instanceof GenericSpecialGogglesItem) {
                GenericSpecialGogglesItem.switchOnNVGMode(facewearItem);
            }
        });
    }
}
