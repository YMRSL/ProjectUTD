package net.mcreator.survivalinstinct.procedures;

import javax.annotation.Nullable;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
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
public class HazmatArmorSuitProcedure {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        HazmatArmorSuitProcedure.execute((Event)event, (Entity)event.getEntity());
    }

    public static void execute(Entity entity) {
        HazmatArmorSuitProcedure.execute(null, entity);
    }

    private static void execute(@Nullable Event event, Entity entity) {
        ItemStack itemStack;
        if (entity == null) {
            return;
        }
        if (entity instanceof LivingEntity) {
            LivingEntity _entGetArmor = (LivingEntity)entity;
            itemStack = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
        } else {
            itemStack = ItemStack.EMPTY;
        }
        if (itemStack.getItem() == SurvivalInstinctModItems.HAZMAT_BOOTS.get()) {
            ItemStack itemStack2;
            if (entity instanceof LivingEntity) {
                LivingEntity _entGetArmor = (LivingEntity)entity;
                itemStack2 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
            } else {
                itemStack2 = ItemStack.EMPTY;
            }
            if (itemStack2.getItem() == SurvivalInstinctModItems.HAZMAT_LEGGINGS.get()) {
                ItemStack itemStack3;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack3 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                } else {
                    itemStack3 = ItemStack.EMPTY;
                }
                if (itemStack3.getItem() == SurvivalInstinctModItems.HAZMAT_CHESTPLATE.get()) {
                    ItemStack itemStack4;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack4 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                    } else {
                        itemStack4 = ItemStack.EMPTY;
                    }
                    if (itemStack4.getItem() == SurvivalInstinctModItems.HAZMAT_HELMET.get()) {
                        LivingEntity _entity;
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.POISON);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.WITHER);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.DARKNESS);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.BLINDNESS);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.HUNGER);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.WEAKNESS);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.CONFUSION);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.DIG_SLOWDOWN);
                        }
                        if (entity instanceof LivingEntity) {
                            _entity = (LivingEntity)entity;
                            _entity.removeEffect(MobEffects.LEVITATION);
                        }
                    }
                }
            }
        }
    }
}

