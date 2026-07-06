package com.scarasol.zombiekit.item.weapon.parts;

import com.scarasol.sona.util.SonaMath;
import com.scarasol.zombiekit.item.api.DoubleHandWeapon;
import com.scarasol.zombiekit.item.api.ModifiableWeapon;
import com.scarasol.zombiekit.item.api.Parts;
import com.scarasol.zombiekit.item.api.SingleHandWeapon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class ChargingParts extends Item implements Parts {

    private final int partsLevel;
    private final int chargingTime = 200;


    public ChargingParts(Properties properties, int partsLevel) {
        super(properties);
        this.partsLevel = partsLevel;
    }

    public abstract void partsEffect(LivingEntity target, LivingEntity attacker, float damage);

    public boolean isOnCoolDown(ItemStack itemStack, long currentTime) {
        return currentTime - getCooldownTime(itemStack) < this.getChargingTime();
    }

    public int getChargingTime() {
        if (this.partsLevel == 0)
            return 200;
        else if (this.partsLevel == 1)
            return 180;
        return 160;
    }

    public long getCooldownTime(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ModifiableWeapon && itemStack.hasTag())
            return itemStack.getTag().getLong("ReleaseTime");
        return -1L;
    }

    public void setCooldownTime(ItemStack itemStack, long releaseTime) {
        itemStack.getOrCreateTag().putLong("ReleaseTime", releaseTime);
    }

    public abstract double getRange();

    @Override
    public int getPartsLevel() {
        return this.partsLevel;
    }

    @Override
    public PartsType getPartsType() {
        return PartsType.CHARGING;
    }

    public void attack(ServerLevel serverLevel, LivingEntity attacker, float damage) {
        ItemStack itemStack = attacker.getMainHandItem();
        if (isOnCoolDown(itemStack, serverLevel.getGameTime()))
            return;
        Vec3 center = attacker.getOnPos().getCenter();
        sweepAttack(attacker);
        setCooldownTime(itemStack, serverLevel.getGameTime());
        serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(getRange()), target -> shouldTakeEffect(attacker, target))
                .forEach(target -> partsEffect(target, attacker, damage));
    }

    public void sweepAttack(LivingEntity attacker) {
        attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, attacker.getSoundSource(), 1.0F, 1.0F);
        double d0 = -Mth.sin(attacker.getYRot() * ((float)Math.PI / 180F));
        double d1 = Mth.cos(attacker.getYRot() * ((float)Math.PI / 180F));
        if (attacker.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, attacker.getX() + d0, attacker.getY(0.5D), attacker.getZ() + d1, 0, d0, 0.0D, d1, 0.0D);
        }
    }

    public boolean shouldTakeEffect(LivingEntity attacker, LivingEntity target) {
        if (target.equals(attacker))
            return false;
        if (!target.hasLineOfSight(attacker))
            return false;
        Vec3 position = attacker.position();
        Vec3 lookAngle = attacker.getViewVector(1);
        Vec3 vec3 = target.position().subtract(position);
        return SonaMath.vectorDegreeCalculate(lookAngle, vec3) < 75;
    }

    public static boolean unlock(ItemStack itemStack) {
        return itemStack.getItem() instanceof TieredItem tieredItem && (itemStack.getItem() instanceof DoubleHandWeapon || tieredItem.getTier() == Tiers.NETHERITE);
    }

    @Override
    public boolean canUse(ItemStack itemStack) {
        if (itemStack.getItem() instanceof TieredItem tieredItem && (itemStack.getItem() instanceof DoubleHandWeapon || tieredItem.getTier() == Tiers.NETHERITE)) {
            if (tieredItem.getTier() instanceof Tiers tiers) {
                if (tiers == Tiers.NETHERITE)
                    return true;
                return partsLevel < 2;
            }
        }
        return false;
    }
}
