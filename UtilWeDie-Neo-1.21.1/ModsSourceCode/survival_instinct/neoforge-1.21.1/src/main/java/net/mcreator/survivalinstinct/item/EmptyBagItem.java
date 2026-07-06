package net.mcreator.survivalinstinct.item;

import io.netty.buffer.Unpooled;
import net.mcreator.survivalinstinct.world.inventory.EmptyBagGUIMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class EmptyBagItem
extends Item {
    public EmptyBagItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON).component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY));
    }

    public InteractionResultHolder<ItemStack> use(Level world, final Player entity, final InteractionHand hand) {
        InteractionResultHolder ar = super.use(world, entity, hand);
        if (entity instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)entity;
            serverPlayer.openMenu((MenuProvider)new MenuProvider(){

                public Component getDisplayName() {
                    return Component.literal((String)"Empty Bag");
                }

                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                    FriendlyByteBuf packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
                    packetBuffer.writeBlockPos(entity.blockPosition());
                    packetBuffer.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
                    return new EmptyBagGUIMenu(id, inventory, packetBuffer);
                }
            }, buf -> {
                buf.writeBlockPos(entity.blockPosition());
                buf.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
            });
        }
        return ar;
    }
}
