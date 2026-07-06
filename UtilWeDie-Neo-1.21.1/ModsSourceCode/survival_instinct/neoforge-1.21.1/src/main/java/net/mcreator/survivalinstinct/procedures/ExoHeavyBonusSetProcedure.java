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
public class ExoHeavyBonusSetProcedure {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        ExoHeavyBonusSetProcedure.execute((Event)event, (Entity)event.getEntity());
    }

    public static void execute(Entity entity) {
        ExoHeavyBonusSetProcedure.execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        block67: {
            LivingEntity _entity;
            block70: {
                ItemStack itemStack;
                ItemStack itemStack2;
                ItemStack itemStack3;
                LivingEntity _entGetArmor;
                LivingEntity _entGetArmor2;
                LivingEntity _entGetArmor3;
                block69: {
                    ItemStack itemStack4;
                    ItemStack itemStack5;
                    ItemStack itemStack6;
                    LivingEntity _entGetArmor4;
                    LivingEntity _entGetArmor5;
                    LivingEntity _entGetArmor6;
                    block68: {
                        ItemStack itemStack7;
                        ItemStack itemStack8;
                        ItemStack itemStack9;
                        LivingEntity _entGetArmor7;
                        LivingEntity _entGetArmor8;
                        LivingEntity _entGetArmor9;
                        block66: {
                            ItemStack itemStack10;
                            ItemStack itemStack11;
                            ItemStack itemStack12;
                            LivingEntity _entGetArmor10;
                            LivingEntity _entGetArmor11;
                            LivingEntity _entGetArmor12;
                            block62: {
                                block65: {
                                    ItemStack itemStack13;
                                    ItemStack itemStack14;
                                    ItemStack itemStack15;
                                    block64: {
                                        ItemStack itemStack16;
                                        ItemStack itemStack17;
                                        ItemStack itemStack18;
                                        block63: {
                                            ItemStack itemStack19;
                                            ItemStack itemStack20;
                                            ItemStack itemStack21;
                                            block61: {
                                                ItemStack itemStack22;
                                                ItemStack itemStack23;
                                                ItemStack itemStack24;
                                                if (entity == null) {
                                                    return;
                                                }
                                                if (entity instanceof LivingEntity) {
                                                    _entGetArmor12 = (LivingEntity)entity;
                                                    itemStack24 = _entGetArmor12.getItemBySlot(EquipmentSlot.FEET);
                                                } else {
                                                    itemStack24 = ItemStack.EMPTY;
                                                }
                                                if (itemStack24.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_BOOTS.get()) break block61;
                                                if (entity instanceof LivingEntity) {
                                                    _entGetArmor11 = (LivingEntity)entity;
                                                    itemStack23 = _entGetArmor11.getItemBySlot(EquipmentSlot.FEET);
                                                } else {
                                                    itemStack23 = ItemStack.EMPTY;
                                                }
                                                if (itemStack23.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_BOOTS.get()) break block61;
                                                if (entity instanceof LivingEntity) {
                                                    _entGetArmor10 = (LivingEntity)entity;
                                                    itemStack22 = _entGetArmor10.getItemBySlot(EquipmentSlot.FEET);
                                                } else {
                                                    itemStack22 = ItemStack.EMPTY;
                                                }
                                                if (itemStack22.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_BOOTS.get()) break block62;
                                            }
                                            if (entity instanceof LivingEntity) {
                                                _entGetArmor9 = (LivingEntity)entity;
                                                itemStack21 = _entGetArmor9.getItemBySlot(EquipmentSlot.LEGS);
                                            } else {
                                                itemStack21 = ItemStack.EMPTY;
                                            }
                                            if (itemStack21.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_LEGGINGS.get()) break block63;
                                            if (entity instanceof LivingEntity) {
                                                _entGetArmor8 = (LivingEntity)entity;
                                                itemStack20 = _entGetArmor8.getItemBySlot(EquipmentSlot.LEGS);
                                            } else {
                                                itemStack20 = ItemStack.EMPTY;
                                            }
                                            if (itemStack20.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_LEGGINGS.get()) break block63;
                                            if (entity instanceof LivingEntity) {
                                                _entGetArmor7 = (LivingEntity)entity;
                                                itemStack19 = _entGetArmor7.getItemBySlot(EquipmentSlot.LEGS);
                                            } else {
                                                itemStack19 = ItemStack.EMPTY;
                                            }
                                            if (itemStack19.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_LEGGINGS.get()) break block62;
                                        }
                                        if (entity instanceof LivingEntity) {
                                            _entGetArmor6 = (LivingEntity)entity;
                                            itemStack18 = _entGetArmor6.getItemBySlot(EquipmentSlot.CHEST);
                                        } else {
                                            itemStack18 = ItemStack.EMPTY;
                                        }
                                        if (itemStack18.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_CHESTPLATE.get()) break block64;
                                        if (entity instanceof LivingEntity) {
                                            _entGetArmor5 = (LivingEntity)entity;
                                            itemStack17 = _entGetArmor5.getItemBySlot(EquipmentSlot.CHEST);
                                        } else {
                                            itemStack17 = ItemStack.EMPTY;
                                        }
                                        if (itemStack17.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_CHESTPLATE.get()) break block64;
                                        if (entity instanceof LivingEntity) {
                                            _entGetArmor4 = (LivingEntity)entity;
                                            itemStack16 = _entGetArmor4.getItemBySlot(EquipmentSlot.CHEST);
                                        } else {
                                            itemStack16 = ItemStack.EMPTY;
                                        }
                                        if (itemStack16.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_CHESTPLATE.get()) break block62;
                                    }
                                    if (entity instanceof LivingEntity) {
                                        _entGetArmor3 = (LivingEntity)entity;
                                        itemStack15 = _entGetArmor3.getItemBySlot(EquipmentSlot.HEAD);
                                    } else {
                                        itemStack15 = ItemStack.EMPTY;
                                    }
                                    if (itemStack15.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_HELMET.get()) break block65;
                                    if (entity instanceof LivingEntity) {
                                        _entGetArmor2 = (LivingEntity)entity;
                                        itemStack14 = _entGetArmor2.getItemBySlot(EquipmentSlot.HEAD);
                                    } else {
                                        itemStack14 = ItemStack.EMPTY;
                                    }
                                    if (itemStack14.getItem() == SurvivalInstinctModItems.EXO_HEAVY_GREEN_HELMET.get()) break block65;
                                    if (entity instanceof LivingEntity) {
                                        _entGetArmor = (LivingEntity)entity;
                                        itemStack13 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                                    } else {
                                        itemStack13 = ItemStack.EMPTY;
                                    }
                                    if (itemStack13.getItem() != SurvivalInstinctModItems.EXO_HEAVY_BLACK_HELMET.get()) break block62;
                                }
                                if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                                    _entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 70, 0, true, false));
                                }
                                if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                                    _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 70, 1, true, false));
                                }
                                if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                                    _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 70, 1, true, false));
                                }
                                if (entity instanceof LivingEntity) {
                                    _entity = (LivingEntity)entity;
                                    _entity.removeEffect(MobEffects.BLINDNESS);
                                }
                                if (entity instanceof LivingEntity) {
                                    _entity = (LivingEntity)entity;
                                    _entity.removeEffect(MobEffects.DARKNESS);
                                }
                            }
                            if (entity instanceof LivingEntity) {
                                _entGetArmor12 = (LivingEntity)entity;
                                itemStack12 = _entGetArmor12.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack12 = ItemStack.EMPTY;
                            }
                            if (itemStack12.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_BOOTS.get()) break block66;
                            if (entity instanceof LivingEntity) {
                                _entGetArmor11 = (LivingEntity)entity;
                                itemStack11 = _entGetArmor11.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack11 = ItemStack.EMPTY;
                            }
                            if (itemStack11.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_BOOTS.get()) break block66;
                            if (entity instanceof LivingEntity) {
                                _entGetArmor10 = (LivingEntity)entity;
                                itemStack10 = _entGetArmor10.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack10 = ItemStack.EMPTY;
                            }
                            if (itemStack10.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_BOOTS.get()) break block67;
                        }
                        if (entity instanceof LivingEntity) {
                            _entGetArmor9 = (LivingEntity)entity;
                            itemStack9 = _entGetArmor9.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack9 = ItemStack.EMPTY;
                        }
                        if (itemStack9.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_LEGGINGS.get()) break block68;
                        if (entity instanceof LivingEntity) {
                            _entGetArmor8 = (LivingEntity)entity;
                            itemStack8 = _entGetArmor8.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack8 = ItemStack.EMPTY;
                        }
                        if (itemStack8.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_LEGGINGS.get()) break block68;
                        if (entity instanceof LivingEntity) {
                            _entGetArmor7 = (LivingEntity)entity;
                            itemStack7 = _entGetArmor7.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack7 = ItemStack.EMPTY;
                        }
                        if (itemStack7.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_LEGGINGS.get()) break block67;
                    }
                    if (entity instanceof LivingEntity) {
                        _entGetArmor6 = (LivingEntity)entity;
                        itemStack6 = _entGetArmor6.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack6 = ItemStack.EMPTY;
                    }
                    if (itemStack6.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_CHESTPLATE.get()) break block69;
                    if (entity instanceof LivingEntity) {
                        _entGetArmor5 = (LivingEntity)entity;
                        itemStack5 = _entGetArmor5.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack5 = ItemStack.EMPTY;
                    }
                    if (itemStack5.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_CHESTPLATE.get()) break block69;
                    if (entity instanceof LivingEntity) {
                        _entGetArmor4 = (LivingEntity)entity;
                        itemStack4 = _entGetArmor4.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack4 = ItemStack.EMPTY;
                    }
                    if (itemStack4.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_CHESTPLATE.get()) break block67;
                }
                if (entity instanceof LivingEntity) {
                    _entGetArmor3 = (LivingEntity)entity;
                    itemStack3 = _entGetArmor3.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack3 = ItemStack.EMPTY;
                }
                if (itemStack3.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_HELMET.get()) break block70;
                if (entity instanceof LivingEntity) {
                    _entGetArmor2 = (LivingEntity)entity;
                    itemStack2 = _entGetArmor2.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack2 = ItemStack.EMPTY;
                }
                if (itemStack2.getItem() == SurvivalInstinctModItems.EXO_HEAVY_GREEN_HELMET.get()) break block70;
                if (entity instanceof LivingEntity) {
                    _entGetArmor = (LivingEntity)entity;
                    itemStack = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack = ItemStack.EMPTY;
                }
                if (itemStack.getItem() != SurvivalInstinctModItems.EXO_HEAVY_BLACK_HELMET.get()) break block67;
            }
            if (entity.onGround() && entity.isShiftKeyDown() && entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                _entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 3, true, false));
            }
        }
    }
}

