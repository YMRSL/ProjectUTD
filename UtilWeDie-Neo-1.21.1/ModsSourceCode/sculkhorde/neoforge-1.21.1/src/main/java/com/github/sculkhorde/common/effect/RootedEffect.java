package com.github.sculkhorde.common.effect;

import com.github.sculkhorde.core.ModBlocks;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.ColorUtil;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import java.util.Random;
import java.util.Set;

public class RootedEffect extends MobEffect implements IPotionExpireEffect{

    public static int liquidColor = ColorUtil.hexToInt(ColorUtil.sculkBoneColor1);
    public static MobEffectCategory effectType = MobEffectCategory.HARMFUL;
    public long COOLDOWN = TickUnits.convertSecondsToTicks(1);
    public long cooldownTicksRemaining = COOLDOWN;
    protected Random random = new Random();


    /**
     * Old Dumb Constructor
     * @param effectType Determines if harmful or not
     * @param liquidColor The color in some number format
     */
    protected RootedEffect(MobEffectCategory effectType, int liquidColor) {
        super(effectType, liquidColor);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(SculkHorde.MOD_ID, "rooted_attack_damage"), -0.8F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.MAX_HEALTH, ResourceLocation.fromNamespaceAndPath(SculkHorde.MOD_ID, "rooted_max_health"), -0.5F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.ATTACK_KNOCKBACK, ResourceLocation.fromNamespaceAndPath(SculkHorde.MOD_ID, "rooted_attack_knockback"), -1F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addAttributeModifier(Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(SculkHorde.MOD_ID, "rooted_attack_speed"), -0.5F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    /**
     * Simpler Constructor
     */
    public RootedEffect() {
        this(effectType, liquidColor);
    }


    @Override
    public boolean applyEffectTick(LivingEntity sourceEntity, int amp) {

        if(sourceEntity.level().isClientSide())
        {
            return true;
        }

        if(sourceEntity instanceof ServerPlayer player)
        {
            player.causeFoodExhaustion(10F);
        }

        return true;
    }

    @Override
    public void onPotionExpire(MobEffectEvent.Expired event)
    {
        if(event.getEntity().level().isClientSide()) { return;}

        LivingEntity entity = event.getEntity();
        // OR mob outside of world border
        if(entity == null || EntityAlgorithms.isSculkLivingEntity.test(entity))
        {
            return;
        }

        BlockAlgorithms.setBlockStructure(entity.level(), entity.blockPosition(), ModBlocks.BROOD_NEST_CORE_BLOCK.get().defaultBlockState());
        if(!EntityAlgorithms.isInvalidTargetForSculkHorde(entity))
        {
            entity.hurt(entity.damageSources().magic(), Integer.MAX_VALUE);
        }
    }

    /**
     * A function that is called every tick an entity has this effect. <br>
     * I do not use because it does not provide any useful inputs like
     * the entity it is affecting. <br>
     * I instead use ForgeEventSubscriber.java to handle the logic.
     * @param ticksLeft The amount of ticks remaining
     * @param amplifier The level of the effect
     * @return Determines if the effect should apply.
     */
    @Override
    public boolean shouldApplyEffectTickThisTick(int ticksLeft, int amplifier) {
        if(cooldownTicksRemaining > 0)
        {
            cooldownTicksRemaining--;
            return false;
        }
        cooldownTicksRemaining = COOLDOWN;
        return true;

    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        // Intentionally empty: this effect is not curable.
    }

    public MobEffect addAttributeModifier(Holder<Attribute> attribute, ResourceLocation id, double value, AttributeModifier.Operation operation) {
        return super.addAttributeModifier(attribute, id, value, operation);
    }

}
