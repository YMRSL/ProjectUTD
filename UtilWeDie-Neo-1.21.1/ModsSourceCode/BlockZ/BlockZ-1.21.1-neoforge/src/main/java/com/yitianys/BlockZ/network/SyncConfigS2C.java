package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.BlockZConfigs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：把服务器侧 BlockZ 配置同步给客户端（裁剪版，仅占格背包/服装相关 17 项）。
 * 字段顺序与 {@link BlockZConfigs#setSyncedValues} 完全一致。
 * 上游的护理 / 体力 / 镜头 / 尸体 / 雾气 / lean / HUD / 僵尸等已 DROP 字段全部移除。
 */
public record SyncConfigS2C(
        int gridCols,
        int gridRows,
        boolean enableGridSystem,
        double uiScale,
        boolean enableDayzInventory,
        boolean allowPlayerToggleDayz,
        boolean showDayzToggleChatHint,
        boolean enableVanillaBackpackLock,
        int initialPocketSlots,
        int backpackCoyoteSlots,
        int backpackAliceSlots,
        int backpackCzechSlots,
        int backpackCzechPouchSlots,
        int backpackPatrolPackSlots,
        int vest0Slots,
        int shirtSlots,
        int pantsSlots
) implements CustomPacketPayload {
    public static final Type<SyncConfigS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "sync_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncConfigS2C> STREAM_CODEC =
            StreamCodec.of(SyncConfigS2C::encode, SyncConfigS2C::decode);

    /** 从当前服务器配置生成一个快照包。替代上游的 {@code new SyncConfigS2C()}。 */
    public static SyncConfigS2C createServerSnapshot() {
        return new SyncConfigS2C(
                BlockZConfigs.getGridCols(),
                BlockZConfigs.getGridRows(),
                BlockZConfigs.isGridEnabled(),
                BlockZConfigs.getUiScale(),
                BlockZConfigs.isDayzInventoryEnabled(),
                BlockZConfigs.getAllowPlayerToggleDayz(),
                BlockZConfigs.getShowDayzToggleChatHint(),
                BlockZConfigs.getEnableVanillaBackpackLock(),
                BlockZConfigs.getInitialPocketSlots(),
                BlockZConfigs.getBackpackCoyoteSlots(),
                BlockZConfigs.getBackpackAliceSlots(),
                BlockZConfigs.getBackpackCzechSlots(),
                BlockZConfigs.getBackpackCzechPouchSlots(),
                BlockZConfigs.getBackpackPatrolPackSlots(),
                BlockZConfigs.getVest0Slots(),
                BlockZConfigs.getShirtSlots(),
                BlockZConfigs.getPantsSlots()
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncConfigS2C msg) {
        buf.writeInt(msg.gridCols);
        buf.writeInt(msg.gridRows);
        buf.writeBoolean(msg.enableGridSystem);
        buf.writeDouble(msg.uiScale);
        buf.writeBoolean(msg.enableDayzInventory);
        buf.writeBoolean(msg.allowPlayerToggleDayz);
        buf.writeBoolean(msg.showDayzToggleChatHint);
        buf.writeBoolean(msg.enableVanillaBackpackLock);
        buf.writeInt(msg.initialPocketSlots);
        buf.writeInt(msg.backpackCoyoteSlots);
        buf.writeInt(msg.backpackAliceSlots);
        buf.writeInt(msg.backpackCzechSlots);
        buf.writeInt(msg.backpackCzechPouchSlots);
        buf.writeInt(msg.backpackPatrolPackSlots);
        buf.writeInt(msg.vest0Slots);
        buf.writeInt(msg.shirtSlots);
        buf.writeInt(msg.pantsSlots);
    }

    private static SyncConfigS2C decode(RegistryFriendlyByteBuf buf) {
        int gridCols = buf.readInt();
        int gridRows = buf.readInt();
        boolean enableGridSystem = buf.readBoolean();
        double uiScale = buf.readDouble();
        boolean enableDayzInventory = buf.readBoolean();
        boolean allowPlayerToggleDayz = buf.readBoolean();
        boolean showDayzToggleChatHint = buf.readBoolean();
        boolean enableVanillaBackpackLock = buf.readBoolean();
        int initialPocketSlots = buf.readInt();
        int backpackCoyoteSlots = buf.readInt();
        int backpackAliceSlots = buf.readInt();
        int backpackCzechSlots = buf.readInt();
        int backpackCzechPouchSlots = buf.readInt();
        int backpackPatrolPackSlots = buf.readInt();
        int vest0Slots = buf.readInt();
        int shirtSlots = buf.readInt();
        int pantsSlots = buf.readInt();
        return new SyncConfigS2C(
                gridCols, gridRows, enableGridSystem, uiScale, enableDayzInventory,
                allowPlayerToggleDayz, showDayzToggleChatHint, enableVanillaBackpackLock, initialPocketSlots,
                backpackCoyoteSlots, backpackAliceSlots, backpackCzechSlots, backpackCzechPouchSlots,
                backpackPatrolPackSlots, vest0Slots, shirtSlots, pantsSlots
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncConfigS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> BlockZConfigs.setSyncedValues(
                msg.gridCols, msg.gridRows, msg.enableGridSystem, msg.uiScale, msg.enableDayzInventory,
                msg.allowPlayerToggleDayz, msg.showDayzToggleChatHint,
                msg.enableVanillaBackpackLock, msg.initialPocketSlots,
                msg.backpackCoyoteSlots, msg.backpackAliceSlots, msg.backpackCzechSlots, msg.backpackCzechPouchSlots,
                msg.backpackPatrolPackSlots, msg.vest0Slots, msg.shirtSlots, msg.pantsSlots
        ));
    }
}
