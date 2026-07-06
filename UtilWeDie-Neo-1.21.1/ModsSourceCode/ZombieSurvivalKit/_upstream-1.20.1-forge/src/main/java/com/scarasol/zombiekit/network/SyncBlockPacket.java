package com.scarasol.zombiekit.network;

import com.scarasol.zombiekit.block.entity.ShortwaveRadioBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncBlockPacket(int id, BlockPos pos, String content) {

    public static SyncBlockPacket decode(FriendlyByteBuf buf) {
        return new SyncBlockPacket(buf.readInt(), buf.readBlockPos(), new String(buf.readByteArray()));
    }

    public static void encode(SyncBlockPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.id);
        buf.writeBlockPos(msg.pos);
        buf.writeByteArray(msg.content.getBytes());
    }

    public static void handler(SyncBlockPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (msg != null) {
                context.get().enqueueWork(() -> {
                    BlockEntity blockEntity = context.get().getDirection().getReceptionSide().isServer() ? context.get().getSender().level().getBlockEntity(msg.pos) : Minecraft.getInstance().level.getBlockEntity(msg.pos);
                    handleButton(blockEntity, msg.id, msg.content);
                });
            }
        });
        context.get().setPacketHandled(true);
    }

    public static void handleButton(BlockEntity blockEntity, int id, String content) {
        if (id == 0) {
            if (blockEntity instanceof ShortwaveRadioBlockEntity shortwaveRadioBlockEntity)
                shortwaveRadioBlockEntity.setContent(content);
        }
    }
}
