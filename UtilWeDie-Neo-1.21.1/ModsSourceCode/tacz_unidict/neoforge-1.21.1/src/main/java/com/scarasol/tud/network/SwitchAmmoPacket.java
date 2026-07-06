package com.scarasol.tud.network;

import com.scarasol.tud.client.network.ClientNetworkHandler;
import com.scarasol.tud.manager.AmmoManager;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.scarasol.tud.TudMod.MODID;

public record SwitchAmmoPacket(int id) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SwitchAmmoPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MODID, "switch_ammo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchAmmoPacket> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SwitchAmmoPacket::id, SwitchAmmoPacket::new);

    @Override
    public CustomPacketPayload.Type<SwitchAmmoPacket> type() {
        return TYPE;
    }

    public static void handle(SwitchAmmoPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isServerbound()) {
                if (context.player() instanceof ServerPlayer serverPlayer) {
                    ItemStack itemStack = serverPlayer.getMainHandItem();
                    IGun iGun = IGun.getIGunOrNull(itemStack);
                    if (iGun != null) {
                        if (serverPlayer.isCreative()) {
                            CustomData.update(DataComponents.CUSTOM_DATA, itemStack, t -> t.putInt("TudCurrentAmmo", msg.id()));
                        }
                        iGun.dropAllAmmo(serverPlayer, itemStack);
                        if (iGun.hasBulletInBarrel(itemStack) && !serverPlayer.isCreative()) {
                            iGun.setBulletInBarrel(itemStack, false);
                            ItemStack ammoItem = AmmoManager.getGunAmmo(itemStack);
                            ItemHandlerHelper.giveItemToPlayer(serverPlayer, ammoItem);
                        }
                        CustomData.update(DataComponents.CUSTOM_DATA, itemStack, t -> t.putInt("TudCurrentAmmo", msg.id()));
                        IGunOperator.fromLivingEntity(serverPlayer).initialData();
                        PacketDistributor.sendToPlayer(serverPlayer, msg);
                    }
                }
            } else {
                ClientNetworkHandler.reload();
            }
        });
    }
}
