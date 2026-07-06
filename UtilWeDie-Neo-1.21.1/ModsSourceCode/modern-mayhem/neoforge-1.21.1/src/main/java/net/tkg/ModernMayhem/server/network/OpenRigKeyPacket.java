package net.tkg.ModernMayhem.server.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

public class OpenRigKeyPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenRigKeyPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModernMayhemMod.ID, "open_rig_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRigKeyPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {}, buf -> new OpenRigKeyPacket());

    public OpenRigKeyPacket() {
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenRigKeyPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ItemStack backpack = CuriosUtil.getRigItem((Player)player);
            Item item = backpack.getItem();
            if (item instanceof GenericBackpackItem) {
                GenericBackpackItem genericBackpackItem = (GenericBackpackItem)item;
                genericBackpackItem.OpenGUIFromCuriosInventory((Player)player, backpack);
            }
        });
    }
}
