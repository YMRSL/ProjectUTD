package com.scarasol.zombiekit.entity.projectile;

import com.scarasol.zombiekit.init.ZombieKitEntities;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import java.util.Random;


@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public abstract class ModProjectile extends AbstractArrow implements ItemSupplier {

    protected boolean noGround = true;

    public ModProjectile(EntityType<? extends AbstractArrow> entityType, Level level) {
        super(entityType, level);
    }

    public ModProjectile(EntityType<? extends AbstractArrow> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
    }

    public ModProjectile(EntityType<? extends AbstractArrow> type, LivingEntity owner, Level world) {
        super(type, owner, world);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity entity) {
        super.doPostHurtEffects(entity);
        entity.setArrowCount(entity.getArrowCount() - 1);
    }

    @Override
    public void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        doEffects(level(), getX(), getY(), getZ());
    }

    @Override
    public void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        doEffects(level(), getX(), getY(), getZ());
    }

    public abstract void doEffects(Level level, double x, double y, double z);

    @Override
    public void tick() {
        super.tick();
        isOnGround();
    }

    public void isOnGround(){
        if (this.inGround && noGround)
            this.discard();
    }

    public static void initProjectileEntity(AbstractArrow entityArrow, Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback){
        initProjectileEntity(entityArrow, world, entity.getLookAngle(), random, power, damage, knockback);
    }

    public static void initProjectileEntity(AbstractArrow entityArrow, Level world, Vec3 angle, RandomSource random, float power, double damage, int knockback){
        entityArrow.shoot(angle.x, angle.y, angle.z, power * 2, 0);
        entityArrow.setSilent(true);
        entityArrow.setCritArrow(false);
        entityArrow.setBaseDamage(damage);
        entityArrow.setKnockback(knockback);
        world.addFreshEntity(entityArrow);
    }

}
