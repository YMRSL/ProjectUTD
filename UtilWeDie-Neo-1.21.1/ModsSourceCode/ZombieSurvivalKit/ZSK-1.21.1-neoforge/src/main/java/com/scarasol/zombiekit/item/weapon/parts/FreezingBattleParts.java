package com.scarasol.zombiekit.item.weapon.parts;

import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitDamageTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class FreezingBattleParts extends BattleParts {

    public FreezingBattleParts(Properties properties, int partsLevel) {
        super(properties, partsLevel);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack itemstack, TooltipContext context, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.general_parts.description"));
        list.add(Component.translatable("item.zombiekit.freezing_battle_parts.description_" + (getPartsLevel() + 1)));
    }

    @Override
    public void partsEffect(LivingEntity attacker, LivingEntity target, float damage) {
        target.invulnerableTime = 0;
        float actualDamage;
        int amplifier = (target.hasEffect(SonaMobEffects.FROST)) ? target.getEffect(SonaMobEffects.FROST).getAmplifier() : -1;
        int duration;
        int partsLevel = getPartsLevel();
        if (partsLevel == 0) {
            amplifier = Math.min(amplifier + 1, 2);
            duration = 60;
            actualDamage = damage * 0.5f;
        } else if (partsLevel == 1) {
            amplifier = Math.min(amplifier + 2, 2);
            duration = 60;
            actualDamage = damage * 0.65f;
        } else {
            amplifier = Math.min(amplifier + 2, 2);
            duration = 80;
            actualDamage = damage * 0.8f;
        }
        target.addEffect(new MobEffectInstance(SonaMobEffects.FROST, duration, amplifier, false, false));
        target.hurt(getDamageSource(attacker.level(), attacker), (float) (actualDamage * CommonConfig.DAMAGE_COEFFICIENT.get()));
        if (target.level() instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5D), target.getZ(), 30, 0.2, 0.2, 0.2, 0.1);
    }

    public DamageSource getDamageSource(Level level, LivingEntity attacker) {
        return ZombieKitDamageTypes.damageSource(level.registryAccess(), DamageTypes.FREEZE, attacker);
    }
}
