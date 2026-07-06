package com.scarasol.zombiekit.item.weapon.parts;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.scarasol.zombiekit.item.api.DoubleHandWeapon;
import com.scarasol.zombiekit.item.api.Parts;
import com.scarasol.zombiekit.item.weapon.SweepWeapon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class GripParts extends Item implements Parts {

    private final int partsLevel;
    private final Material material;
    private final UUID attackDamage = UUID.fromString("398DCA24-D818-EE96-04E8-1FB7A6806016");
    private final UUID attackSpeed = UUID.fromString("06939188-AA3B-5C4F-B50D-3E72E3382673");
    private final UUID attackRange = UUID.fromString("ABF05969-A291-B7EF-45BD-E970D417EA19");

    public GripParts(Properties properties, int partsLevel, Material material) {
        super(properties);
        this.partsLevel = partsLevel;
        this.material = material;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, Level world, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
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

    public Multimap<Attribute, AttributeModifier> getWeaponModifiers(Item item) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(attackDamage, "Grip Modifier", getAttackDamage(), AttributeModifier.Operation.MULTIPLY_BASE));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(attackSpeed, "Grip Modifier", getAttackSpeed(), AttributeModifier.Operation.MULTIPLY_BASE));
        double attackRangeModifier = item instanceof SweepWeapon ? getAttackRange() * 0.5 : getAttackRange();
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(attackRange, "Grip Modifier", attackRangeModifier, AttributeModifier.Operation.MULTIPLY_BASE));
        return builder.build();
    }

    public enum Material {
        LIGHT_WEIGHTED,
        HEAVY_WEIGHTED,
        EXTEND
    }


}
