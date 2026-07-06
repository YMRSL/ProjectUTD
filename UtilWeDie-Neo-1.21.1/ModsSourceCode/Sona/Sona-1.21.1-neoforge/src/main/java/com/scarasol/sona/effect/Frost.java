package com.scarasol.sona.effect;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.init.SonaMobEffects;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class Frost extends PhysicalEffect {

    public Frost() {
        super(MobEffectCategory.HARMFUL, -16711681);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) {
            entity.removeEffect(SonaMobEffects.FROST);
        } else {
            if (entity.hasEffect(SonaMobEffects.IGNITION)) {
                int level = entity.getEffect(SonaMobEffects.IGNITION).getAmplifier();
                int duration = entity.getEffect(SonaMobEffects.IGNITION).getDuration();
                entity.removeEffect(SonaMobEffects.IGNITION);
                entity.addEffect(new MobEffectInstance(SonaMobEffects.FRAGILITY, duration, (level + amplifier) / 2, false, false));
            }
        }
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.isOnFire()) {
            entity.setTicksFrozen(0);
            int duration = entity.getEffect(SonaMobEffects.FROST).getDuration();
            entity.removeEffect(SonaMobEffects.FROST);
            entity.addEffect(new MobEffectInstance(SonaMobEffects.FRAGILITY, duration, amplifier, false, false));
            return true;
        }
        float freezeImmune = equipmentFreezeImmune(entity);
        entity.setTicksFrozen(entity.getTicksFrozen() + Math.round((amplifier + 1) * freezeImmune + 2));
        if (entity.isFullyFrozen()) {
            int frozenTime = entity.getEffect(SonaMobEffects.FROST).getDuration();
            if (frozenTime % 10 == 0) {
                if (CommonConfig.OVER_DOT.get())
                    entity.invulnerableTime = 0;
                entity.hurt(entity.level().damageSources().freeze(), (entity.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES) ? (5 + amplifier) / 2 : (1 + amplifier) / 2));
            }
        }
        if (entity.hasEffect(SonaMobEffects.SLIMINESS)) {
            if (freezeImmune != 0 && !entity.isFullyFrozen()) {
                entity.setTicksFrozen(entity.getTicksRequiredToFreeze());
            }
        }
        return true;
    }

    public float equipmentFreezeImmune(LivingEntity entity) {
        float exposed = 0;
        ItemStack[] armors = {entity.getItemBySlot(EquipmentSlot.HEAD), entity.getItemBySlot(EquipmentSlot.CHEST), entity.getItemBySlot(EquipmentSlot.LEGS), entity.getItemBySlot(EquipmentSlot.FEET)};
        for (ItemStack armor : armors) {
            if (armor.isEmpty()) {
                exposed += 0.25;
            } else if (!armor.is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                exposed += 0.125;
            }
        }
        return exposed;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

}
