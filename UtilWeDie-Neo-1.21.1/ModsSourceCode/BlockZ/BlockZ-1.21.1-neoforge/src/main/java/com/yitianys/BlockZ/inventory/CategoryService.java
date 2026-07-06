package com.yitianys.BlockZ.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.PotionItem;

public class CategoryService {
    public enum Category {WEAPON, FOOD, MEDICAL, OTHER}

    public static Category of(ItemStack stack) {
        if (stack.getItem() instanceof SwordItem) return Category.WEAPON;
        // 1.21: Item.isEdible() 移除，食物属性走 FOOD 数据组件。
        if (stack.has(DataComponents.FOOD)) return Category.FOOD;
        if (stack.getItem() instanceof PotionItem) return Category.MEDICAL;
        return Category.OTHER;
    }
}
