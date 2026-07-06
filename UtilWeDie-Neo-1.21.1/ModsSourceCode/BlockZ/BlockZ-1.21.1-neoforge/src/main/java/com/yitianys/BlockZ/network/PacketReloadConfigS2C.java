package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：通知客户端重新加载自定义占格尺寸配置。无负载。
 */
public record PacketReloadConfigS2C() implements CustomPacketPayload {
    public static final Type<PacketReloadConfigS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "reload_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketReloadConfigS2C> STREAM_CODEC =
            StreamCodec.unit(new PacketReloadConfigS2C());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketReloadConfigS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(ItemSizeManager::loadCustomSizes);
    }
}
