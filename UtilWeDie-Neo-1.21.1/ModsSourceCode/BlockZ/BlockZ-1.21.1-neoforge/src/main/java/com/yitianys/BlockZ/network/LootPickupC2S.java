package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.util.InventoryUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：把地面上的掉落物拾取进 DayZ 背包。
 */
public record LootPickupC2S(int entityId) implements CustomPacketPayload {
    public static final Type<LootPickupC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "loot_pickup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LootPickupC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LootPickupC2S::entityId,
            LootPickupC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LootPickupC2S msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (player == null) return;
            Entity e = player.level().getEntity(msg.entityId);
            if (e instanceof ItemEntity item && item.isAlive()) {
                ItemStack stack = item.getItem();
                if (InventoryUtils.addItemToDayZInventory(player.getInventory(), stack)) {
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                            ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
                    if (stack.isEmpty()) {
                        item.discard();
                    }
                }
            }
        });
    }
}
