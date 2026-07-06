package com.github.sculkhorde.common.advancement;

import com.github.sculkhorde.core.SculkHorde;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SculkHordeStartTrigger extends SimpleCriterionTrigger<SculkHordeStartTrigger.SculkHordeStartCriterion> implements CustomCriterionTrigger{

    public static final SculkHordeStartTrigger INSTANCE = new SculkHordeStartTrigger();

    /**
     * Need to be registered in {@link com.github.sculkhorde.util.ModEventSubscriber}.
     */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SculkHorde.MOD_ID, "sculk_horde_start");

    @Override
    public Codec<SculkHordeStartCriterion> codec() {
        return SculkHordeStartCriterion.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, criterion -> true);
    }

    public record SculkHordeStartCriterion(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<SculkHordeStartCriterion> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SculkHordeStartCriterion::player)
                ).apply(instance, SculkHordeStartCriterion::new)
        );
    }
}
