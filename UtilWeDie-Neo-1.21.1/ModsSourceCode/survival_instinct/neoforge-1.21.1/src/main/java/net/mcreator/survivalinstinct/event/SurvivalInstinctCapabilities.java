package net.mcreator.survivalinstinct.event;

import net.mcreator.survivalinstinct.SurvivalInstinctMod;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModBlockEntities;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

@EventBusSubscriber(modid = SurvivalInstinctMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class SurvivalInstinctCapabilities {
    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SurvivalInstinctModBlockEntities.CASH_REGISTER.get(), (be, side) -> new SidedInvWrapper((net.minecraft.world.WorldlyContainer) be, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SurvivalInstinctModBlockEntities.REFRIGERATOR.get(), (be, side) -> new SidedInvWrapper((net.minecraft.world.WorldlyContainer) be, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SurvivalInstinctModBlockEntities.TRASH_CAN.get(), (be, side) -> new SidedInvWrapper((net.minecraft.world.WorldlyContainer) be, side));
        event.registerItem(Capabilities.ItemHandler.ITEM,
            (stack, ctx) -> new net.mcreator.survivalinstinct.capability.BagItemHandler(stack, net.minecraft.core.component.DataComponents.CONTAINER, 9, 16, () -> SurvivalInstinctModItems.EMPTY_BAG.get()),
            SurvivalInstinctModItems.EMPTY_BAG.get());
        event.registerItem(Capabilities.ItemHandler.ITEM,
            (stack, ctx) -> new net.mcreator.survivalinstinct.capability.BagItemHandler(stack, net.minecraft.core.component.DataComponents.CONTAINER, 9, 1, () -> SurvivalInstinctModItems.GARBAGE_BAG.get()),
            SurvivalInstinctModItems.GARBAGE_BAG.get());
    }
}
