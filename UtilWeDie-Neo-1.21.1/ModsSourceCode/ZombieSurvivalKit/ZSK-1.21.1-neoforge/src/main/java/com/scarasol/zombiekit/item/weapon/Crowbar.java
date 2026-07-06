package com.scarasol.zombiekit.item.weapon;

import com.scarasol.sona.item.IRustItem;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitKeyMappings;
import com.scarasol.zombiekit.item.api.SingleHandWeapon;
import com.scarasol.zombiekit.item.weapon.parts.GripParts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Crowbar extends SwordItem implements IRustItem, SingleHandWeapon {

    public Crowbar(Tier tier, int damage, float speed, Properties properties) {
        super(tier, properties.attributes(SwordItem.createAttributes(tier, damage, speed)));
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        double damage = getTier() == Tiers.NETHERITE ? CommonConfig.NETHERITE_CROWBAR_DAMAGE.get() : CommonConfig.CROWBAR_DAMAGE.get();
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.EMPTY
                .withModifierAdded(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .withModifierAdded(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, CommonConfig.CROWBAR_SPEED.get() - 4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        Item grip = getGripParts(stack);
        if (grip instanceof GripParts gripParts) {
            modifiers = gripParts.applyWeaponModifiers(modifiers, this);
        }
        return modifiers;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack itemstack, @NotNull LivingEntity entity, @NotNull LivingEntity sourceEntity) {
        int amplifier = getTier() == Tiers.NETHERITE ? 1 : 0;
        int time = getTier() == Tiers.NETHERITE ? 100 : 60;
        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, time, amplifier, false, false));
        return super.hurtEnemy(itemstack, entity, sourceEntity);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, TooltipContext context, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.crowbar.description"));
        list.add(Component.translatable("item.zombiekit.modification", ZombieKitKeyMappings.MODIFICATION_GUI.getKey().getDisplayName()));
        if (getTier() != Tiers.NETHERITE)
            list.add(Component.translatable("item.zombiekit.level_limit"));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        if (itemAbility == ItemAbilities.SWORD_SWEEP)
            return false;
        return super.canPerformAction(stack, itemAbility);
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
