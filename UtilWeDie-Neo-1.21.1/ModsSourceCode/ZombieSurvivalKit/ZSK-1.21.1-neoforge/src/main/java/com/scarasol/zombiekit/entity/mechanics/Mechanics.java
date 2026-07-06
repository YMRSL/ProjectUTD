package com.scarasol.zombiekit.entity.mechanics;

import com.scarasol.sona.effect.PhysicalEffect;
import com.scarasol.sona.init.SonaDamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;

public abstract class Mechanics extends PathfinderMob{


    public Mechanics(EntityType<? extends Mechanics> type, Level world) {
        super(type, world);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ThrownPotion || source.getDirectEntity() instanceof AreaEffectCloud)
            return false;
        if (source.is(DamageTypes.MAGIC))
            return false;
        if (source.is(DamageTypes.WITHER) || source.is(DamageTypes.FREEZE) || source.is(DamageTypes.IN_WALL))
            return false;
        if (source.getMsgId().equals("witherSkull") || source.is(DamageTypes.CACTUS))
            return false;
        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LIGHTNING_BOLT))
            return super.hurt(source, amount);
        if (source.is(SonaDamageTypes.CORROSION) || source.is(DamageTypes.FALL) || source.is(DamageTypes.FELL_OUT_OF_WORLD))
            return super.hurt(source, amount);
        return super.hurt(source, 1f);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance instance) {
        return instance.getEffect().value() instanceof PhysicalEffect;
    }
}
