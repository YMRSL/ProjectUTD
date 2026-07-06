package net.mcreator.survivalinstinct.item;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;

public class GallonOfWaterItem
extends Item {
    public GallonOfWaterItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.6f).alwaysEdible().build()));
    }

    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.DRINK;
    }

    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 45;
    }
}

