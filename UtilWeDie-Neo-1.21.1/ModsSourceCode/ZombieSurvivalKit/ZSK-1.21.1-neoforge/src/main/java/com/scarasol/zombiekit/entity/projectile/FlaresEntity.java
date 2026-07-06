package com.scarasol.zombiekit.entity.projectile;

import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.init.ZombieKitBlocks;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class FlaresEntity extends ModProjectile{
    private BlockPos lastPos;

    public FlaresEntity(EntityType<? extends FlaresEntity> type, Level world) {
        super(type, world);
    }

    public FlaresEntity(EntityType<? extends FlaresEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
    }

    public FlaresEntity(EntityType<? extends FlaresEntity> type, LivingEntity entity, Level world) {
        super(type, entity, world);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Blocks.AIR);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(BlockPos.containing(this.getX(), this.getY(),this.getZ()).equals(lastPos))){
            lastPos = BlockPos.containing(this.getX(), this.getY(),this.getZ());
        }
        if (this.inGround || this.isInWaterOrBubble())
            this.discard();
    }

    @Override
    public void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockState blockState = level().getBlockState(BlockPos.containing(getX(), getY(), getZ()));
        if (canReplace(blockState)){
            level().setBlock(BlockPos.containing(getX(), getY(), getZ()), ZombieKitBlocks.FLARES_LIGHT.get().defaultBlockState(), 3);
        }else if (lastPos != null) {
            BlockState blockState2 = level().getBlockState(lastPos);
            if (canReplace(blockState2)){
                level().setBlock(lastPos, ZombieKitBlocks.FLARES_LIGHT.get().defaultBlockState(), 3);
            }
        }
    }

    private static boolean canReplace(BlockState blockState) {
        return blockState.isAir() || blockState.getBlock() instanceof SnowLayerBlock || blockState.canBeReplaced();
    }

    @Override
    public void doEffects(Level level, double x, double y, double z) {
    }

    @Override
    public void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (entityHitResult.getEntity() instanceof LivingEntity _entity){
            _entity.addEffect(new MobEffectInstance(SonaMobEffects.IGNITION, 140, 3, false, false));
        }
        discard();
    }

    public static FlaresEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
        FlaresEntity entityArrow = new FlaresEntity(ZombieKitEntities.FLARES.get(), entity, world);
        ModProjectile.initProjectileEntity(entityArrow, world, entity, random, power, damage, knockback);
        entityArrow.igniteForSeconds(100);
        return entityArrow;
    }



}
