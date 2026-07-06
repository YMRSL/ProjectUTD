package com.scarasol.zombiekit.entity.mechanics;

import com.google.common.collect.Sets;
import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.init.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Collectors;

public class UvLampEntity extends Mechanics{

    public static final EntityDataAccessor<Boolean> hasBattery = SynchedEntityData.defineId(UvLampEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> power =SynchedEntityData.defineId(UvLampEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> lightswitch =SynchedEntityData.defineId(UvLampEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int RANGE = 6;
    private int time;

    public final Set<BlockPos> posNeedToUpdate = Sets.newHashSet();

    public UvLampEntity(EntityType<? extends Mechanics> type, Level world) {
        super(type, world);
    }

    public UvLampEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ZombieKitEntities.UV_LAMP.get(), world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(hasBattery, false);
        this.entityData.define(power, 0);
        this.entityData.define(lightswitch, false);

    }

    public boolean isHasBattery() {
        return entityData.get(hasBattery);
    }

    public void setHasBattery(boolean hasBattery) {
        entityData.set(UvLampEntity.hasBattery, hasBattery);
    }

    public int getPower() {
        return entityData.get(power);
    }

    public void setPower(int power) {
        entityData.set(UvLampEntity.power, power);
    }

    public boolean isLightswitch() {
        return entityData.get(lightswitch);
    }

    public void setLightswitch(boolean lightswitch) {
        entityData.set(UvLampEntity.lightswitch, lightswitch);
    }

    public void addBlockPos(BlockPos blockPos) {
        if (isLightswitch()) {
            if (checkStep(blockPos)) {
                posNeedToUpdate.add(blockPos);
            }
        }
    }

    public boolean checkStep(BlockPos blockPos) {
        BlockPos location = getOnPos().above();
        int dx = blockPos.getX() - location.getX();
        int dy = blockPos.getY() - location.getY();
        int dz = blockPos.getZ() - location.getZ();
        int steps = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
        return steps <= RANGE;
    }

    public void updatePos() {
        if (!posNeedToUpdate.isEmpty()) {
            Set<BlockPos> posNeedToUpdateCopy = Sets.newHashSet(posNeedToUpdate);
            Level level = level();
            Vec3 location = getOnPos().above().getCenter();
            Set<BlockPos> checkedPos = Sets.newHashSet();
            for (BlockPos pos : posNeedToUpdateCopy) {
                Vec3 direction = pos.getCenter().subtract(location).normalize();
                if (direction.equals(Vec3.ZERO))
                    continue;
                boolean flag = false;
                for (int i = 1;; i++) {
                    BlockPos checkPos = BlockPos.containing(location.add(direction.scale(i)));
                    if (!checkStep(checkPos))
                        break;
                    BlockState blockState = level.getBlockState(checkPos);
                    if (checkPos.equals(getOnPos().above()))
                        continue;
                    if (blockState.getLightBlock(level, checkPos) != 0) {
                        flag = true;
                        continue;
                    }
                    if (checkedPos.contains(checkPos))
                        continue;
                    checkedPos.add(checkPos);
                    if (!flag) {
                        BlockHitResult result = level.clip(new ClipContext(
                                getOnPos().above().getCenter(), checkPos.getCenter(),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                null
                        ));
                        if (result.getType() == HitResult.Type.MISS && blockState.isAir())
                            level.setBlock(checkPos, ZombieKitBlocks.SPREAD_LIGHT.get().defaultBlockState(), 3);
                        else if (result.getType() != HitResult.Type.MISS && blockState.is(ZombieKitBlocks.SPREAD_LIGHT.get()))
                            level.removeBlock(checkPos, false);
                    }else {
                        if (blockState.is(ZombieKitBlocks.SPREAD_LIGHT.get())) {
                                level.removeBlock(checkPos, false);
                        }

                    }
                }
            }
            posNeedToUpdate.removeAll(checkedPos);
        }
    }

    public void fillLight() {
        Level level = level();
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos blockPos = getOnPos().above().offset(x, y, z);
                    if (blockPos == getOnPos().above())
                        continue;
                    if (level.getBlockState(blockPos).isAir()) {
                        BlockHitResult result = level.clip(new ClipContext(
                                getOnPos().above().getCenter(), blockPos.getCenter(),
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                null
                        ));
                        if (result.getType() == HitResult.Type.MISS) {
                            level.setBlock(blockPos, ZombieKitBlocks.SPREAD_LIGHT.get().defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    public void clearLight() {
        Level level = level();
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int y = -RANGE; y <= RANGE; y++) {
                for (int z = -RANGE; z <= RANGE; z++) {
                    BlockPos blockPos = getOnPos().above().offset(x, y, z);
                    if (level.getBlockState(blockPos).is(ZombieKitBlocks.SPREAD_LIGHT.get()))
                        level.removeBlock(blockPos, false);
                }
            }
        }
    }

    public void popBattery(){
        ItemStack itemStack = new ItemStack(ZombieKitItems.BATTERY.get(), 1);
        itemStack.setDamageValue(100 - getPower());
        ItemEntity itemEntity = new ItemEntity(level(), getX(), getY(), getZ(), itemStack);
        itemEntity.setPickUpDelay(10);
        level().addFreshEntity(itemEntity);
        setHasBattery(false);
        setPower(0);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        Level level = this.level();
        if (!isHasBattery() && player.getOffhandItem().getItem() == ZombieKitItems.WRENCH.get() && player.getMainHandItem().getItem() == ZombieKitItems.BATTERY.get()){
            setHasBattery(true);
            setPower(100 - player.getMainHandItem().getDamageValue());
            if (!player.isCreative()){
                player.getMainHandItem().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }else if (isHasBattery() && player.getOffhandItem().getItem() == ZombieKitItems.WRENCH.get()){
            popBattery();
            return InteractionResult.SUCCESS;
        }else if (player.getMainHandItem().getItem() == ZombieKitItems.WRENCH.get()){
            if (isHasBattery())
                popBattery();
            if (isLightswitch()) {
                clearLight();
            }
            ItemStack itemStack = new ItemStack(ZombieKitItems.UV_LAMP.get(), 1);
            itemStack.setDamageValue(20 - Mth.floor(getHealth()));
            ItemEntity itemEntity = new ItemEntity(level, getX(), getY(), getZ(), itemStack);
            itemEntity.setPickUpDelay(10);
            level.addFreshEntity(itemEntity);
            this.discard();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (isLightswitch()){
            level().destroyBlock(getOnPos().above(), false);
            level().destroyBlock(getOnPos().above().above(), false);
        }
        if (isHasBattery()){
            popBattery();
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains("HasBattery"))
            setHasBattery(compoundTag.getBoolean("HasBattery"));
        if (compoundTag.contains("Power"))
            setPower(compoundTag.getInt("Power"));
        if (compoundTag.contains("Lightswitch"))
            setLightswitch(compoundTag.getBoolean("Lightswitch"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putInt("Power", getPower());
        compoundTag.putBoolean("HasBattery", isHasBattery());
        compoundTag.putBoolean("Lightswitch", isLightswitch());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.onGround())
            setNoAi(true);
        if (!level().isClientSide) {
            if (isHasBattery()){
                if (level().getBestNeighborSignal(this.getOnPos()) > 0){
                    if (isLightswitch()){
                        setLightswitch(false);
                        clearLight();
                        level().playSound(null, getOnPos(), ZombieKitSounds.turn_on.get(), SoundSource.NEUTRAL, 1, 1);
                    }
                    if (level().getGameTime() % com.scarasol.zombiekit.config.CommonConfig.LAMP_POWER.get() == 0)
                        setPower(Math.min(getPower() + 1, 100));
                }else if (getPower() > 0){
                    if (!isLightswitch()){
                        setLightswitch(true);
                        fillLight();
                        level().playSound(null, getOnPos(), ZombieKitSounds.turn_on.get(), SoundSource.NEUTRAL, 1, 1);
                    }
                    time = (time + 1) % 5;
                    if (time == 0)
                        searchUndead();
                    if (level().getGameTime() % com.scarasol.zombiekit.config.CommonConfig.LAMP_POWER.get() == 0)
                        setPower(Math.max(getPower() - 1, 0));
                }else {
                    if (isLightswitch()){
                        setLightswitch(false);
                        clearLight();
                        level().playSound(null, getOnPos(), ZombieKitSounds.turn_on.get(), SoundSource.NEUTRAL, 1, 1);
                    }
                }
            }else {
                if (isLightswitch()){
//                level().destroyBlock(getOnPos().above(), false);
//                level().destroyBlock(getOnPos().above().above(), false);

                    setLightswitch(false);
                    clearLight();
                    level().playSound(null, getOnPos(), ZombieKitSounds.turn_on.get(), SoundSource.NEUTRAL, 1, 1);
                }
            }
            if (isLightswitch())
                updatePos();
        }

    }

    public void searchUndead(){
        Vec3 _center = new Vec3(getX(), getY(), getZ());
            List<Mob> entFound = new ArrayList<>(level().getEntitiesOfClass(Mob.class, new AABB(_center, _center).inflate(RANGE + 7, 4, RANGE + 7), e -> true));
        for (Mob entityIterator : entFound) {
            if (entityIterator.getMobType() == MobType.UNDEAD || CommonConfig.findIndex(ForgeRegistries.ENTITY_TYPES.getKey(entityIterator.getType()).toString(), CommonConfig.INFECTION_SOURCE_MOB.get()) != -1){
                if (entityIterator.getType().is(ZombieKitTags.UV_RESISTANCE) || !entityIterator.hasLineOfSight(this))
                    continue;
                if (com.scarasol.zombiekit.config.CommonConfig.HIGH_PERFORMANCE_MODE.get()){
                    entityIterator.addEffect(new MobEffectInstance(SonaMobEffects.IGNITION.get(), 20, 2, false, false));
                }
                entityIterator.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20, 1, false, false));
                entityIterator.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
                entityIterator.addEffect(new MobEffectInstance(SonaMobEffects.FRAGILITY.get(), 20, 2, false, false));
                if (entityIterator.getTarget() == null || !entityIterator.getTarget().isAlive() || entityIterator.getType().is(ZombieKitTags.UV_NONRESISTANCE)){
                    entityIterator.setTarget(null);
                    BlockPos blockPos = entityIterator.getOnPos();
                    BlockPos uvPos = getOnPos();
                    BlockPos newPos = blockPos.offset(blockPos.getX() - uvPos.getX(), blockPos.getY() - uvPos.getY(), blockPos.getZ() - uvPos.getZ());
                    entityIterator.getNavigation().stop();
                    entityIterator.getNavigation().moveTo(newPos.getX(), newPos.getY(), newPos.getZ(), 1);
                }
            }
        }
    }


    public static AttributeSupplier.Builder createAttributes() {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();
        builder = builder.add(Attributes.MOVEMENT_SPEED, 0);
        builder = builder.add(Attributes.MAX_HEALTH, 20);
        builder = builder.add(Attributes.ARMOR, 10);
        builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
        builder = builder.add(Attributes.FOLLOW_RANGE, 16);
        builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
        return builder;
    }



}
