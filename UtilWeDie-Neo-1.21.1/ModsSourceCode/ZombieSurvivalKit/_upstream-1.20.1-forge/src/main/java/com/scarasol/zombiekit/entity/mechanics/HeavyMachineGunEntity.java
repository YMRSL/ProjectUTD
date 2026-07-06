package com.scarasol.zombiekit.entity.mechanics;

import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.sona.util.SonaMath;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.api.FixedVehicle;
import com.scarasol.zombiekit.entity.projectile.HeavyMachineGunAmmoEntity;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.init.ZombieKitTags;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import net.minecraft.client.CameraType;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;


import java.util.List;
import java.util.UUID;

public class HeavyMachineGunEntity extends Mechanics implements GeoEntity, FixedVehicle {
    public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(HeavyMachineGunEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> OVERLOAD = SynchedEntityData.defineId(HeavyMachineGunEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(HeavyMachineGunEntity.class, EntityDataSerializers.STRING);
    public static final AttributeModifier ATTRIBUTE_MODIFIER = new AttributeModifier(UUID.fromString("1CCA8D2D-9A0B-3FF2-E505-EF4A439570C3"), "machine_gun", 20, AttributeModifier.Operation.ADDITION);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SHOOT_ANI = RawAnimation.begin().thenPlay("shoot");
    private static final RawAnimation IDLE_ANI = RawAnimation.begin().thenLoop("idle");
    private boolean init;
    private double temperature = 0;
    private int cloudTime;
    private boolean fire;
    private int coolDownTime;

    public HeavyMachineGunEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ZombieKitEntities.HEAVY_MACHINE_GUN.get(), world);
    }

    public HeavyMachineGunEntity(EntityType<HeavyMachineGunEntity> type, Level world) {
        super(type, world);
        xpReward = 0;
        setPersistenceRequired();
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void tick() {
        super.tick();
        heat();
        if (this.getVehicle() == null) {
            setYBodyRot(yBodyRotO);
        } else if (this.getVehicle() instanceof LivingEntity livingEntity) {
            setYBodyRot(livingEntity.yBodyRot);
            yBodyRotO = livingEntity.yBodyRot;
        }
        List<Entity> passengers = getPassengers();
        if (passengers.size() != 0 && passengers.get(0) instanceof LivingEntity passenger) {
            turnGunpoint(passenger);
            if (passenger instanceof Player player && fire) {
                if (coolDownTime++ % 4 == 0) {
                    if (checkCanFire(player)) {
                        fire();
                    } else if (!level().isClientSide())
                        level().playSound(null, player.getX(), player.getY(), player.getZ(), ZombieKitSounds.heavy_machine_gun_trigger.get(), SoundSource.BLOCKS, 1, 1);
                }
            } else {
                coolDownTime = 0;
            }
        } else {
            if (this.level() instanceof ServerLevel serverLevel) {
                if (!this.init) {
                    StructureManager structureFeatureManager = serverLevel.structureManager();
                    Structure configuredStructureFeature = structureFeatureManager.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ZombieKitTags.STRUCTURE);
                    if (configuredStructureFeature != null && structureFeatureManager.getStructureAt(BlockPos.containing(this.getX(), this.getY(), this.getZ()), configuredStructureFeature).isValid()) {
                        BlockState state = serverLevel.getBlockState(BlockPos.containing(this.getX(), this.getY(), this.getZ()).below());
                        if (state.getBlock().getStateDefinition().getProperty("facing") instanceof EnumProperty) {
                            GunDirection direction = GunDirection.getDirection(state.getBlock().getStateDefinition().getProperty("facing").toString().toUpperCase());
                            setYBodyRot(direction.getAngle());
                            yBodyRotO = direction.getAngle();
                            setYHeadRot(direction.getAngle());
                        }
                    }
                    init = true;
                }
            }
            tryMakeMobRide();
        }
    }

    public boolean isFire() {
        return fire;
    }

