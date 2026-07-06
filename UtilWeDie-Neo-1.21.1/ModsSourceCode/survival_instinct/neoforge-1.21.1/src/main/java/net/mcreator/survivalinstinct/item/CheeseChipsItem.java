package net.mcreator.survivalinstinct.item;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class CheeseChipsItem
extends Item {
    public CheeseChipsItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6f).build()));
    }

    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 24;
    }
}

