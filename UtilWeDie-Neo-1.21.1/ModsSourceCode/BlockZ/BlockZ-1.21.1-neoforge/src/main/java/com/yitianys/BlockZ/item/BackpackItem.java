package com.yitianys.BlockZ.item;

import com.yitianys.BlockZ.init.BlockZDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

public class BackpackItem extends Item {
    public BackpackItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);

        CompoundTag invTag = stack.get(BlockZDataComponents.BACKPACK_INVENTORY.get());
        if (invTag == null || invTag.isEmpty()) return;
        if (context.registries() == null) return;

        ItemStackHandler handler = new ItemStackHandler();
        handler.deserializeNBT(context.registries(), invTag);
        if (handler.getSlots() == 0) return;

        tooltipComponents.add(Component.literal("§7Contents:"));

        int count = 0;
        int total = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) {
                total++;
            }
        }
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack itemStack = handler.getStackInSlot(i);
            if (itemStack.isEmpty()) continue;
            if (count >= 5) { // Limit to 5 items to prevent huge tooltips
                tooltipComponents.add(Component.literal("§7... and " + (total - count) + " more"));
                break;
            }
            tooltipComponents.add(Component.literal("§8- " + itemStack.getHoverName().getString() + " x" + itemStack.getCount()));
            count++;
        }
    }
}
