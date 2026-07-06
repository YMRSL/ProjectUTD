package net.mcreator.survivalinstinct.procedures;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class GreenNightVisionGUIDisplayOverlayIngameProcedure {
    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static boolean execute(Entity entity) {
        ItemStack itemStack;
        ItemStack itemStack2;
        ItemStack itemStack3;
        ItemStack itemStack4;
        if (entity == null) {
            return false;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity _entGetArmor = (LivingEntity)entity;
            itemStack4 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
        } else {
            itemStack4 = ItemStack.EMPTY;
        }
        if (itemStack4.getItem() == SurvivalInstinctModItems.NIGHT_VISION_GOGGLES_HELMET.get()) return true;
        if (entity instanceof LivingEntity) {
            LivingEntity _entGetArmor = (LivingEntity)entity;
            itemStack3 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
        } else {
            itemStack3 = ItemStack.EMPTY;
        }
        if (itemStack3.getItem() == SurvivalInstinctModItems.GREEN_HUNTER_HELMET.get()) return true;
        if (entity instanceof LivingEntity) {
            LivingEntity _entGetArmor = (LivingEntity)entity;
            itemStack2 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
        } else {
            itemStack2 = ItemStack.EMPTY;
        }
        if (itemStack2.getItem() == SurvivalInstinctModItems.DESERT_HUNTER_HELMET.get()) return true;
        if (entity instanceof LivingEntity) {
            LivingEntity _entGetArmor = (LivingEntity)entity;
            itemStack = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
        } else {
            itemStack = ItemStack.EMPTY;
        }
        if (itemStack.getItem() != SurvivalInstinctModItems.BLACK_HUNTER_HELMET.get()) return false;
        return true;
    }
}

