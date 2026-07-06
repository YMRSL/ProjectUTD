package net.mcreator.survivalinstinct.item;

import net.mcreator.survivalinstinct.procedures.GasolineCanOnPlayerStoppedUsingProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class GasolineCanItem
extends Item {
    public GasolineCanItem() {
        super(new Item.Properties().durability(3).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(12).saturationModifier(0.1f).alwaysEdible().build()));
    }

    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.DRINK;
    }

    public boolean hasCraftingRemainingItem() {
        return true;
    }

    public ItemStack getCraftingRemainingItem(ItemStack itemstack) {
        ItemStack retval = new ItemStack((ItemLike)this);
        retval.setDamageValue(itemstack.getDamageValue() + 1);
        if (retval.getDamageValue() >= retval.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        return retval;
    }

    public boolean isRepairable(ItemStack itemstack) {
        return false;
    }

    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 42;
    }

    public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
        ItemStack retval = super.finishUsingItem(itemstack, world, entity);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        GasolineCanOnPlayerStoppedUsingProcedure.execute((Entity)entity);
        return retval;
    }
}

