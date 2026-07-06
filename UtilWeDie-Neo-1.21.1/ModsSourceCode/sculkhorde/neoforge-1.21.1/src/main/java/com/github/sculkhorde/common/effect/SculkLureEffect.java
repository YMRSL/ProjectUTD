package com.github.sculkhorde.common.effect;

import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.ModMobEffects;
import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.systems.event_system.events.GhastDeploymentEvent;
import com.github.sculkhorde.util.DifficultyUtil;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.MobProfileUtil;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Set;

public class SculkLureEffect extends MobEffect {

    public static int liquidColor = 338997;
    public static MobEffectCategory effectType = MobEffectCategory.HARMFUL;
    public long cooldownTicksRemaining = 0;


    /**
     * Old Dumb Constructor
     * @param effectType Determines if harmful or not
     * @param liquidColor The color in some number format
     */
    protected SculkLureEffect(MobEffectCategory effectType, int liquidColor) {
        super(effectType, liquidColor);
    }

    /**
     * Simpler Constructor
     */
    public SculkLureEffect() {
        this(effectType, liquidColor);
    }

    public long getCooldownBasedOnDifficulty()
    {
        if(ServerLifecycleHooks.getCurrentServer() == null)
        {
            return TickUnits.convertMinutesToTicks(1);
        }

        else if(DifficultyUtil.isCurrentDifficultyEasy())
        {
            return TickUnits.convertMinutesToTicks(5);
        }
        else if(DifficultyUtil.isCurrentDifficultyNormal())
        {
            return TickUnits.convertMinutesToTicks(2);
        }
        else
        {
            return TickUnits.convertMinutesToTicks(1);
        }
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int p_19468_) {

        if(entity.level().isClientSide()) { return true;}
        if(EntityAlgorithms.isSculkLivingEntity.test(entity))
        {
            // Remove effect
            entity.removeEffect(ModMobEffects.SCULK_LURE);
            return true;
        }

        if(ModSavedData.getSaveData() != null) { ModSavedData.getSaveData().reportDeath((ServerLevel) entity.level(), entity.blockPosition()); }


        GhastDeploymentEvent.trySendGhastDepolymentEvent(entity);


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
        cooldownTicksRemaining = getCooldownBasedOnDifficulty();
        return true;

    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance effectInstance) {
        // Intentionally empty: this effect is not curable.
    }

}
