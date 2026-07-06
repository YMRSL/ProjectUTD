package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S2C：把服务器侧的占格规则（尺寸 / NBT 规则 / 自定义槽位 / 容量宽度 / 颜色）同步给客户端。
 */
public record SyncGridRulesS2C(
        boolean gridEnabled,
        Map<Item, ItemSizeManager.ItemSize> sizes,
        List<ItemSizeManager.NbtRule> nbtRules,
        Map<Item, Integer> customSlots,
        Map<Item, Integer> capacityWidths,
        Map<Item, Integer> gridColors
) implements CustomPacketPayload {
    public static final Type<SyncGridRulesS2C> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "sync_grid_rules"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncGridRulesS2C> STREAM_CODEC =
            StreamCodec.of(SyncGridRulesS2C::encode, SyncGridRulesS2C::decode);

    public static SyncGridRulesS2C createServerSnapshot() {
        return new SyncGridRulesS2C(
                BlockZConfigs.isGridEnabled(),
                ItemSizeManager.snapshotSizes(),
                ItemSizeManager.snapshotNbtRules(),
                ItemSizeManager.snapshotCustomSlots(),
                ItemSizeManager.snapshotCapacityWidths(),
                ItemSizeManager.snapshotGridColors()
        );
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncGridRulesS2C msg) {
        buf.writeBoolean(msg.gridEnabled);

        buf.writeVarInt(msg.sizes.size());
        msg.sizes.forEach((item, size) -> {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
            buf.writeVarInt(size.width());
            buf.writeVarInt(size.height());
        });

        buf.writeVarInt(msg.nbtRules.size());
        for (ItemSizeManager.NbtRule rule : msg.nbtRules) {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(rule.item()));
            buf.writeUtf(rule.nbtKey());
            buf.writeUtf(rule.nbtValue());
            buf.writeVarInt(rule.width());
            buf.writeVarInt(rule.height());
        }

        buf.writeVarInt(msg.customSlots.size());
        msg.customSlots.forEach((item, slots) -> {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
            buf.writeVarInt(slots);
        });

        buf.writeVarInt(msg.capacityWidths.size());
        msg.capacityWidths.forEach((item, width) -> {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
            buf.writeVarInt(width);
        });

        buf.writeVarInt(msg.gridColors.size());
        msg.gridColors.forEach((item, color) -> {
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(item));
            buf.writeInt(color);
        });
    }

    private static SyncGridRulesS2C decode(RegistryFriendlyByteBuf buf) {
        boolean gridEnabled = buf.readBoolean();

        Map<Item, ItemSizeManager.ItemSize> sizes = new HashMap<>();
        int sizeCount = buf.readVarInt();
        for (int i = 0; i < sizeCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = BuiltInRegistries.ITEM.get(id);
            int w = buf.readVarInt();
            int h = buf.readVarInt();
            if (item != null) {
                sizes.put(item, new ItemSizeManager.ItemSize(w, h));
            }
        }

        List<ItemSizeManager.NbtRule> nbtRules = new ArrayList<>();
        int ruleCount = buf.readVarInt();
        for (int i = 0; i < ruleCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = BuiltInRegistries.ITEM.get(id);
            String key = buf.readUtf();
            String value = buf.readUtf();
            int w = buf.readVarInt();
            int h = buf.readVarInt();
            if (item != null) {
                nbtRules.add(new ItemSizeManager.NbtRule(item, key, value, w, h));
            }
        }

        Map<Item, Integer> customSlots = new HashMap<>();
        int slotCount = buf.readVarInt();
        for (int i = 0; i < slotCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = BuiltInRegistries.ITEM.get(id);
            int slots = buf.readVarInt();
            if (item != null) {
                customSlots.put(item, slots);
            }
        }

        Map<Item, Integer> capacityWidths = new HashMap<>();
        int widthCount = buf.readVarInt();
        for (int i = 0; i < widthCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = BuiltInRegistries.ITEM.get(id);
            int width = buf.readVarInt();
            if (item != null) {
                capacityWidths.put(item, width);
            }
        }

        Map<Item, Integer> gridColors = new HashMap<>();
        int colorCount = buf.readVarInt();
        for (int i = 0; i < colorCount; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Item item = BuiltInRegistries.ITEM.get(id);
            int color = buf.readInt();
            if (item != null) {
                gridColors.put(item, color);
            }
        }

        return new SyncGridRulesS2C(gridEnabled, sizes, nbtRules, customSlots, capacityWidths, gridColors);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncGridRulesS2C msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ItemSizeManager.setSyncedGridEnabled(msg.gridEnabled);
            ItemSizeManager.setRules(msg.sizes, msg.nbtRules, msg.customSlots, msg.capacityWidths, msg.gridColors);
        });
    }
}
