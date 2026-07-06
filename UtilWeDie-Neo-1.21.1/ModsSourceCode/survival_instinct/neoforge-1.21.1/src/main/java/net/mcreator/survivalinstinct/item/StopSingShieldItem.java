package net.mcreator.survivalinstinct.item;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class StopSingShieldItem
extends ShieldItem {
    public StopSingShieldItem() {
        super(new Item.Properties().durability(410));
    }

    public boolean isValidRepairItem(ItemStack itemstack, ItemStack repairitem) {
        return Ingredient.of((ItemStack[])new ItemStack[]{new ItemStack((ItemLike)SurvivalInstinctModItems.ALUMINIUM.get())}).test(repairitem);
    }
}

