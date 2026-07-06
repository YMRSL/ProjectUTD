package net.mcreator.survivalinstinct.procedures;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModBlocks;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class BearTrapEntityWalksOnTheBlockProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        LivingEntity _entity;
        if (entity == null) {
            return;
        }
        world.setBlock(BlockPos.containing((double)x, (double)y, (double)z), ((Block)SurvivalInstinctModBlocks.BEAR_TRAP_CLOSE.get()).defaultBlockState(), 3);
        if (world instanceof Level) {
            Level _level = (Level)world;
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing((double)x, (double)y, (double)z), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:bear_trap_close")), SoundSource.NEUTRAL, 1.0f, 1.0f);
            } else {
                _level.playLocalSound(x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:bear_trap_close")), SoundSource.NEUTRAL, 1.0f, 1.0f, false);
            }
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 99, false, false));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(SurvivalInstinctModMobEffects.BLEEDING, 1200, 0, true, true));
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 1, true, true));
        }
        entity.hurt(new DamageSource((Holder)world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 3.0f);
    }
}

