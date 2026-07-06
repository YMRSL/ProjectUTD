package net.mcreator.survivalinstinct.item;
import net.minecraft.world.entity.LivingEntity;

import net.mcreator.survivalinstinct.procedures.MREOpenProcedure;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class MREItem
extends Item {
    public MREItem() {
        super(new Item.Properties().stacksTo(16).rarity(Rarity.COMMON));
    }

    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.EAT;
    }

    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 42;
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        InteractionResultHolder ar = super.use(world, entity, hand);
        MREOpenProcedure.execute((LevelAccessor)world, entity.getX(), entity.getY(), entity.getZ(), (Entity)entity);
        return ar;
    }
}

