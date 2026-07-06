package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：通知客户端 DayZ 界面的开关状态。
 */
public record DayzToggleStateS2C(boolean enabled) implements CustomPacketPayload {
    public static final Type<DayzToggleStateS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "dayz_toggle_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DayzToggleStateS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DayzToggleStateS2C::enabled,
            DayzToggleStateS2C::new
    );

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DayzToggleStateS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.yitianys.BlockZ.client.network.ClientPacketHandler.handleDayzToggleState(msg);
            }
        });
    }
}
