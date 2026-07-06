package com.scarasol.zombiekit.network;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.block.entity.ShortwaveRadioBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Bidirectional radio-content sync.
 *  - C2S: ShortwaveRadioScreen -> server (resolve block entity against the sender's level).
 *  - S2C: ShortwaveRadioBlock -> client (resolve against the client level).
 *
 * Registered with {@code playBidirectional}. The clientbound branch's {@code Minecraft} access is isolated in the
 * nested {@link ClientAccess} class and only referenced when {@code FMLEnvironment.dist == Dist.CLIENT}, so a dedicated
 * server never classloads {@code net.minecraft.client.Minecraft}.
 */
public record SyncBlockPacket(int id, BlockPos pos, String content) implements CustomPacketPayload {

    public static final Type<SyncBlockPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "sync_block"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncBlockPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SyncBlockPacket::id,
            BlockPos.STREAM_CODEC, SyncBlockPacket::pos,
            ByteBufCodecs.STRING_UTF8, SyncBlockPacket::content,
            SyncBlockPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(SyncBlockPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockEntity blockEntity;
            if (context.flow().isServerbound()) {
                blockEntity = context.player().level().getBlockEntity(msg.pos);
            } else if (FMLEnvironment.dist == Dist.CLIENT) {
                blockEntity = ClientAccess.clientBlockEntity(msg.pos);
            } else {
                blockEntity = null;
            }
            handleButton(blockEntity, msg.id, msg.content);
        });
    }

    public static void handleButton(BlockEntity blockEntity, int id, String content) {
        if (id == 0) {
            if (blockEntity instanceof ShortwaveRadioBlockEntity shortwaveRadioBlockEntity)
                shortwaveRadioBlockEntity.setContent(content);
        }
    }

    /** Client-only access, loaded lazily only on the physical client. */
    private static final class ClientAccess {
        private static BlockEntity clientBlockEntity(BlockPos pos) {
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            return level == null ? null : level.getBlockEntity(pos);
        }
    }
}
