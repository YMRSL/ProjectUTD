package com.scarasol.zombiekit.item.weapon;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.scarasol.sona.item.IRustItem;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitKeyMappings;
import com.scarasol.zombiekit.item.api.DoubleHandWeapon;
import com.scarasol.zombiekit.item.weapon.parts.GripParts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class Rake extends HoeItem implements IRustItem, DoubleHandWeapon {

    private Multimap<Attribute, AttributeModifier> weaponModifiers;

    public Rake(Tier tier, int attackDamage, float speed, Properties properties) {
        super(tier, attackDamage, speed, properties);
    }

    @Override
    public float getAttackDamage() {
        double damage = getTier() == Tiers.NETHERITE ? CommonConfig.NETHERITE_RAKE_DAMAGE.get() : CommonConfig.RAKE_DAMAGE.get();
        return (float) (damage * 0.5);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot equipmentSlot) {
        initModifier();
        return equipmentSlot == EquipmentSlot.MAINHAND ? this.weaponModifiers : super.getDefaultAttributeModifiers(equipmentSlot);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getAttributeModifiers(slot, stack));
            Item grip = getGripParts(stack);
            if (grip instanceof GripParts gripParts) {
                builder.putAll(gripParts.getWeaponModifiers(this));
            }
            return builder.build();
        }
        return super.getAttributeModifiers(slot, stack);
    }

    public void initModifier() {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        double damage = getTier() == Tiers.NETHERITE ? CommonConfig.NETHERITE_RAKE_DAMAGE.get() : CommonConfig.RAKE_DAMAGE.get();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon Modifier", damage - 1, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon Modifier", CommonConfig.RAKE_SPEED.get() - 4, AttributeModifier.Operation.ADDITION));
        double amplifier = getTier() == Tiers.NETHERITE ? CommonConfig.NETHERITE_RAKE_RANGE_INCREASE.get() : CommonConfig.RAKE_RANGE_INCREASE.get();
        builder.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3"), "Rake Modifier", amplifier, AttributeModifier.Operation.MULTIPLY_BASE));
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA4"), "Rake Modifier", amplifier, AttributeModifier.Operation.MULTIPLY_BASE));
        weaponModifiers = builder.build();
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, Level world, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
        list.add(Component.translatable("item.zombiekit.rake.description"));
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
