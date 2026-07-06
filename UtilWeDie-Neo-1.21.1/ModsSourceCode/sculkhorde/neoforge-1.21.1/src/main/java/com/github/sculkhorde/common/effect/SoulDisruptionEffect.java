package com.github.sculkhorde.common.effect;

import com.github.sculkhorde.util.ColorUtil;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;

import java.util.Set;

public class SoulDisruptionEffect extends MobEffect {

    public static int liquidColor = ColorUtil.hexToRGB("29DFEB");
    public static MobEffectCategory effectType = MobEffectCategory.NEUTRAL;
    public long COOLDOWN = TickUnits.convertSecondsToTicks(2);
    public long cooldownTicksRemaining = COOLDOWN;


    /**
     * Old Dumb Constructor
     * @param effectType Determines if harmful or not
     * @param liquidColor The color in some number format
     */
    protected SoulDisruptionEffect(MobEffectCategory effectType, int liquidColor) {
        super(effectType, liquidColor);
    }

    /**
     * Simpler Constructor
     */
    public SoulDisruptionEffect() {
        this(effectType, liquidColor);
    }


    @Override
    public boolean applyEffectTick(LivingEntity entity, int amp) {

        if(entity.level().isClientSide()) { return true;}
        // IF entity has a sculk infection, remove it

        return true;
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

}
