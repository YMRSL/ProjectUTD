package com.scarasol.zombiekit.entity.mechanics;

import com.scarasol.sona.util.SonaMath;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.api.FixedVehicle;
import com.scarasol.zombiekit.block.entity.MortarRackBlockEntity;
import com.scarasol.zombiekit.data.LaunchSchedule;
import com.scarasol.zombiekit.entity.ai.control.MortarLookControl;
import com.scarasol.zombiekit.entity.ai.goal.MortarUsingGoal;
import com.scarasol.zombiekit.entity.projectile.MortarShellEntity;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.init.ZombieKitTags;
import com.scarasol.zombiekit.item.projectile.MortarShell;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import com.scarasol.zombiekit.network.MapVariables;
import net.minecraft.client.CameraType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MortarEntity extends Mechanics implements GeoEntity, FixedVehicle {

    public static final EntityDataAccessor<Float> ANGLE = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> AZIMUTH = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> SHELL = SynchedEntityData.defineId(MortarEntity.class, EntityDataSerializers.STRING);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private long launchTimestamp;
    private int launchWaiting = 20;
    private LaunchSchedule currentSchedule;
    public static final float VELOCITY = 3.5f;
    private BlockPos rack;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);


    public MortarEntity(EntityType<? extends Mechanics> type, Level world) {
        super(type, world);
        xpReward = 0;
        setPersistenceRequired();
        this.lookControl = new MortarLookControl(this);
    }

    public MortarEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ZombieKitEntities.MORTAR.get(), world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new MortarUsingGoal<>(this));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANGLE, -45f);
        this.entityData.define(AZIMUTH, 0f);
        this.entityData.define(TEXTURE, "mortar");
        this.entityData.define(SHELL, "Empty");

    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains("Angle")) {
            setAngle(compoundTag.getFloat("Angle"));
        }
        if (compoundTag.contains("Azimuth")) {
            setAzimuth(compoundTag.getFloat("Azimuth"));
        }
        if (compoundTag.contains("Shell")) {
            setShell(compoundTag.getString("Shell"));
        }
        if (compoundTag.contains("LaunchTimestamp")) {
            this.launchTimestamp = compoundTag.getLong("LaunchTimestamp");
        }
        if (compoundTag.contains("RackX")) {
            rack = new BlockPos(compoundTag.getInt("RackX"), compoundTag.getInt("RackY"), compoundTag.getInt("RackZ"));
        }

    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putFloat("Angle", getAngle());
        compoundTag.putFloat("Azimuth", getAzimuth());
        compoundTag.putString("Shell", getShellStr());
        compoundTag.putLong("LaunchTimestamp", this.launchTimestamp);
        if (this.rack != null) {
            compoundTag.putInt("RackX", rack.getX());
            compoundTag.putInt("RackY", rack.getY());
            compoundTag.putInt("RackZ", rack.getZ());
        }
    }

    public BlockPos getRack() {
        return rack;
    }

    public void setRack(BlockPos rack) {
        this.rack = rack;
    }

    public LaunchSchedule getCurrentSchedule() {
        return currentSchedule;
    }

    public void setCurrentSchedule(LaunchSchedule currentSchedule) {
        this.currentSchedule = currentSchedule;
    }

    public void setTexture(String texture) {
        this.entityData.set(TEXTURE, texture);
    }

    public String getTexture() {
        return this.entityData.get(TEXTURE);
    }

    public String getModel() {
        if (getShell() == null)
            return "mortar_without_shell";
        return "mortar_with_shell";
    }


    public float getAngle() {
        return this.entityData.get(ANGLE);
    }

    public void setAngle(float angle) {
        angle = Math.max(Math.min(-45, angle), -85);
        this.entityData.set(ANGLE, angle);
    }

    public void setAzimuth(float azimuth) {
        this.entityData.set(AZIMUTH, azimuth);
    }

    public float getAzimuth() {
        return this.entityData.get(AZIMUTH);
    }

    @Nullable
    public Item getShell() {
        String str = getShellStr();
        if ("Empty".equals(str)) {
            return null;
        }
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(str));
    }

    public String getShellStr() {
        return this.entityData.get(SHELL);
    }

    public void setShell(String shell) {
        this.entityData.set(SHELL, shell);
    }

    public void setShell(@Nullable Item item) {
        if (item == null) {
            setShell("Empty");
            return;
        }
        setShell(ForgeRegistries.ITEMS.getKey(item).toString());
    }

    public void setShell(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            setShell((Item) null);
            return;
        }
        setShell(itemStack.getItem());
    }

    public boolean reload(Item item) {
        if (item instanceof MortarShell) {
            setShell(item);
            launchWaiting = 20;
            return true;
        }
        return false;
    }

    @Override
    public void remove(RemovalReason removalReason) {
        if (removalReason != RemovalReason.UNLOADED_TO_CHUNK && removalReason != RemovalReason.UNLOADED_WITH_PLAYER) {
            if (level() instanceof ServerLevel serverLevel) {
                MapVariables mapVariables = MapVariables.get(serverLevel);
                Map<UUID, ChunkPos> mortarPos = mapVariables.mortarPos;
                mortarPos.remove(uuid);
                mapVariables.setDirty();
            }
        }
        super.remove(removalReason);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive() && level() instanceof ServerLevel serverLevel && serverLevel.getGameTime() % 5 == 0) {
            MapVariables mapVariables = MapVariables.get(serverLevel);
            Map<UUID, ChunkPos> mortarPos = mapVariables.mortarPos;
            ChunkPos chunkPos = new ChunkPos(blockPosition());
            if (!chunkPos.equals(mortarPos.get(uuid))) {
                mortarPos.put(uuid, new ChunkPos(blockPosition()));
                mapVariables.setDirty();
            }
        }
        this.setXRot(getAngle());
        this.setYHeadRot(getAzimuth());
        this.setYBodyRot(getYHeadRot());
        this.setYRot(getYHeadRot());
        List<Entity> passengers = getPassengers();
        if (passengers.size() == 0) {
            tryMakeMobRide();
        }else {
            if (passengers.get(0) instanceof Player player) {
                turnMortar(player);
            }
            if (checkLaunch()) {
                if (launchWaiting-- == 0) {
                    executeLaunch(passengers.get(0));
                    launchWaiting = -1;
                }
            }
        }

    }

    public void turnMortar(Player player) {
        Vec3 passengerViewer = player.getViewVector(1);
        Vec3 horizontalVec = new Vec3(-Mth.sin(-getAzimuth() * ((float) Math.PI / 180F) - (float) Math.PI), 0, -Mth.cos(-getAzimuth() * ((float) Math.PI / 180F) - (float) Math.PI)).normalize();
        Vec3 horizontalViewer = new Vec3(passengerViewer.x, 0, passengerViewer.z).normalize();
        Vec3 limit1 = horizontalVec.yRot((float) Math.toRadians(-30));
        Vec3 limit2 = horizontalVec.yRot((float) Math.toRadians(30));
        float degree = (float) SonaMath.vectorDegreeCalculate(horizontalVec, horizontalViewer);
        if (degree >= 30) {
            if (SonaMath.vectorDegreeCalculate(limit1, horizontalViewer) < SonaMath.vectorDegreeCalculate(limit2, horizontalViewer)) {
                this.setAzimuth(Mth.wrapDegrees(getAzimuth() + Math.min((degree + 30f) / 2f, 2.5f)));
            } else {
                this.setAzimuth(Mth.wrapDegrees(getAzimuth() - Math.min((degree - 30f) / 2f, 2.5f)));
            }

        }
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(ZombieKitItems.WRENCH.get())) {
            ItemEntity entityToSpawn = new ItemEntity(this.level(), getX(), getY(), getZ(), new ItemStack(ZombieKitItems.MORTAR_SUMMON.get()));
            entityToSpawn.setPickUpDelay(10);
            this.level().addFreshEntity(entityToSpawn);
            this.discard();
        }else if (!player.isShiftKeyDown()) {
            if (!getPassengers().isEmpty())
                getPassengers().get(0).stopRiding();
            player.setYRot(getAzimuth());
            player.startRiding(this);
        } else {
            setAzimuth(player.getYRot());
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    public void tryMakeMobRide() {
        List<LivingEntity> list = this.level().getNearbyEntities(LivingEntity.class, TargetingConditions.forNonCombat().range(5.0), this, this.getBoundingBox().inflate(2.0, 2.0, 2.0));
        if (!list.isEmpty()) {
            list.sort((l1, l2) -> {
                double a = Math.pow(l1.getX() - MortarEntity.this.getX(), 2) + Math.pow(l1.getZ() - MortarEntity.this.getZ(), 2);
                double b = Math.pow(l2.getX() - MortarEntity.this.getX(), 2) + Math.pow(l2.getZ() - MortarEntity.this.getZ(), 2);
                return Double.compare(a, b);
            });
            for (LivingEntity entity : list) {
                if (entity instanceof Mob mob && entity.getType().is(ZombieKitTags.ARTILLERY)) {
                    if (!mob.isNoAi() && mob.getVehicle() == null) {
                        mob.startRiding(this);
                        mob.setYRot(yBodyRot);
                        mob.setYHeadRot(yBodyRot);
                        mob.setYBodyRot(yBodyRot);
                        mob.setPersistenceRequired();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public void heal(float amount) {
    }

    @Nullable
    public static BlockPos getCoverPos(Entity player) {
        Vec3 position = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 vec3;
        vec3 = player.getLookAngle();
        Level level = player.level();
        for (int i = 0; i < 200; i++) {
            position = position.add(vec3);
            BlockPos pos = BlockPos.containing(position);
            BlockState blockState = level.getBlockState(pos);
            if (blockState.canOcclude()) {
                return pos;
            }
        }
        return null;

    }

    public static Vec3 calculateDroneVector(float x, float y) {
        float f = x * ((float)Math.PI / 180F);
        float f1 = -y * ((float)Math.PI / 180F);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3((double)(f3 * f4), (double)(-f5), (double)(f2 * f4));
    }

    public boolean reload() {
        if (this.rack != null && level().getBlockEntity(this.rack) instanceof MortarRackBlockEntity mortarRackBlockEntity) {
            ItemStack itemStack = mortarRackBlockEntity.popShell();
            if (!itemStack.isEmpty()) {
                return reload(itemStack.getItem());
            }
        }else {
            this.rack = null;
        }
        return false;
    }

    public boolean checkLaunch() {
        return getShell() instanceof MortarShell && level().getGameTime() - this.launchTimestamp > 80;
    }

    public void launch(boolean accurate) {
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 angle = getLookAngle();
            RandomSource randomSource = serverLevel.getRandom();
            double x = angle.x;
            double y = angle.y;
            double z = angle.z;
            float power = VELOCITY;
            if (!accurate) {
                x = x + randomSource.nextGaussian() * 0.02;
                y = y + randomSource.nextGaussian() * 0.02;
                z = z + randomSource.nextGaussian() * 0.02;
                power = (float) (power + randomSource.nextGaussian() * 0.02);
            }
            this.launchTimestamp = serverLevel.getGameTime();
            serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), ZombieKitSounds.mortar_launch.get(), SoundSource.PLAYERS, 2, 0);
            MortarShellEntity.shoot(serverLevel, this, x, y, z, serverLevel.getRandom(), power, currentSchedule, ((MortarShell)getShell()).getEffect());
            currentSchedule = null;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, getX() + angle.x * 2, getY() + angle.y * 2, getZ() + angle.z * 2, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 40, 1.5, 0.2, 1.5, 0.01);
            setShell("Empty");
        }
    }

    public void executeLaunch(Entity passenger) {
        if (passenger instanceof Player || (getRack() != null && level().getBlockEntity(getRack()) instanceof MortarRackBlockEntity blockEntity && blockEntity.hasTable()))
            launch(true);
        else
            launch(false);
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

    @Override
    protected void positionRider(Entity entity, Entity.MoveFunction moveFunction) {
        if (this.hasPassenger(entity)) {
            Vec3 angle = new Vec3(getLookAngle().x, 0, getLookAngle().z).yRot((float) (Math.PI / 4)).normalize().scale(1.2);
            moveFunction.accept(entity, this.getX() + angle.x, this.getY(), this.getZ() + angle.z);
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(""));
    }

    @Override
    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(""));
    }

    private PlayState procedurePredicate(AnimationState event) {
        AnimationController<MortarEntity> controller = event.getController();
        if (!controller.isPlayingTriggeredAnimation()) {
            if (controller.getCurrentRawAnimation() == null || event.getController().hasAnimationFinished()) {
                controller.setAnimation(IDLE);
            }
        }
        return PlayState.CONTINUE;
    }


    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(MortarEntity.RemovalReason.KILLED);
            this.dropExperience();
        }
    }

    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHitIn) {
        super.dropCustomDeathLoot(source, looting, recentlyHitIn);
        this.spawnAtLocation(new ItemStack(ZombieKitItems.MORTAR_COMPONENTS.get()));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "procedureController", 0, this::procedurePredicate)
                .receiveTriggeredAnimations());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public boolean validXRot(LivingEntity livingEntity, float xRot) {
        return true;
    }

    @Override
    public boolean validYRot(LivingEntity livingEntity, float yRot) {
        Vec3 passengerViewer = this.calculateViewVector(livingEntity.getXRot(), yRot);
        Vec3 horizontalVec = new Vec3(-Mth.sin(-getAzimuth() * ((float) Math.PI / 180F) - (float) Math.PI), 0, -Mth.cos(-getAzimuth() * ((float) Math.PI / 180F) - (float) Math.PI)).normalize();
        Vec3 horizontalViewer = new Vec3(passengerViewer.x, 0, passengerViewer.z).normalize();
        return SonaMath.vectorDegreeCalculate(horizontalVec, horizontalViewer) <= 45;
    }

    @Override
    public int getMaxHeadXRot() {
        return 2;
    }

    @Override
    public int getMaxHeadYRot() {
        return 0;
    }

    @Override
    public int getHeadRotSpeed(){
        return 3;
    }

    @Override
    public CameraType getVehicleCameraType() {
        return CameraType.THIRD_PERSON_BACK;
    }
}
