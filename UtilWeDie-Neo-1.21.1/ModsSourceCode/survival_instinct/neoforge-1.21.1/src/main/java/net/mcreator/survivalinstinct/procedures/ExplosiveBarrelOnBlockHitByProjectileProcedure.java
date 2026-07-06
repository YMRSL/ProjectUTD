package net.mcreator.survivalinstinct.procedures;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class ExplosiveBarrelOnBlockHitByProjectileProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        Level _level;
        if (world instanceof Level && !(_level = (Level)world).isClientSide()) {
            _level.explode(null, x, y, z, 5.0f, Level.ExplosionInteraction.TNT);
        }
    }
}

