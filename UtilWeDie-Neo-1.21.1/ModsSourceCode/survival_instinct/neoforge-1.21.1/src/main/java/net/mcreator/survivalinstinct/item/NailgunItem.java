package net.mcreator.survivalinstinct.item;

import net.mcreator.survivalinstinct.entity.NailProyectileEntity;
import net.mcreator.survivalinstinct.procedures.NailgunRangedItemShootsProjectileProcedure;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class NailgunItem
extends Item {
    public NailgunItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
    }

    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.BLOCK;
    }

    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 72000;
    }

    public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
        InteractionResultHolder ar = InteractionResultHolder.fail((Object)entity.getItemInHand(hand));
        if (entity.getAbilities().instabuild || this.findAmmo(entity) != ItemStack.EMPTY) {
            ar = InteractionResultHolder.success((Object)entity.getItemInHand(hand));
            entity.startUsingItem(hand);
        }
        return ar;
    }

    public void onUseTick(Level world, LivingEntity entity, ItemStack itemstack, int count) {
        if (!world.isClientSide() && entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            ItemStack stack = this.findAmmo((Player)player);
            if (player.getAbilities().instabuild || stack != ItemStack.EMPTY) {
                NailProyectileEntity projectile = NailProyectileEntity.shoot(world, entity, world.getRandom());
                if (player.getAbilities().instabuild) {
                    projectile.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                } else if (stack.isDamageableItem()) {
                    stack.setDamageValue(stack.getDamageValue() + 1);
                    if (stack.getDamageValue() >= stack.getMaxDamage()) {
                        stack.shrink(1);
                        stack.setDamageValue(0);
                        if (stack.isEmpty()) {
                            player.getInventory().removeItem(stack);
                        }
                    }
                } else {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.getInventory().removeItem(stack);
                    }
                }
                NailgunRangedItemShootsProjectileProcedure.execute((Entity)entity, itemstack);
            }
            entity.releaseUsingItem();
        }
    }

    private ItemStack findAmmo(Player player) {
        ItemStack stack = ProjectileWeaponItem.getHeldProjectile((LivingEntity)player, e -> e.getItem() == NailProyectileEntity.PROJECTILE_ITEM.getItem());
        if (stack == ItemStack.EMPTY) {
            for (int i = 0; i < player.getInventory().items.size(); ++i) {
                ItemStack teststack = (ItemStack)player.getInventory().items.get(i);
                if (teststack == null || teststack.getItem() != NailProyectileEntity.PROJECTILE_ITEM.getItem()) continue;
                stack = teststack;
                break;
            }
        }
        return stack;
    }
}

