package net.mcreator.survivalinstinct.item;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class RiceCookieItem
extends Item {
    public RiceCookieItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.8f).build()));
    }

    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 24;
    }
}

