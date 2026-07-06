package com.scarasol.zombiekit.entity.projectile;

import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.api.NoAttenuationProjectile;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.data.LaunchSchedule;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import com.scarasol.zombiekit.init.ZombieKitParticleTypes;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Random;


public class MortarShellEntity extends ModProjectile implements ItemSupplier, NoAttenuationProjectile, GeoEntity {

    @Nullable
    private LaunchSchedule launchSchedule;
    @Nullable
    private ChunkPos currentChunkPos;

    private final ShellEffect effect;

    private boolean firstHit;
    private int lifeTime;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public static final ShellEffect DEFAULT_EFFECT = (shellEntity) ->{
        Level.ExplosionInteraction explosionInteraction = CommonConfig.MORTAR_BREAK_BLOCK.get() ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
        shellEntity.level().explode(shellEntity, shellEntity.getX(), shellEntity.getY(), shellEntity.getZ(),
                CommonConfig.MORTAR_EXPLOSION_LEVEL.get(), explosionInteraction);
                shellEntity.discard();
    };

    public MortarShellEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ZombieKitEntities.MORTAR_SHELL.get(), world, (shell) -> {});
    }

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, Level world) {
        this(type, world, DEFAULT_EFFECT);
    }


    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, LivingEntity owner, Level world, ShellEffect shellEffect) {
        super(type, owner, world);
        this.effect = shellEffect;
        this.noGround = false;
    }

    public MortarShellEntity(EntityType<? extends MortarShellEntity> type, Level world, ShellEffect shellEffect) {
        super(type, world);
        this.effect = shellEffect;
        this.noGround = false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains("LifeTime"))
            lifeTime = compoundTag.getInt("LifeTime");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("LifeTime", lifeTime);
    }

    public void mineLaying() {
        if (this.level() instanceof ServerLevel serverLevel) {
            Random random = new Random();
            serverLevel.explode(this, getX(), getY(), getZ(), 1, Level.ExplosionInteraction.NONE);
            for (int i = 0; i < 8; i++) {
                Vec3 angle = new Vec3(random.nextGaussian(), Math.abs(random.nextGaussian()), random.nextGaussian()).normalize();
                AbstractArrow landmine = new LandmineEntity(ZombieKitEntities.LANDMINE.get(), serverLevel);
                landmine.setPos(this.position());
                ModProjectile.initProjectileEntity(landmine, serverLevel, angle, serverLevel.random, 0.3f, 0, 0);
            }
        }
        this.discard();
    }

    public void burn() {
        Level level = level();
        MolotovCocktailEntity.burn(level, getX(), getY(), getZ(), CommonConfig.NAPALM_RANGE.get());
        level.explode(this, getX(), getY(), getZ(), 1, Level.ExplosionInteraction.NONE);
        level.getEntitiesOfClass(LivingEntity.class, new AABB(getOnPos()).inflate(CommonConfig.NAPALM_RANGE.get()), (target) -> !target.fireImmune())
                .forEach((target) -> {
                    target.hurt(level.damageSources().inFire(), CommonConfig.NAPALM_DAMAGE.get());
                    int time = CommonConfig.NAPALM_BURN_TIME.get() * 20;
                    target.addEffect(new MobEffectInstance(SonaMobEffects.IGNITION.get(), time, 3, false, false));
                    target.addEffect(new MobEffectInstance(SonaMobEffects.SLIMINESS.get(), time, 0, false, false));
                });
        this.discard();
    }

    public void smoke() {
        if (level() instanceof ServerLevel serverLevel) {
            if (!firstHit) {
                serverLevel.explode(this, getX(), getY(), getZ(), 1, Level.ExplosionInteraction.NONE);
                Random random = new Random();
                for (int i = 0; i < 20; i++) {
                    double x = getX() + random.nextDouble(-8, 8);
                    double y;
                    double z = getZ() + random.nextDouble(-8, 8);
                    BlockPos spawnPos = serverLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, getY(), z));
                    if (Math.abs(spawnPos.getY() - getY()) < 4) {
                        y = spawnPos.getY() + 1;
                    }else {
                        y = getY();
                    }
                    serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(getOnPos()).inflate(10D), (target) -> !target.fireImmune() && target.hasLineOfSight(this))
                            .forEach((target) -> {
                                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 600, 0, false, false));
                            });
                    serverLevel.getPlayers((player) -> true).forEach (
                            player -> serverLevel.sendParticles(player, (SimpleParticleType)ZombieKitParticleTypes.LARGE_SMOKE.get(), true, x, y, z, 1, 0, 0, 0, 0)
                    );
                }
                firstHit = true;
            }else {
                level().getEntitiesOfClass(LivingEntity.class, new AABB(getOnPos()).inflate(10D))
                        .forEach((target) -> {
                            target.addEffect(new MobEffectInstance(SonaMobEffects.CAMOUFLAGE.get(), 20, 4, false, false));
                            if (!(target instanceof Player))
                                target.addEffect(new MobEffectInstance(SonaMobEffects.CONFUSION.get(), 20, 0, false, false));
                        });
                if (lifeTime++ > 1000) {
                    this.discard();
                }
            }
        }
    }

    @Nullable
    public LaunchSchedule getLaunchSchedule() {
        return launchSchedule;
    }

    public void setLaunchSchedule(@Nullable LaunchSchedule launchSchedule) {
        this.launchSchedule = launchSchedule;
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        updateForceRegion();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        super.tick();
        updateForceRegion();
        if (inGround && firstHit)
            effect.shellEffect(this);
    }

    @Override
    public void doEffects(Level level, double x, double y, double z) {
        if (level() instanceof ServerLevel serverLevel) {
            if (this.launchSchedule != null) {
                this.launchSchedule.completeSchedule(serverLevel);
            } else if (currentChunkPos != null)
                serverLevel.getChunkSource().chunkMap.getDistanceManager().removeRegionTicket(TicketType.FORCED, currentChunkPos, 3, currentChunkPos);
            effect.shellEffect(this);
        }
    }

    public void updateForceRegion() {
        ChunkPos pos = new ChunkPos(getOnPos());
        if (launchSchedule == null && !pos.equals(currentChunkPos) && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().chunkMap.getDistanceManager().addRegionTicket(TicketType.FORCED, pos, 3, pos);
            if (currentChunkPos != null ){
                serverLevel.getChunkSource().chunkMap.getDistanceManager().removeRegionTicket(TicketType.FORCED, currentChunkPos, 3, currentChunkPos);
            }
            currentChunkPos = pos;
        }
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        super.remove(reason);
        if (reason == RemovalReason.DISCARDED || reason == RemovalReason.KILLED)
            if (currentChunkPos != null && this.level() instanceof ServerLevel serverLevel)
                serverLevel.getChunkSource().chunkMap.getDistanceManager().removeRegionTicket(TicketType.FORCED, currentChunkPos, 3, currentChunkPos);
    }

    public static MortarShellEntity shoot(Level world, LivingEntity entity, double x, double y, double z, RandomSource random, float power, @Nullable LaunchSchedule launchSchedule, ShellEffect effect) {
        MortarShellEntity shell = new MortarShellEntity(ZombieKitEntities.MORTAR_SHELL.get(), entity, world, effect);
        shell.setPos(entity.position().add(entity.getLookAngle().scale(2.0)));
        shell.shoot(x, y, z, power, 0);
        shell.setSilent(true);
        shell.setCritArrow(false);
        shell.setBaseDamage(1);
        shell.setKnockback(0);
        shell.setLaunchSchedule(launchSchedule);
        world.addFreshEntity(shell);
        return shell;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    @FunctionalInterface
    public interface ShellEffect {
        void shellEffect(MortarShellEntity shellEntity);
    }
}
