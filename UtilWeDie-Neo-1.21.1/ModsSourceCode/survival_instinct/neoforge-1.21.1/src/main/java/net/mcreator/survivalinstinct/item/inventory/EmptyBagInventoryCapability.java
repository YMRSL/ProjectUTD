package net.mcreator.survivalinstinct.item.inventory;

import net.mcreator.survivalinstinct.client.gui.EmptyBagGUIScreen;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;

@EventBusSubscriber(value={Dist.CLIENT}, modid = "survival_instinct")
public class EmptyBagInventoryCapability {
    @SubscribeEvent
    @OnlyIn(value=Dist.CLIENT)
    public static void onItemDropped(ItemTossEvent event) {
        if (event.getEntity().getItem().getItem() == SurvivalInstinctModItems.EMPTY_BAG.get() && Minecraft.getInstance().screen instanceof EmptyBagGUIScreen) {
            Minecraft.getInstance().player.closeContainer();
        }
    }
}
