package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：从一个已存在的方块/实体容器打开 DayZ 界面。
 * type=1 携带 BlockPos；type=2 携带 entityId。
 */
public record OpenDayZContainerC2S(byte kind, BlockPos pos, int entityId) implements CustomPacketPayload {
    private static final byte TYPE_POS = 1;
    private static final byte TYPE_ENTITY = 2;

    public static final Type<OpenDayZContainerC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "open_dayz_container"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDayZContainerC2S> STREAM_CODEC =
            StreamCodec.of(OpenDayZContainerC2S::encode, OpenDayZContainerC2S::decode);

    public static OpenDayZContainerC2S forPos(BlockPos pos) {
        return new OpenDayZContainerC2S(TYPE_POS, pos, -1);
    }

    public static OpenDayZContainerC2S forEntity(int entityId) {
        return new OpenDayZContainerC2S(TYPE_ENTITY, null, entityId);
    }

    private static void encode(RegistryFriendlyByteBuf buf, OpenDayZContainerC2S msg) {
        buf.writeByte(msg.kind);
        if (msg.kind == TYPE_POS && msg.pos != null) {
            buf.writeBlockPos(msg.pos);
        } else if (msg.kind == TYPE_ENTITY) {
            buf.writeInt(msg.entityId);
        }
    }

    private static OpenDayZContainerC2S decode(RegistryFriendlyByteBuf buf) {
        byte type = buf.readByte();
        if (type == TYPE_POS) {
            BlockPos pos = buf.readBlockPos();
            return new OpenDayZContainerC2S(TYPE_POS, pos, -1);
        }
        if (type == TYPE_ENTITY) {
            int entityId = buf.readInt();
            return new OpenDayZContainerC2S(TYPE_ENTITY, null, entityId);
        }
        return new OpenDayZContainerC2S((byte) 0, null, -1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenDayZContainerC2S msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!BlockZConfigs.isDayzInventoryEnabled()) return;

            ServerLevel level = player.serverLevel();

            if (msg.kind == TYPE_POS && msg.pos != null) {
                if (!level.isLoaded(msg.pos)) return;
                BlockEntity be = level.getBlockEntity(msg.pos);
                if (be == null) return;
                Component title = be instanceof Nameable nameable ? nameable.getDisplayName() : Component.translatable("container.inventory");

                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new DayZInventoryMenu(id, inv, msg.pos),
                        title
                ), buf -> {
                    buf.writeInt(BlockZConfigs.getInitialPocketSlots());
                    buf.writeBoolean(true);
                    buf.writeBlockPos(msg.pos);
                    CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
                });
                return;
            }

            if (msg.kind == TYPE_ENTITY) {
                Entity entity = level.getEntity(msg.entityId);
                if (entity == null) return;
                Component title = entity.getDisplayName();

                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new DayZInventoryMenu(id, inv, entity),
                        title
                ), buf -> {
                    buf.writeInt(BlockZConfigs.getInitialPocketSlots());
                    buf.writeBoolean(false);
                    // New Protocol:
                    // boolean hasPos (false)
                    // byte type (1 = Entity)
                    // int entityId
                    buf.writeByte(1); // Type: Entity
                    buf.writeInt(entity.getId());
                    CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
                });
            }
        });
    }
}
