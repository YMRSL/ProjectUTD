package com.scarasol.zombiekit.item.bonus;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Aokinao extends BonusItem{
    public Aokinao(Properties properties) {
        super(properties);
    }

    @Override
    public void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex) {
        super.onInventoryTick(stack, level, player, slotIndex, selectedIndex);
        if (selectedIndex == slotIndex && level instanceof ServerLevel serverLevel) {
            RandomSource randomSource = level.random;
            if (randomSource.nextDouble() < 0.2) {
                double x = player.getX() + randomSource.nextGaussian();
                double y = player.getY() + 3;
                double z = player.getZ() + randomSource.nextGaussian();
                serverLevel.getPlayers(serverPlayer -> player.distanceTo(serverPlayer) < 60)
                        .forEach(serverPlayer -> serverLevel.sendParticles(serverPlayer, ParticleTypes.SNOWFLAKE, true, x, y, z, 0, randomSource.nextGaussian() * 0.08, -0.2, randomSource.nextGaussian() * 0.08, 0.2));
            }
        }
    }
}
