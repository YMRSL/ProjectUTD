package com.scarasol.sona.effect;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class Stun extends PhysicalEffect {

    public Stun() {
        super(MobEffectCategory.HARMFUL, -10027060);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath("sona", "stun_movement_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath("sona", "stun_attack_damage"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.GRAVITY, ResourceLocation.fromNamespaceAndPath("sona", "stun_gravity"), 1000.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void onEffectStarted(@NotNull LivingEntity entity, int amplifier) {
        super.onEffectStarted(entity, amplifier);
        if (entity instanceof Player) {
            if (entity.level() instanceof ServerLevel serverLevel && entity.getServer() != null)
                entity.getServer().getCommands().performPrefixedCommand(
                        new CommandSourceStack(CommandSource.NULL, entity.position(),
                                entity.getRotationVector(), serverLevel, 4,
                                entity.getName().getString(),
                                entity.getDisplayName(), entity.getServer(), entity),
                        "playsound sona:tinnitus player @s");
        }
    }

}
