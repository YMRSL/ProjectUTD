package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ComposterBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@EventBusSubscriber(bus=EventBusSubscriber.Bus.MOD, modid = "survival_instinct")
public class SurvivalInstinctModCompostableItems {
    @SubscribeEvent
    public static void addComposterItems(FMLCommonSetupEvent event) {
        ComposterBlock.COMPOSTABLES.put((ItemLike)SurvivalInstinctModItems.ROTTEN_APPLE.get(), 0.7f);
        ComposterBlock.COMPOSTABLES.put((ItemLike)SurvivalInstinctModItems.ROTTEN_ORANGE.get(), 0.7f);
    }
}

