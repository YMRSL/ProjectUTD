package com.scarasol.sona.network;

import com.scarasol.sona.SonaMod;
import com.scarasol.sona.accessor.mixin.IBaseContainerBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * C2S: 客户端上锁请求。
 */
public record LockPacket(BlockPos blockPos, UUID lockCode, boolean locked) implements CustomPacketPayload {

    public static final Type<LockPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "lock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LockPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, LockPacket::blockPos,
            ByteBufCodecs.fromCodec(net.minecraft.core.UUIDUtil.CODEC), LockPacket::lockCode,
            ByteBufCodecs.BOOL, LockPacket::locked,
            LockPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LockPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                BlockEntity blockEntity = level.getBlockEntity(msg.blockPos());
                BlockState blockState = level.getBlockState(msg.blockPos());
                if (blockEntity instanceof IBaseContainerBlockEntityAccessor baseContainerBlockEntity && !blockEntity.getPersistentData().contains("flag")) {
                    if (msg.locked())
                        baseContainerBlockEntity.lockContainer(blockEntity.getPersistentData(), msg.lockCode());
                    blockEntity.getPersistentData().putBoolean("LockFlag", true);
                    level.sendBlockUpdated(msg.blockPos(), blockState, blockState, 3);
                }
            }
        });
    }
}
