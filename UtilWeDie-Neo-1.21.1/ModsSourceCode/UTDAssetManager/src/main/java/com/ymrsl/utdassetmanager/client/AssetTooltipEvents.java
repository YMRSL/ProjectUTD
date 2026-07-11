package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.UTDAssetManagerMod;
import com.ymrsl.utdassetmanager.core.AssetStatus;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = UTDAssetManagerMod.MOD_ID, value = Dist.CLIENT)
public final class AssetTooltipEvents {
    private static ItemStack cachedStack = ItemStack.EMPTY;
    private static AssetRecord cachedRecord;
    private static AssetStatus cachedStatus;
    private static long cachedAt;

    private AssetTooltipEvents() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        long now = System.currentTimeMillis();
        AssetRecord record;
        AssetStatus status;
        if (cachedRecord != null && cachedStatus != null && now - cachedAt < 250L
                && ItemStack.isSameItemSameComponents(cachedStack, stack)) {
            record = cachedRecord;
            status = cachedStatus;
        } else {
            record = AssetStackCodec.capture(stack);
            if (record == null) return;
            status = AssetRepository.get().statusFor(record);
            cachedStack = stack.copyWithCount(1);
            cachedRecord = record;
            cachedStatus = status;
            cachedAt = now;
        }
        if (record == null) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        String headline = status.catalogued() ? "已纳管" : status.humanSelected() ? "已标注 / 待纳管" : "未管理";
        tooltip.add(Component.literal("UTD // " + headline).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("human_selected: " + status.humanSelected()
                + "  catalogued: " + status.catalogued()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("recipe_input: " + status.recipeInputCount()
                + "  recipe_output: " + status.recipeOutputCount()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("loot_enabled: " + status.lootEnabled()
                + "  loot_level: " + status.lootLevel()).withStyle(status.lootEnabled() ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("sync_state: " + status.syncState().name().toLowerCase()
                + "  stale: " + status.stale()).withStyle(status.needsSync() ? ChatFormatting.YELLOW : ChatFormatting.GREEN));
        tooltip.add(Component.literal("issues: " + status.issues().size()).withStyle(status.issues().isEmpty()
                ? ChatFormatting.DARK_GRAY : ChatFormatting.RED));
        if (!"zh_cn".equalsIgnoreCase(record.capturedLocale)) {
            tooltip.add(Component.literal("名称采集语言不是 zh_cn: " + record.capturedLocale).withStyle(ChatFormatting.RED));
        }
    }
}
