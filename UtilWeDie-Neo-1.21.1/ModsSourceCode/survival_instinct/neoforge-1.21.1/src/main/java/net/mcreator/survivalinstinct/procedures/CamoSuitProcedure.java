package net.mcreator.survivalinstinct.procedures;

import javax.annotation.Nullable;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "survival_instinct")
public class CamoSuitProcedure {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        CamoSuitProcedure.execute((Event)event, (Entity)event.getEntity());
    }

    public static void execute(Entity entity) {
        CamoSuitProcedure.execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        block22: {
            LivingEntity _entity;
            block25: {
                ItemStack itemStack;
                ItemStack itemStack2;
                block24: {
                    ItemStack itemStack3;
                    ItemStack itemStack4;
                    block23: {
                        ItemStack itemStack5;
                        ItemStack itemStack6;
                        block21: {
                            ItemStack itemStack7;
                            ItemStack itemStack8;
                            if (entity == null) {
                                return;
                            }
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack8 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack8 = ItemStack.EMPTY;
                            }
                            if (itemStack8.getItem() == SurvivalInstinctModItems.GUILLIE_BOOTS.get()) break block21;
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack7 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack7 = ItemStack.EMPTY;
                            }
                            if (itemStack7.getItem() != SurvivalInstinctModItems.SPRUCE_GUILLIE_BOOTS.get()) break block22;
                        }
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack6 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack6 = ItemStack.EMPTY;
                        }
                        if (itemStack6.getItem() == SurvivalInstinctModItems.GUILLIE_LEGGINGS.get()) break block23;
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack5 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack5 = ItemStack.EMPTY;
                        }
                        if (itemStack5.getItem() != SurvivalInstinctModItems.SPRUCE_GUILLIE_LEGGINGS.get()) break block22;
                    }
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack4 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack4 = ItemStack.EMPTY;
                    }
                    if (itemStack4.getItem() == SurvivalInstinctModItems.GUILLIE_CHESTPLATE.get()) break block24;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack3 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack3 = ItemStack.EMPTY;
                    }
                    if (itemStack3.getItem() != SurvivalInstinctModItems.SPRUCE_GUILLIE_CHESTPLATE.get()) break block22;
                }
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack2 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack2 = ItemStack.EMPTY;
                }
                if (itemStack2.getItem() == SurvivalInstinctModItems.GUILLIE_HELMET.get()) break block25;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack = ItemStack.EMPTY;
                }
                if (itemStack.getItem() != SurvivalInstinctModItems.SPRUCE_GUILLIE_HELMET.get()) break block22;
            }
            if (entity.isShiftKeyDown() && entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                _entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 25, 0, false, false));
            }
        }
    }
}

