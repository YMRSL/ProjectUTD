package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：同步玩家身上某个背包槽位的物品到客户端镜像。
 */
public record SyncBackpackS2C(int slotId, ItemStack stack) implements CustomPacketPayload {
    public static final Type<SyncBackpackS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "sync_backpack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBackpackS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncBackpackS2C::slotId,
            ItemStack.OPTIONAL_STREAM_CODEC, SyncBackpackS2C::stack,
            SyncBackpackS2C::new
    );

    public SyncBackpackS2C(ItemStack backpack) {
        this(0, backpack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncBackpackS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.yitianys.BlockZ.client.network.ClientPacketHandler.handleSyncBackpack(msg);
            }
        });
    }
}
