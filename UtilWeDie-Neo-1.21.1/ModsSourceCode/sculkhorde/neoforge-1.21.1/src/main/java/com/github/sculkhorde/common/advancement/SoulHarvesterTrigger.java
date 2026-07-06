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

public class SoulHarvesterTrigger extends SimpleCriterionTrigger<SoulHarvesterTrigger.SoulHarvesterCriterion> implements CustomCriterionTrigger{

    public static final SoulHarvesterTrigger INSTANCE = new SoulHarvesterTrigger();

    /**
     * Need to be registered in {@link com.github.sculkhorde.util.ModEventSubscriber}.
     */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SculkHorde.MOD_ID, "soul_harvester_trigger");

    @Override
    public Codec<SoulHarvesterCriterion> codec() {
        return SoulHarvesterCriterion.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, criterion -> true);
    }

    public record SoulHarvesterCriterion(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<SoulHarvesterCriterion> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(SoulHarvesterCriterion::player)
                ).apply(instance, SoulHarvesterCriterion::new)
        );
    }
}
