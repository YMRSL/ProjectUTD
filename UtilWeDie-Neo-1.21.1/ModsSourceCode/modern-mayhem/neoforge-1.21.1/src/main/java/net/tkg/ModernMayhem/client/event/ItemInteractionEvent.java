package net.tkg.ModernMayhem.client.event;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PotionItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.server.config.CommonConfig;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

public class ItemInteractionEvent {
    public static void register() {
        NeoForge.EVENT_BUS.register(ItemInteractionEvent.class);
    }

    @SubscribeEvent
    public static void onRightClickItem(LivingEntityUseItemEvent.Start event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (!CuriosUtil.hasNVGEquipped(player)) {
            return;
        }
        ItemStack stack = CuriosUtil.getFaceWearItem(player);
        ItemStack eventItem = event.getItem();
        if (!(eventItem.has(DataComponents.FOOD) || eventItem.getItem() instanceof MilkBucketItem || eventItem.getItem() instanceof PotionItem)) {
            return;
        }
        if (GenericSpecialGogglesItem.isNVGOnFace(stack) && !((Boolean)CommonConfig.CAN_EAT_WITH_FACEWEAR_DOWN.get()).booleanValue()) {
            event.setCanceled(true);
        }
        ModernMayhemMod.LOGGER.info("Item is edible: {}, player can eat with facewear down: {}, nvg on face: {}", new Object[]{event.getItem().has(DataComponents.FOOD), CommonConfig.CAN_EAT_WITH_FACEWEAR_DOWN.get(), GenericSpecialGogglesItem.isNVGOnFace(stack)});
    }
}