    public void setFire(boolean fire) {
        this.fire = fire;
    }

    public boolean checkCanFire(Player player) {
        if (getOverload())
            return false;
        if (player.isCreative())
            return true;
        List<ItemStack> ammo = player.getInventory().items.stream()
                .filter((itemStack) -> itemStack.is(ZombieKitTags.MACHINE_GUN_AMMO)).toList();
        if (!ammo.isEmpty()) {
            if (!level().isClientSide())
                ammo.get(0).shrink(1);
            return true;
        }
        return false;
    }

    public void heat() {
        if (getOverload()) {
            this.temperature = Math.max(temperature - 0.05, 0);
            if (temperature < 50) {
                this.setOverload(false);
            }
        } else {
            this.temperature = Math.max(temperature - 0.1, 0);
        }
        cloudTime = (cloudTime + 1) % 10;
        Vec3 angle = this.getViewVector(1);
        if (this.temperature > 50 && this.cloudTime % 10 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX() + angle.x, getY() + 1.5, getZ() + angle.z, 0, 0, 2, 0, 0.1);
            return;
        }
        if (this.temperature > 75 && this.cloudTime % 5 == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX() + angle.x, getY() + 1.5, getZ() + angle.z, 0, 0, 2, 0, 0.1);
            if (temperature > 99) {
                this.level().playSound(null, getX(), getY(), getZ(), ZombieKitSounds.heavy_machine_gun_overload.get(), SoundSource.BLOCKS, 1, 1);
                this.setOverload(true);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains("Temperature")) {
            this.temperature = compoundTag.getDouble("Temperature");
        }
        if (compoundTag.contains("Overload")) {
            this.setOverload(compoundTag.getBoolean("Overload"));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putDouble("Temperature", this.temperature);
        compoundTag.putBoolean("Overload", this.getOverload());
    }

    public void turnGunpoint(LivingEntity passenger) {
        Vec3 passengerViewer = passenger.getViewVector(1);
        Vec3 horizontalVec = new Vec3(-Mth.sin(-yBodyRot * ((float) Math.PI / 180F) - (float) Math.PI), 0, -Mth.cos(-yBodyRot * ((float) Math.PI / 180F) - (float) Math.PI)).normalize();
        Vec3 horizontalViewer = new Vec3(passengerViewer.x, 0, passengerViewer.z).normalize();
        passenger.setYBodyRot(yBodyRot);
        if (SonaMath.vectorDegreeCalculate(horizontalVec, horizontalViewer) <= 60.01) {
            float y = passenger instanceof Player ? passenger.getYRot() : passenger.getYHeadRot();
            setYRot(y);
            setYHeadRot(getYRot());
        }
        float x = passenger.getXRot();
        if (x <= 30 && x >= -50) {
            setXRot(x);
        }
    }

    public void tryMakeMobRide() {
        List<LivingEntity> list = this.level().getNearbyEntities(LivingEntity.class, TargetingConditions.forNonCombat().range(5.0), this, this.getBoundingBox().inflate(2.0, 2.0, 2.0));
        if (!list.isEmpty()) {
            list.sort((l1, l2) -> {
                double a = Math.pow(l1.getX() - HeavyMachineGunEntity.this.getX(), 2) + Math.pow(l1.getZ() - HeavyMachineGunEntity.this.getZ(), 2);
                double b = Math.pow(l2.getX() - HeavyMachineGunEntity.this.getX(), 2) + Math.pow(l2.getZ() - HeavyMachineGunEntity.this.getZ(), 2);
                return Double.compare(a, b);
            });
            for (LivingEntity entity : list) {
                if (entity instanceof Mob mob && entity.getType().is(ZombieKitTags.MACHINE_GUNNER)) {
                    if (!mob.isNoAi() && mob.getVehicle() == null) {
                        mob.startRiding(this);
                        mob.setYRot(yBodyRot);
                        mob.setYHeadRot(yBodyRot);
                        mob.setYBodyRot(yBodyRot);
                        AttributeInstance attributeInstance = mob.getAttributes().getInstance(Attributes.FOLLOW_RANGE);
                        if (attributeInstance == null) continue;
                        attributeInstance.removeModifier(ATTRIBUTE_MODIFIER);
                        attributeInstance.addPermanentModifier(ATTRIBUTE_MODIFIER);
                        mob.setPersistenceRequired();
                        break;
                    }
                }
            }
        }
    }


