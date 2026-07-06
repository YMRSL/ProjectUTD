package net.mcreator.survivalinstinct.item;

import java.util.List;
import net.mcreator.survivalinstinct.procedures.MorphineSyringePlayerFinishesUsingItemProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class MorphineSyringeItem
extends Item {
    public MorphineSyringeItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(0).saturationModifier(0.0f).alwaysEdible().build()));
    }

    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.BOW;
    }

    public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add((Component)Component.literal((String)"\u00a77Item Effect:"));
        list.add((Component)Component.literal((String)" \u00a79Resistance II and Hunger I for 45 seconds"));
    }

    public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
        ItemStack retval = super.finishUsingItem(itemstack, world, entity);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        MorphineSyringePlayerFinishesUsingItemProcedure.execute((Entity)entity);
        return retval;
    }
}

