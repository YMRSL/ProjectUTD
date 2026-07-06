package net.mcreator.survivalinstinct.item;

import net.mcreator.survivalinstinct.procedures.RottenApplePlayerFinishesUsingItemProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class RottenOrangeItem
extends Item {
    public RottenOrangeItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).build()));
    }

    public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
        ItemStack retval = super.finishUsingItem(itemstack, world, entity);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        RottenApplePlayerFinishesUsingItemProcedure.execute((Entity)entity);
        return retval;
    }
}

