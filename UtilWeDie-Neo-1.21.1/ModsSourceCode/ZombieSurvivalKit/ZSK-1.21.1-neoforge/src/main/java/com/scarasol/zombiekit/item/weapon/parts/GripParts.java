package com.scarasol.zombiekit.item.weapon.parts;

import com.scarasol.zombiekit.item.api.Parts;
import com.scarasol.zombiekit.item.weapon.SweepWeapon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GripParts extends Item implements Parts {

    private final int partsLevel;
    private final Material material;
    private static final ResourceLocation ATTACK_DAMAGE_ID = ResourceLocation.fromNamespaceAndPath("zombiekit", "grip_attack_damage");
    private static final ResourceLocation ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("zombiekit", "grip_attack_speed");
    private static final ResourceLocation ATTACK_RANGE_ID = ResourceLocation.fromNamespaceAndPath("zombiekit", "grip_attack_range");

    public GripParts(Properties properties, int partsLevel, Material material) {
        super(properties);
        this.partsLevel = partsLevel;
        this.material = material;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, TooltipContext context, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.general_parts.description"));
        if (material == Material.LIGHT_WEIGHTED)
            list.add(Component.translatable("item.zombiekit.light_weighted_parts.description_" + (getPartsLevel() + 1)));
        else if (material == Material.HEAVY_WEIGHTED)
            list.add(Component.translatable("item.zombiekit.heavy_weighted_parts.description_" + (getPartsLevel() + 1)));
        else
            list.add(Component.translatable("item.zombiekit.extend_weighted_parts.description_" + (getPartsLevel() + 1)));
    }

    @Override
    public int getPartsLevel() {
        return this.partsLevel;
    }

    @Override
    public PartsType getPartsType() {
        return PartsType.GRIP;
    }

    public static boolean unlock(ItemStack itemStack) {
        return true;
    }

    @Override
    public boolean canUse(ItemStack itemStack) {
        if (itemStack.getItem() instanceof TieredItem tieredItem) {
            if (tieredItem.getTier() instanceof Tiers tiers) {
                if (tiers == Tiers.NETHERITE)
                    return true;
                return partsLevel < 2;
            }
        }
        return false;
    }

    public double getAttackSpeed() {
        if (material == Material.LIGHT_WEIGHTED) {
            if (this.partsLevel == 0)
                return 0.1;
            else if (this.partsLevel == 1)
                return 0.2;
            else
                return 0.3;
        }else if (material == Material.EXTEND) return -0.15;
        return -0.1;
    }

    public double getAttackDamage() {
        if (material == Material.HEAVY_WEIGHTED) {
            if (this.partsLevel == 0)
                return 0.1;
            else if (this.partsLevel == 1)
                return 0.15;
            else
                return 0.2;
        }else if (material == Material.EXTEND) return -0.1;
        return -0.05;
    }

    public double getSweepRange() {
        if (this.material == Material.EXTEND) {
            if (this.partsLevel == 0)
                return 1.2;
            else if (this.partsLevel == 1)
                return 1.35;
            else
                return 1.5;
        }
        return 0;
    }

    public float getDurabilityModifier() {
        if (this.partsLevel == 0)
            return 1.2f;
        if (this.partsLevel == 1)
            return 1.4f;
        else
            return 1.6f;
    }

    private double getAttackRange() {
        if (this.material == Material.EXTEND) {
            if (this.partsLevel == 0)
                return 0.1;
            else if (this.partsLevel == 1)
                return 0.175;
            else
                return 0.25;
        }
        return 0;
    }

    public ItemAttributeModifiers applyWeaponModifiers(ItemAttributeModifiers modifiers, Item item) {
        double attackRangeModifier = item instanceof SweepWeapon ? getAttackRange() * 0.5 : getAttackRange();
        return modifiers
                .withModifierAdded(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_ID, getAttackDamage(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .withModifierAdded(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_ID, getAttackSpeed(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(ATTACK_RANGE_ID, attackRangeModifier, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND);
    }

    public enum Material {
        LIGHT_WEIGHTED,
        HEAVY_WEIGHTED,
        EXTEND
    }


}
