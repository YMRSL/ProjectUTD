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
public class ExoesqueletonBonusSetProcedure {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        ExoesqueletonBonusSetProcedure.execute((Event)event, (Entity)event.getEntity());
    }

    public static void execute(Entity entity) {
        ExoesqueletonBonusSetProcedure.execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        ItemStack itemStack;
        LivingEntity _entity;
        LivingEntity _entGetArmor;
        LivingEntity _entGetArmor2;
        LivingEntity _entGetArmor3;
        ItemStack itemStack2;
        LivingEntity _entGetArmor4;
        if (entity == null) {
            return;
        }
        if (entity instanceof LivingEntity) {
            _entGetArmor4 = (LivingEntity)entity;
            itemStack2 = _entGetArmor4.getItemBySlot(EquipmentSlot.FEET);
        } else {
            itemStack2 = ItemStack.EMPTY;
        }
        if (itemStack2.getItem() == SurvivalInstinctModItems.EXO_BOOTS.get()) {
            ItemStack itemStack3;
            if (entity instanceof LivingEntity) {
                _entGetArmor3 = (LivingEntity)entity;
                itemStack3 = _entGetArmor3.getItemBySlot(EquipmentSlot.LEGS);
            } else {
                itemStack3 = ItemStack.EMPTY;
            }
            if (itemStack3.getItem() == SurvivalInstinctModItems.EXO_LEGGINGS.get()) {
                ItemStack itemStack4;
                if (entity instanceof LivingEntity) {
                    _entGetArmor2 = (LivingEntity)entity;
                    itemStack4 = _entGetArmor2.getItemBySlot(EquipmentSlot.CHEST);
                } else {
                    itemStack4 = ItemStack.EMPTY;
                }
                if (itemStack4.getItem() == SurvivalInstinctModItems.EXO_CHESTPLATE.get()) {
                    ItemStack itemStack5;
                    if (entity instanceof LivingEntity) {
                        _entGetArmor = (LivingEntity)entity;
                        itemStack5 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                    } else {
                        itemStack5 = ItemStack.EMPTY;
                    }
                    if (itemStack5.getItem() == SurvivalInstinctModItems.EXO_HELMET.get()) {
                        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                            _entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 70, 0, true, false));
                        }
                        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                            _entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 70, 0, true, false));
                        }
                        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                            _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 0, true, false));
                        }
                    }
                }
            }
        }
        if (entity instanceof LivingEntity) {
            _entGetArmor4 = (LivingEntity)entity;
            itemStack = _entGetArmor4.getItemBySlot(EquipmentSlot.FEET);
        } else {
            itemStack = ItemStack.EMPTY;
        }
        if (itemStack.getItem() == SurvivalInstinctModItems.EXO_BOOTS.get()) {
            ItemStack itemStack6;
            if (entity instanceof LivingEntity) {
                _entGetArmor3 = (LivingEntity)entity;
                itemStack6 = _entGetArmor3.getItemBySlot(EquipmentSlot.LEGS);
            } else {
                itemStack6 = ItemStack.EMPTY;
            }
            if (itemStack6.getItem() == SurvivalInstinctModItems.EXO_LEGGINGS.get()) {
                ItemStack itemStack7;
                if (entity instanceof LivingEntity) {
                    _entGetArmor2 = (LivingEntity)entity;
                    itemStack7 = _entGetArmor2.getItemBySlot(EquipmentSlot.CHEST);
                } else {
                    itemStack7 = ItemStack.EMPTY;
                }
                if (itemStack7.getItem() == SurvivalInstinctModItems.EXO_CHESTPLATE.get()) {
                    ItemStack itemStack8;
                    if (entity instanceof LivingEntity) {
                        _entGetArmor = (LivingEntity)entity;
                        itemStack8 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                    } else {
                        itemStack8 = ItemStack.EMPTY;
                    }
                    if (itemStack8.getItem() == SurvivalInstinctModItems.EXO_HELMET.get() && entity.onGround() && entity.isShiftKeyDown() && entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                        _entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 3, true, false));
                    }
                }
            }
        }
    }
}

