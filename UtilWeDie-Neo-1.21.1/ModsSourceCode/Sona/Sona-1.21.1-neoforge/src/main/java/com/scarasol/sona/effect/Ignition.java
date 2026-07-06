package com.scarasol.sona.effect;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.init.SonaMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class Ignition extends PhysicalEffect {

    public Ignition() {
        super(MobEffectCategory.HARMFUL, -6750208);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.fireImmune()) {
            entity.removeEffect(SonaMobEffects.IGNITION);
        } else {
            if (entity.hasEffect(SonaMobEffects.FROST)) {
                int level = entity.getEffect(SonaMobEffects.FROST).getAmplifier();
                int duration = entity.getEffect(SonaMobEffects.FROST).getDuration();
                entity.removeEffect(SonaMobEffects.FROST);
                entity.setTicksFrozen(0);
                entity.addEffect(new MobEffectInstance(SonaMobEffects.FRAGILITY, duration, (level + amplifier) / 2, false, false));
            }
        }
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        int ignitedTime = entity.getEffect(SonaMobEffects.IGNITION).getDuration();
        boolean burnUnderWater = entity.hasEffect(SonaMobEffects.SLIMINESS);
        if (entity.isInWaterRainOrBubble()) {
            if (burnUnderWater) {
                entity.setSharedFlagOnFire(false);
                if (entity.level() instanceof ServerLevel server && ignitedTime % 10 == 0) {
                    server.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY(), entity.getZ(), 20, 0, 0.8, 0, 0.01);
                }
            } else {
                entity.removeEffect(SonaMobEffects.IGNITION);
                return true;
            }
        } else if (entity.isFreezing()) {
            if (burnUnderWater) {
                breakPowderSnow(entity.level(), BlockPos.containing(entity.getX(), entity.getY(), entity.getZ()));
                breakPowderSnow(entity.level(), BlockPos.containing(entity.getX(), entity.getY() + 1, entity.getZ()));
            } else {
                entity.addEffect(new MobEffectInstance(SonaMobEffects.FRAGILITY, ignitedTime, amplifier, false, false));
                entity.removeEffect(SonaMobEffects.IGNITION);
                return true;
            }
        } else {
            entity.setRemainingFireTicks(5);
            entity.setSharedFlagOnFire(!entity.fireImmune());
        }
        if (ignitedTime % 20 == 0) {
            if (CommonConfig.OVER_DOT.get())
                entity.invulnerableTime = 0;
            entity.hurt(entity.level().damageSources().inFire(), amplifier + 1);
        }
        return true;
    }

    public void breakPowderSnow(Level world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() == Blocks.POWDER_SNOW) {
            world.destroyBlock(pos, false);
        }
    }


    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

}
