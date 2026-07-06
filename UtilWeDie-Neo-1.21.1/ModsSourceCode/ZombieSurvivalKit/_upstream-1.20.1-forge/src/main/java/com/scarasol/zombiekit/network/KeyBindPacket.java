package com.scarasol.zombiekit.network;


import com.scarasol.zombiekit.inventory.WeaponModificationMenu;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import com.scarasol.zombiekit.item.weapon.parts.ChargingParts;
import com.scarasol.zombiekit.item.api.ModifiableWeapon;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.function.Supplier;

public class KeyBindPacket {

    private final int mode;

    public KeyBindPacket(int mode) {
        this.mode = mode;
    }

    public int getMode() {
        return mode;
    }

    public static KeyBindPacket decode(FriendlyByteBuf buf) {
        return new KeyBindPacket(buf.readInt());
    }

    public static void encode(KeyBindPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.getMode());
    }

    public static void handler(KeyBindPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (msg != null) {
                context.get().enqueueWork(() -> {
                    if (context.get().getDirection().getReceptionSide().isServer()){
                        ServerPlayer player = context.get().getSender();
                        if (msg.getMode() <= 5)
                            ExoArmor.switchMode(player, msg.getMode());
                        else if (msg.getMode() == 6) {
                            if (player.getMainHandItem().getItem() instanceof ModifiableWeapon)
                                NetworkHooks.openScreen(player, new MenuProvider() {
                                    @Override
                                    public Component getDisplayName() {
                                        return Component.literal("weapon_modification_gui");
                                    }

                                    @Override
                                    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                                        return new WeaponModificationMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeItem(player.getMainHandItem()));
                                    }
                                }, friendlyByteBuf -> friendlyByteBuf.writeItem(player.getMainHandItem()));
                        } else if (msg.getMode() == 7) {
                            ItemStack itemStack = player.getMainHandItem();
                            if (itemStack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
                                Item parts = modifiableWeapon.getChargingParts(itemStack);
                                if (parts instanceof ChargingParts chargingParts) {
                                    chargingParts.attack((ServerLevel) player.level(), player, (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
                                }
                            }
                        } else if (msg.getMode() == 8) {
                            ItemStack itemStack = player.getMainHandItem();
                            if (itemStack.getItem() instanceof Flamethrower flamethrower) {
                                flamethrower.reload(player, player.level(), InteractionHand.MAIN_HAND);
                            }
                        }

                    }
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}
