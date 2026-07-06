package com.scarasol.zombiekit.item.weapon;

import com.scarasol.sona.item.IRustItem;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.init.ZombieKitKeyMappings;
import com.scarasol.zombiekit.item.api.SingleHandWeapon;
import com.scarasol.zombiekit.item.weapon.parts.GripParts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Knife extends SwordItem implements IRustItem, SingleHandWeapon {

    public Knife(Tier tier, int damage, float speed, Properties properties) {
        super(tier, properties.attributes(SwordItem.createAttributes(tier, damage, speed)));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        double damage;
        if (this.equals(ZombieKitItems.TRIANGULAR_THORN.get())){
            damage = CommonConfig.TRIANGULAR_THORN_DAMAGE.get();
        }else {
            damage = getTier() == Tiers.NETHERITE ? CommonConfig.NETHERITE_KNIFE_DAMAGE.get() : CommonConfig.KNIFE_DAMAGE.get();
        }
        double speed = getTier() == Tiers.NETHERITE ? CommonConfig.NETHERITE_KNIFE_SPEED.get() : CommonConfig.KNIFE_SPEED.get();
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
                .withModifierAdded(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .withModifierAdded(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, speed - 4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        Item grip = getGripParts(stack);
        if (grip instanceof GripParts gripParts) {
            modifiers = gripParts.applyWeaponModifiers(modifiers, this);
        }
        return modifiers;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack itemstack, Player player, LivingEntity livingEntity, InteractionHand interactionHand){
        super.interactLivingEntity(itemstack, player, livingEntity, interactionHand);
        if (!player.getCooldowns().isOnCooldown(itemstack.getItem()) && (livingEntity instanceof Mob mob && !player.equals(mob.getTarget()))){
            double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            livingEntity.getPersistentData().putBoolean("CancelKnockback", true);
            livingEntity.hurt(player.level().damageSources().playerAttack(player), (float) (CommonConfig.ASSASSINATE_MULTIPLIER.get() * damage));
            if (!player.getAbilities().instabuild) {
                itemstack.hurtAndBreak(5, player, EquipmentSlot.MAINHAND);
                player.getCooldowns().addCooldown(itemstack.getItem(), 140);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP)
            return false;
        return super.canPerformAction(stack, itemAbility);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, TooltipContext context, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.knife.description"));
        list.add(Component.translatable("item.zombiekit.modification", ZombieKitKeyMappings.MODIFICATION_GUI.getKey().getDisplayName()));
        if (getTier() != Tiers.NETHERITE)
            list.add(Component.translatable("item.zombiekit.level_limit"));
    }

    @Override
    public int getMaxDamage(ItemStack itemStack) {
        float modifier = 1;
        Item item = getGripParts(itemStack);
        if (item instanceof GripParts gripParts)
            modifier = modifier * gripParts.getDurabilityModifier();
        return Math.round(super.getMaxDamage(itemStack) * modifier);
    }
}
