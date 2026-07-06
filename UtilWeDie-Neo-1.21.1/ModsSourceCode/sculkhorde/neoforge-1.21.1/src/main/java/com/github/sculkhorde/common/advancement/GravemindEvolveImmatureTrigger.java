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

public class GravemindEvolveImmatureTrigger extends SimpleCriterionTrigger<GravemindEvolveImmatureTrigger.GravemindEvoleImmatureCriterion> implements CustomCriterionTrigger{

    public static final GravemindEvolveImmatureTrigger INSTANCE = new GravemindEvolveImmatureTrigger();

    /**
     * Need to be registered in {@link com.github.sculkhorde.util.ModEventSubscriber}.
     */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(SculkHorde.MOD_ID, "gravemind_evolve_immature_trigger");

    @Override
    public Codec<GravemindEvoleImmatureCriterion> codec() {
        return GravemindEvoleImmatureCriterion.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, criterion -> true);
    }

    public record GravemindEvoleImmatureCriterion(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<GravemindEvoleImmatureCriterion> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(GravemindEvoleImmatureCriterion::player)
                ).apply(instance, GravemindEvoleImmatureCriterion::new)
        );
    }
}
