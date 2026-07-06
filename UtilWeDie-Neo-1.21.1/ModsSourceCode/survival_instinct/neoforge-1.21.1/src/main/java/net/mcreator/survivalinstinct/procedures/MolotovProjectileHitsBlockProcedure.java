package net.mcreator.survivalinstinct.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class MolotovProjectileHitsBlockProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world instanceof Level) {
            Level _level = (Level)world;
            if (!_level.isClientSide()) {
                _level.playSound(null, BlockPos.containing((double)x, (double)y, (double)z), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:explote_molotov")), SoundSource.NEUTRAL, 1.0f, 1.0f);
            } else {
                _level.playLocalSound(x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:explote_molotov")), SoundSource.NEUTRAL, 1.0f, 1.0f, false);
            }
        }
        if (world.getBlockState(BlockPos.containing((double)(x + 1.0), (double)(y + 1.0), (double)z)).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x + 1.0), (double)(y + 1.0), (double)z), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)(x - 1.0), (double)(y + 1.0), (double)z)).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x - 1.0), (double)(y + 1.0), (double)z), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)(x + 2.0), (double)(y + 1.0), (double)z)).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x + 2.0), (double)(y + 1.0), (double)z), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)(x - 2.0), (double)(y + 1.0), (double)z)).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x - 2.0), (double)(y + 1.0), (double)z), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z + 1.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z + 1.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z + 2.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z + 2.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z - 1.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z - 1.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z - 2.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)x, (double)(y + 1.0), (double)(z - 2.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)(x + 1.0), (double)(y + 1.0), (double)(z + 1.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x + 1.0), (double)(y + 1.0), (double)(z + 1.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)(x + 1.0), (double)(y + 1.0), (double)(z - 1.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x + 1.0), (double)(y + 1.0), (double)(z - 1.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)(x - 1.0), (double)(y + 1.0), (double)(z - 1.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x - 1.0), (double)(y + 1.0), (double)(z - 1.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)(x - 1.0), (double)(y + 1.0), (double)(z + 1.0))).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)(x - 1.0), (double)(y + 1.0), (double)(z + 1.0)), Blocks.FIRE.defaultBlockState(), 3);
        }
        if (world.getBlockState(BlockPos.containing((double)x, (double)(y + 1.0), (double)z)).getBlock() == Blocks.AIR) {
            world.setBlock(BlockPos.containing((double)x, (double)(y + 1.0), (double)z), Blocks.FIRE.defaultBlockState(), 3);
        }
    }
}

