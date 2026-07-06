package net.mcreator.survivalinstinct.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class RopeTrapEnitityProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        LivingEntity _entity;
        if (entity == null) {
            return;
        }
        if (world instanceof Level) {
            Level _level = (Level)world;
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing((double)x, (double)y, (double)z), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.break")), SoundSource.NEUTRAL, 1.0f, 1.0f);
            } else {
                _level.playLocalSound(x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("block.wood.break")), SoundSource.NEUTRAL, 1.0f, 1.0f, false);
            }
        }
        if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
            _entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 320, 2, true, true));
        }
        world.destroyBlock(BlockPos.containing((double)x, (double)y, (double)z), false);
    }
}

