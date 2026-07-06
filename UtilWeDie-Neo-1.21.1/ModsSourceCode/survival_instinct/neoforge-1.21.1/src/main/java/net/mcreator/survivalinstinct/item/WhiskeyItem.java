package net.mcreator.survivalinstinct.item;

import java.util.List;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.mcreator.survivalinstinct.procedures.WhiskeyPlayerFinishesUsingItemProcedure;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

public class WhiskeyItem
extends Item {
    public WhiskeyItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON).food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4f).alwaysEdible().build()));
    }

    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.DRINK;
    }

    public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add((Component)Component.literal((String)"\u00a77Item Effect:"));
        list.add((Component)Component.literal((String)" \u00a79Absorption II for 60 seconds and Nausea I for 8 seconds"));
    }

    public ItemStack finishUsingItem(ItemStack itemstack, Level world, LivingEntity entity) {
        ItemStack retval = new ItemStack((ItemLike)SurvivalInstinctModItems.EMPTY_BOTTLE.get());
        super.finishUsingItem(itemstack, world, entity);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        WhiskeyPlayerFinishesUsingItemProcedure.execute((Entity)entity);
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

