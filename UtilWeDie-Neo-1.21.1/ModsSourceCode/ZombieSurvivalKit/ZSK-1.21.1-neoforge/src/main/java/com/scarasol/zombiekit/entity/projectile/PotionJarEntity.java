package com.scarasol.zombiekit.entity.projectile;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import com.scarasol.zombiekit.init.ZombieKitItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PotionJarEntity extends ModProjectile {

    private final DustParticleOptions potion = new DustParticleOptions(new Vector3f(Vec3.fromRGB24(50380).toVector3f()), 1.0f);

    public PotionJarEntity(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public PotionJarEntity(EntityType<? extends PotionJarEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
    }

    public PotionJarEntity(EntityType<? extends PotionJarEntity> type, LivingEntity entity, Level world) {
        super(type, entity, world);
    }


    @Override
    public ItemStack getItem() {
        return new ItemStack(ZombieKitItems.POTION_JAR.get());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ZombieKitItems.POTION_JAR.get());
    }

    @Override
    protected void doPostHurtEffects(LivingEntity entity) {
        super.doPostHurtEffects(entity);
        entity.setArrowCount(entity.getArrowCount() - 1);
    }

    public static AbstractArrow shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
        AbstractArrow entityArrow = new PotionJarEntity(ZombieKitEntities.POTION_JAR.get(), entity, world);
        ModProjectile.initProjectileEntity(entityArrow, world, entity, random, power, damage, knockback);
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1, 1f / (random.nextFloat() * 0.5f + 1) + (power / 2));
        return entityArrow;
    }

    public void doEffects(Level level, double x, double y, double z) {
        Vec3 _center = new Vec3(x, y, z);
        List<LivingEntity> _entfound = level.getEntitiesOfClass(LivingEntity.class, new AABB(_center, _center).inflate(7 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
        for (LivingEntity entityiterator : _entfound) {
            if (entityiterator.getType().is(EntityTypeTags.UNDEAD) || CommonConfig.findIndex(BuiltInRegistries.ENTITY_TYPE.getKey(entityiterator.getType()).toString(), CommonConfig.INFECTION_SOURCE_MOB.get()) != -1) {
                entityiterator.addEffect(new MobEffectInstance(SonaMobEffects.INSANE, 400, 0));
            }
        }
        if (level instanceof ServerLevel serverLevel) {
            level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1, 1);
            serverLevel.sendParticles(potion, x, y, z, 1000, 2, 2, 2, 0.005);
        }
    }
}
