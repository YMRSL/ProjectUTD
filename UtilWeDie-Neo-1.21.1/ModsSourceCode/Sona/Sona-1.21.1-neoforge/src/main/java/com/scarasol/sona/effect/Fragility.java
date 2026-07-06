package com.scarasol.sona.effect;

import com.scarasol.sona.init.SonaMobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class Fragility extends MobEffectBase {

    private static final ResourceLocation FRAGILITY_ID = ResourceLocation.fromNamespaceAndPath("sona", "fragility_armor");

    public Fragility() {
        super(MobEffectCategory.HARMFUL, -7864320);
    }

    /**
     * 1.21: attribute mutation that depends on the entity moves from the old
     * {@code addAttributeModifiers(LivingEntity, AttributeMap, int)} to {@code onEffectStarted}.
     * The reduction is non-linear in the amplifier so it can't use the template-based
     * {@code addAttributeModifier}; we apply it manually as a permanent modifier and strip it on removal.
     */
    @Override
    public void onEffectStarted(@NotNull LivingEntity entity, int amplifier) {
        double level = Math.max(entity.hasEffect(SonaMobEffects.FRAGILITY) ? entity.getEffect(SonaMobEffects.FRAGILITY).getAmplifier() : 0, amplifier) + 1;
        double addition = -1 * ((0.15 * level) / (0.15 * level + 1)) * 20;
        AttributeModifier attributeModifier = new AttributeModifier(FRAGILITY_ID, addition, AttributeModifier.Operation.ADD_VALUE);
        AttributeInstance attributeInstance = entity.getAttributes().getInstance(Attributes.ARMOR);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(FRAGILITY_ID);
            attributeInstance.addPermanentModifier(attributeModifier);
        }
    }

    @Override
    public void removeAttributeModifiers(@NotNull AttributeMap attributeMap) {
        super.removeAttributeModifiers(attributeMap);
        AttributeInstance attributeInstance = attributeMap.getInstance(Attributes.ARMOR);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(FRAGILITY_ID);
        }
    }

}