    public void fire() {
        List<Entity> passengers = getPassengers();
        if (passengers.size() != 0 && passengers.get(0) instanceof LivingEntity owner) {
            owner.addEffect(new MobEffectInstance(SonaMobEffects.EXPOSURE.get(), 20, 3, false, false));
            if (!this.level().isClientSide) {
                HeavyMachineGunAmmoEntity.shoot(this.level(), owner, 8f, 0, 0);
                this.level().playSound(null, getX(), getY(), getZ(), ZombieKitSounds.heavy_machine_gun_fire.get(), SoundSource.BLOCKS, 4, 1);
                if (!(this.hasEffect(SonaMobEffects.FROST.get()) || this.isFreezing()))
                    this.temperature = Math.min(temperature + 0.9, 100);
            }
            if (passengers.get(0) instanceof Player player) {
                player.turn(Mth.nextFloat(this.level().getRandom(), -8f, 8f), Mth.nextFloat(this.level().getRandom(), -10, -5));
            }
        }
        triggerAnim("procedureController", "shoot");
    }

    public boolean canSee(Entity livingEntity) {
        boolean YRange;
        double x = livingEntity.getX();
        double y = livingEntity.getY();
        double z = livingEntity.getZ();
        Vec3 horizontalVec = new Vec3(-Mth.sin(-yBodyRot * ((float) Math.PI / 180F) - (float) Math.PI), 0, -Mth.cos(-yBodyRot * ((float) Math.PI / 180F) - (float) Math.PI));
        Vec3 horizontalDis = new Vec3(x - getX(), 0, z - getZ());
        boolean XRange = SonaMath.vectorDegreeCalculate(horizontalVec, horizontalDis) < 60;
        if (y > getY())
            YRange = Math.toDegrees(Math.atan((y - getY()) / horizontalDis.length())) < 30;
        else if (y < getY())
            YRange = Math.toDegrees(Math.atan((getY() - y) / horizontalDis.length())) < 50;
        else
            YRange = true;
        return XRange && YRange;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHOOT, false);
        this.entityData.define(OVERLOAD, false);
        this.entityData.define(TEXTURE, "m2_machine_gun");
    }

    public void setTexture(String texture) {
        this.entityData.set(TEXTURE, texture);
    }

    public String getTexture() {
        return this.entityData.get(TEXTURE);
    }

    public void setOverload(boolean overload) {
        this.entityData.set(OVERLOAD, overload);
    }

    public boolean getOverload() {
        return this.entityData.get(OVERLOAD);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return super.getPassengersRidingOffset() - 0.8;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(""));
    }

    @Override
    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(""));
    }

    @Override
    public void heal(float p_21116_) {
    }

    @Override
    public InteractionResult mobInteract(Player sourceentity, InteractionHand hand) {
        ItemStack itemstack = sourceentity.getItemInHand(hand);
        InteractionResult retval = InteractionResult.sidedSuccess(this.level().isClientSide());
        super.mobInteract(sourceentity, hand);
        if (itemstack.is(ZombieKitItems.WRENCH.get())) {
            ItemEntity entityToSpawn = new ItemEntity(this.level(), getX(), getY(), getZ(), new ItemStack(ZombieKitItems.HEAVY_MACHINE_GUN_SUMMON.get()));
            entityToSpawn.setPickUpDelay(10);
            this.level().addFreshEntity(entityToSpawn);
            this.discard();
        } else if (itemstack.is(Items.POWDER_SNOW_BUCKET)) {
            this.addEffect(new MobEffectInstance(SonaMobEffects.FROST.get(), 1200, 0));
            if (!sourceentity.getAbilities().instabuild) {
                ItemStack setstack = new ItemStack(Items.BUCKET);
                setstack.setCount(1);
                ItemHandlerHelper.giveItemToPlayer(sourceentity, setstack);
                itemstack.setCount(itemstack.getCount() - 1);
            }
        } else {
            if (!sourceentity.isShiftKeyDown()) {
                sourceentity.startRiding(this);
                sourceentity.setYBodyRot(yBodyRot);
                sourceentity.setYRot(yBodyRot);
                sourceentity.setYHeadRot(yBodyRot);
                sourceentity.setXRot(0);
            } else {
                setYBodyRot(sourceentity.getYRot());
                setYHeadRot(sourceentity.getYRot());
                setYRot(sourceentity.getYRot());
                if (!this.getPassengers().isEmpty()) {
                    this.getPassengers().get(0).setYBodyRot(sourceentity.getYRot());
                    this.getPassengers().get(0).setYHeadRot(sourceentity.getYRot());
                }

            }
        }

        return retval;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.refreshDimensions();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source == this.level().damageSources().drown())
            return false;
        return super.hurt(source, amount);
    }

    @Override
    public EntityDimensions getDimensions(Pose p_33597_) {
        return super.getDimensions(p_33597_).scale((float) 1);
    }


    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        this.spawnAtLocation(new ItemStack(ZombieKitItems.MACHINE_GUN_COMPONENTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, 0);
        builder = builder.add(Attributes.MAX_HEALTH, 20);
        builder = builder.add(Attributes.ARMOR, 0);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
        builder = builder.add(Attributes.FOLLOW_RANGE, 0);
        builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1000);
        return builder;
    }

    private PlayState procedurePredicate(AnimationState event) {
        AnimationController<Flamethrower> controller = event.getController();
        if (!controller.isPlayingTriggeredAnimation()) {
            if (controller.getCurrentRawAnimation() == null || event.getController().hasAnimationFinished()) {
                controller.setAnimation(IDLE_ANI);
            }
        }
        return PlayState.CONTINUE;
    }


    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(HeavyMachineGunEntity.RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "procedureController", 0, this::procedurePredicate)
                .triggerableAnim("shoot", SHOOT_ANI)
                .receiveTriggeredAnimations());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean validXRot(LivingEntity livingEntity, float xRot) {
        return xRot <= 30 && xRot >= -50;
    }

    @Override
    public boolean validYRot(LivingEntity livingEntity, float yRot) {
        Vec3 passengerViewer = this.calculateViewVector(livingEntity.getXRot(), yRot);
        Vec3 horizontalVec = new Vec3(-Mth.sin(-yBodyRot * ((float) Math.PI / 180F) - (float) Math.PI), 0, -Mth.cos(-yBodyRot * ((float) Math.PI / 180F) - (float) Math.PI)).normalize();
        Vec3 horizontalViewer = new Vec3(passengerViewer.x, 0, passengerViewer.z).normalize();
        return SonaMath.vectorDegreeCalculate(horizontalVec, horizontalViewer) <= 60;
    }

    @Override
    public CameraType getVehicleCameraType() {
        return CameraType.FIRST_PERSON;
    }


    enum GunDirection {
        NORTH(180f),
        SOUTH(0f),
        WEST(90f),
        EAST(-90f);

        private final float angle;

        GunDirection(float angle) {
            this.angle = angle;
        }

        public float getAngle() {
            return this.angle;
        }

        public static GunDirection getDirection(String facing) {
            return switch (facing) {
                case "NORTH" -> NORTH;
                case "SOUTH" -> SOUTH;
                case "WEST" -> WEST;
                default -> EAST;
            };
        }
    }
}
