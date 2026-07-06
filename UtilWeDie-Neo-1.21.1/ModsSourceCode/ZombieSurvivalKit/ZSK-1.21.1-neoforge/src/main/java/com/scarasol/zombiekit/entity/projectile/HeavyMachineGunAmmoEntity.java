package com.scarasol.zombiekit.entity.projectile;

import com.scarasol.zombiekit.api.NoAttenuationProjectile;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitDamageTypes;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.Tags;

public class HeavyMachineGunAmmoEntity extends ModProjectile implements NoAttenuationProjectile {
    private int life = 0;

    public HeavyMachineGunAmmoEntity(EntityType<? extends HeavyMachineGunAmmoEntity> type, Level world) {
        super(type, world);
    }

    public HeavyMachineGunAmmoEntity(EntityType<? extends HeavyMachineGunAmmoEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
    }

    public HeavyMachineGunAmmoEntity(EntityType<? extends HeavyMachineGunAmmoEntity> type, LivingEntity entity, Level world) {
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
        if (++life > 60)
            this.discard();
    }

    @Override
    public void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        Entity entity = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        DamageSource ammo1;
        DamageSource ammo2;
        if (owner != null){
            ammo1 = ZombieKitDamageTypes.damageSource(this.level().registryAccess(), ZombieKitDamageTypes.HEAVY_MACHINE_GUN, this, owner);
            ammo2 = ZombieKitDamageTypes.damageSource(this.level().registryAccess(), ZombieKitDamageTypes.HEAVY_MACHINE_GUN_BYPASS_ARMOR, this, owner);
        }else {
            ammo1 = ZombieKitDamageTypes.damageSource(this.level().registryAccess(), ZombieKitDamageTypes.HEAVY_MACHINE_GUN, this, this);
            ammo2 = ZombieKitDamageTypes.damageSource(this.level().registryAccess(), ZombieKitDamageTypes.HEAVY_MACHINE_GUN_BYPASS_ARMOR, this, this);
        }
        float armorIgnored = (float) (CommonConfig.HEAVY_MACHINE_GUN_DAMAGE.get() * CommonConfig.ARMOR_PIERCING_RATE.get());
        float hurt = CommonConfig.HEAVY_MACHINE_GUN_DAMAGE.get() - armorIgnored;

        int invulnerableTime = entity.invulnerableTime;
        if (CommonConfig.IGNORING_INVULNERABILITY.get())
            entity.invulnerableTime = 0;
        entity.hurt(ammo1, hurt);
        if (invulnerableTime <= 10 || CommonConfig.IGNORING_INVULNERABILITY.get())
            entity.invulnerableTime = 0;
        entity.hurt(ammo2, armorIgnored);
        discard();
    }

    @Override
    public void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockState blockState = this.level().getBlockState(blockHitResult.getBlockPos());
        if(blockState.is(Tags.Blocks.GLASS_BLOCKS) || blockState.is(Tags.Blocks.GLASS_PANES)){
            this.level().destroyBlock(blockHitResult.getBlockPos(), false, this);
        }
    }

    @Override
    public void doEffects(Level level, double x, double y, double z) {
    }

    public static void shoot(Level world, LivingEntity entity, float power, double damage, int knockback) {
        HeavyMachineGunAmmoEntity entityArrow = new HeavyMachineGunAmmoEntity(ZombieKitEntities.HEAVY_MACHINE_GUN_AMMO.get(), entity, world);
        entityArrow.shoot(entity.getViewVector(1).x, entity.getViewVector(1).y, entity.getViewVector(1).z, power, 0);
        entityArrow.setSilent(true);
        entityArrow.setCritArrow(false);
        entityArrow.setBaseDamage(damage);
        entityArrow.setNoGravity(true);
        world.addFreshEntity(entityArrow);
    }
}
