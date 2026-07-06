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
public class JuggernautArmorSetBonusProcedure {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        JuggernautArmorSetBonusProcedure.execute((Event)event, (Entity)event.getEntity());
    }

    public static void execute(Entity entity) {
        JuggernautArmorSetBonusProcedure.execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        block31: {
            LivingEntity _entity;
            block34: {
                ItemStack itemStack;
                ItemStack itemStack2;
                ItemStack itemStack3;
                block33: {
                    ItemStack itemStack4;
                    ItemStack itemStack5;
                    ItemStack itemStack6;
                    block32: {
                        ItemStack itemStack7;
                        ItemStack itemStack8;
                        ItemStack itemStack9;
                        block30: {
                            ItemStack itemStack10;
                            ItemStack itemStack11;
                            ItemStack itemStack12;
                            if (entity == null) {
                                return;
                            }
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack12 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack12 = ItemStack.EMPTY;
                            }
                            if (itemStack12.getItem() == SurvivalInstinctModItems.GREEN_JUGGERNAUT_BOOTS.get()) break block30;
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack11 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack11 = ItemStack.EMPTY;
                            }
                            if (itemStack11.getItem() == SurvivalInstinctModItems.DESERT_JUGGERNAUT_BOOTS.get()) break block30;
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack10 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack10 = ItemStack.EMPTY;
                            }
                            if (itemStack10.getItem() != SurvivalInstinctModItems.BLACK_JUGGERNAUT_BOOTS.get()) break block31;
                        }
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack9 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack9 = ItemStack.EMPTY;
                        }
                        if (itemStack9.getItem() == SurvivalInstinctModItems.GREEN_JUGGERNAUT_LEGGINGS.get()) break block32;
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack8 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack8 = ItemStack.EMPTY;
                        }
                        if (itemStack8.getItem() == SurvivalInstinctModItems.DESERT_JUGGERNAUT_LEGGINGS.get()) break block32;
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack7 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack7 = ItemStack.EMPTY;
                        }
                        if (itemStack7.getItem() != SurvivalInstinctModItems.BLACK_JUGGERNAUT_LEGGINGS.get()) break block31;
                    }
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack6 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack6 = ItemStack.EMPTY;
                    }
                    if (itemStack6.getItem() == SurvivalInstinctModItems.GREEN_JUGGERNAUT_CHESTPLATE.get()) break block33;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack5 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack5 = ItemStack.EMPTY;
                    }
                    if (itemStack5.getItem() == SurvivalInstinctModItems.DESERT_JUGGERNAUT_CHESTPLATE.get()) break block33;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack4 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack4 = ItemStack.EMPTY;
                    }
                    if (itemStack4.getItem() != SurvivalInstinctModItems.BLACK_JUGGERNAUT_CHESTPLATE.get()) break block31;
                }
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack3 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack3 = ItemStack.EMPTY;
                }
                if (itemStack3.getItem() == SurvivalInstinctModItems.GREEN_JUGGERNAUT_HELMET.get()) break block34;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack2 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack2 = ItemStack.EMPTY;
                }
                if (itemStack2.getItem() == SurvivalInstinctModItems.DESERT_JUGGERNAUT_HELMET.get()) break block34;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack = ItemStack.EMPTY;
                }
                if (itemStack.getItem() != SurvivalInstinctModItems.BLACK_JUGGERNAUT_HELMET.get()) break block31;
            }
            if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, false, false));
            }
            if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 0, false, false));
            }
        }
    }
}

