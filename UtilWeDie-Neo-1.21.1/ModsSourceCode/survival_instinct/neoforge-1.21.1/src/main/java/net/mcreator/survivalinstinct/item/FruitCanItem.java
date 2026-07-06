package net.mcreator.survivalinstinct.item;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class FruitCanItem
extends Item {
    public FruitCanItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.0f).build()));
    }

    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 42;
    }

    public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
        ItemStack retval = new ItemStack((ItemLike)SurvivalInstinctModItems.EMPTY_CAN.get());
        super.finishUsingItem(itemstack, world, entity);
        if (itemstack.isEmpty()) {
            return retval;
        }
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (!player.getAbilities().instabuild && !player.getInventory().add(retval)) {
                player.drop(retval, false);
            }
        }
        return itemstack;
    }
}

