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
 * S2C：告知客户端是否允许玩家自行切换 DayZ 界面。
 */
public record DayzTogglePermissionS2C(boolean allowed) implements CustomPacketPayload {
    public static final Type<DayzTogglePermissionS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "dayz_toggle_permission"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DayzTogglePermissionS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DayzTogglePermissionS2C::allowed,
            DayzTogglePermissionS2C::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DayzTogglePermissionS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                com.yitianys.BlockZ.client.network.ClientPacketHandler.handleDayzTogglePermission(msg);
            }
        });
    }
}
