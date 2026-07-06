package net.mcreator.survivalinstinct.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

public class HomemadeBombProyectileProjectileExplosionProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        Projectile _entityToSpawn;
        Entity _shootFrom;
        Level projectileLevel;
        Level _level;
        if (entity == null) {
            return;
        }
        if (world instanceof Level && !(_level = (Level)world).isClientSide()) {
            _level.explode(null, x, y, z, 3.0f, Level.ExplosionInteraction.TNT);
        }
        if (!(projectileLevel = (_shootFrom = entity).level()).isClientSide()) {
            _entityToSpawn = new Object(){

                public Projectile getArrow(Level level, float damage, int knockback) {
                    Arrow entityToSpawn = new Arrow(EntityType.ARROW, level);
                    entityToSpawn.setBaseDamage((double)damage);
                    return entityToSpawn;
                }
            }.getArrow(projectileLevel, 5.0f, 0);
            _entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
            _entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 1.0f, 0.0f);
            projectileLevel.addFreshEntity((Entity)_entityToSpawn);
        }
        if (!(projectileLevel = (_shootFrom = entity).level()).isClientSide()) {
            _entityToSpawn = new Object(){

                public Projectile getArrow(Level level, float damage, int knockback) {
                    Arrow entityToSpawn = new Arrow(EntityType.ARROW, level);
                    entityToSpawn.setBaseDamage((double)damage);
                    return entityToSpawn;
                }
            }.getArrow(projectileLevel, 5.0f, 0);
            _entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
            _entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 1.0f, 0.0f);
            projectileLevel.addFreshEntity((Entity)_entityToSpawn);
        }
        if (!(projectileLevel = (_shootFrom = entity).level()).isClientSide()) {
            _entityToSpawn = new Object(){

                public Projectile getArrow(Level level, float damage, int knockback) {
                    Arrow entityToSpawn = new Arrow(EntityType.ARROW, level);
                    entityToSpawn.setBaseDamage((double)damage);
                    return entityToSpawn;
                }
            }.getArrow(projectileLevel, 5.0f, 0);
            _entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
            _entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 1.0f, 0.0f);
            projectileLevel.addFreshEntity((Entity)_entityToSpawn);
        }
        if (!(projectileLevel = (_shootFrom = entity).level()).isClientSide()) {
            _entityToSpawn = new Object(){

                public Projectile getArrow(Level level, float damage, int knockback) {
                    Arrow entityToSpawn = new Arrow(EntityType.ARROW, level);
                    entityToSpawn.setBaseDamage((double)damage);
                    return entityToSpawn;
                }
            }.getArrow(projectileLevel, 5.0f, 0);
            _entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
            _entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 1.0f, 0.0f);
            projectileLevel.addFreshEntity((Entity)_entityToSpawn);
        }
    }
}

