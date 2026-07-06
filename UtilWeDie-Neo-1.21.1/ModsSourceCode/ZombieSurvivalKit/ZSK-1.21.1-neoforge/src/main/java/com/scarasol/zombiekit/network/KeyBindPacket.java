package com.scarasol.zombiekit.network;


import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.inventory.WeaponModificationMenu;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import com.scarasol.zombiekit.item.weapon.parts.ChargingParts;
import com.scarasol.zombiekit.item.api.ModifiableWeapon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record KeyBindPacket(int mode) implements CustomPacketPayload {

    public static final Type<KeyBindPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "key_bind"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KeyBindPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, KeyBindPacket::mode,
            KeyBindPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public int getMode() {
        return mode;
    }

    public static void handler(KeyBindPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (msg.getMode() <= 5)
                    ExoArmor.switchMode(player, msg.getMode());
                else if (msg.getMode() == 6) {
                    if (player.getMainHandItem().getItem() instanceof ModifiableWeapon)
                        player.openMenu(new MenuProvider() {
                            @Override
                            public Component getDisplayName() {
                                return Component.literal("weapon_modification_gui");
                            }

                            @Override
                            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                                RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), p.registryAccess());
                                ItemStack.STREAM_CODEC.encode(buf, player.getMainHandItem());
                                return new WeaponModificationMenu(id, inventory, buf);
                            }
                        }, buf -> ItemStack.STREAM_CODEC.encode(buf, player.getMainHandItem()));
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
}
