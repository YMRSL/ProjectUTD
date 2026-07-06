package net.mcreator.survivalinstinct.procedures;

import javax.annotation.Nullable;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
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
public class FireFighterBonusSetProcedure {
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        FireFighterBonusSetProcedure.execute((Event)event, (Entity)event.getEntity());
    }

    public static void execute(Entity entity) {
        FireFighterBonusSetProcedure.execute(null, entity);
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
        if (itemStack.getItem() == SurvivalInstinctModItems.FIRE_FIGHTER_BOOTS.get()) {
            ItemStack itemStack2;
            if (entity instanceof LivingEntity) {
                LivingEntity _entGetArmor = (LivingEntity)entity;
                itemStack2 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
            } else {
                itemStack2 = ItemStack.EMPTY;
            }
            if (itemStack2.getItem() == SurvivalInstinctModItems.FIRE_FIGHTER_LEGGINGS.get()) {
                ItemStack itemStack3;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack3 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                } else {
                    itemStack3 = ItemStack.EMPTY;
                }
                if (itemStack3.getItem() == SurvivalInstinctModItems.FIRE_FIGHTER_CHESTPLATE.get()) {
                    ItemStack itemStack4;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack4 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                    } else {
                        itemStack4 = ItemStack.EMPTY;
                    }
                    if (itemStack4.getItem() == SurvivalInstinctModItems.FIRE_FIGHTER_HELMET.get()) {
                        entity.clearFire();
                    }
                }
            }
        }
    }
}

