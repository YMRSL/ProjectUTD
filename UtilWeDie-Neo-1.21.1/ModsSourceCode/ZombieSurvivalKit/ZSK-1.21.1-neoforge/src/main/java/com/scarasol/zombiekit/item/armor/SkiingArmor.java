package com.scarasol.zombiekit.item.armor;

import com.google.common.collect.Iterables;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SkiingArmor extends ArmorItem {

    public SkiingArmor(Holder<ArmorMaterial> armorMaterial, Type equipmentSlot, Properties properties) {
        super(armorMaterial, equipmentSlot, ModArmorMaterial.applyDurability(armorMaterial, properties, equipmentSlot));
    }

    @Override
    public void inventoryTick(ItemStack itemStack, Level level, Entity entity, int slotIndex, boolean selected){
        super.inventoryTick(itemStack, level, entity, slotIndex, selected);
        if (!(entity instanceof Player player))
            return;
        if (getEquipmentSlot() == EquipmentSlot.HEAD && Iterables.contains(player.getArmorSlots(), itemStack)){
            player.removeEffect(MobEffects.BLINDNESS);
        }
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack itemStack, LivingEntity livingEntity) {
        return getEquipmentSlot() == EquipmentSlot.FEET;
    }

}
