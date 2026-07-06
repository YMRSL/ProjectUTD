package com.scarasol.zombiekit.item.weapon;

import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitParticleTypes;
import com.scarasol.zombiekit.item.api.BaseFuelCanister;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class FuelCanister extends Item implements BaseFuelCanister {
    private final FuelType fuelType;

    public FuelCanister(Properties properties, FuelType fuelType) {
        super(properties);
        this.fuelType = fuelType;
    }

    @Override
    public void canisterEffect(LivingEntity target) {
        int damage = 0;
        if (fuelType == FuelType.NORMAL) {
            damage = CommonConfig.FUEL_DAMAGE.get();
            target.addEffect(new MobEffectInstance(SonaMobEffects.IGNITION, 110, 1, false, false));
        }else if (fuelType == FuelType.HIGH_TEMPERATURE) {
            damage = CommonConfig.HIGH_TEMPERATURE_FUEL_DAMAGE.get();
            target.addEffect(new MobEffectInstance(SonaMobEffects.IGNITION, 150, 3, false, false));
        }else if (fuelType == FuelType.NAPALM) {
            damage = CommonConfig.NAPALM_FUEL_DAMAGE.get();
            target.addEffect(new MobEffectInstance(SonaMobEffects.IGNITION, 230, Math.max(damage - 1, 1), false, false));
            target.addEffect(new MobEffectInstance(SonaMobEffects.SLIMINESS, 230, 0, false, false));
        }
        target.hurt(target.level().damageSources().inFire(), damage);
    }

    @NotNull
    @Override
    public ParticleType<?> getParticleType() {
        return switch (fuelType) {
            case HIGH_TEMPERATURE -> ZombieKitParticleTypes.LARGE_SOUL_FLAME.get();
            case NAPALM -> ZombieKitParticleTypes.LARGE_NAPALM_FLAME.get();
            default -> ZombieKitParticleTypes.LARGE_FLAME.get();
        };
    }

    @Override
    public @NotNull String getTexture() {
        return switch (fuelType) {
            case HIGH_TEMPERATURE -> "flamethrower_high_temperature";
            case NAPALM -> "flamethrower_napalm";
            default -> "flamethrower";
        };
    }

    public enum FuelType {
        NORMAL,
        HIGH_TEMPERATURE,
        NAPALM
    }
}
