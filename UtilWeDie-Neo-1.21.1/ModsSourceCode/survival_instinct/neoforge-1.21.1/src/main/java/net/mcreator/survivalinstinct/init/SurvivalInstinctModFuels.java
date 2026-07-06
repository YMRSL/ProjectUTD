package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "survival_instinct")
public class SurvivalInstinctModFuels {
    @SubscribeEvent
    public static void furnaceFuelBurnTimeEvent(FurnaceFuelBurnTimeEvent event) {
        ItemStack itemstack = event.getItemStack();
        if (itemstack.getItem() == SurvivalInstinctModItems.APPLIANCE_MAGAZINE.get()) {
            event.setBurnTime(600);
        } else if (itemstack.getItem() == SurvivalInstinctModItems.AUTOMOBILE_MAGAZINE.get()) {
            event.setBurnTime(600);
        } else if (itemstack.getItem() == SurvivalInstinctModItems.DUNGEONS_AND_COMBAT_MAGAZINE.get()) {
            event.setBurnTime(600);
        } else if (itemstack.getItem() == SurvivalInstinctModItems.MONEY.get()) {
            event.setBurnTime(1200);
        } else if (itemstack.getItem() == SurvivalInstinctModItems.GASOLINE_CAN.get()) {
            event.setBurnTime(4600);
        }
    }
}

