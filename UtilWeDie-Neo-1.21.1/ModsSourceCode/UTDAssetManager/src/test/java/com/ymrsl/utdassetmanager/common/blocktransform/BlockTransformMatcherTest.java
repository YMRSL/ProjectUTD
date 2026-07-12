package com.ymrsl.utdassetmanager.common.blocktransform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformMatcher.InteractionView;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Activation;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.ActivationHand;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Catalyst;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.CatalystSource;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Creative;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Result;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Target;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class BlockTransformMatcherTest {
    private static BlockTransformRule rule() {
        return new BlockTransformRule(
                "test",
                true,
                10,
                new Target("minecraft:oak_log", Map.of("axis", "y"), "reject"),
                new Catalyst("minecraft:stick", "", "{}", 2, CatalystSource.CLICKED_HAND, true),
                new Activation(ActivationHand.MAIN, true, false),
                new Result("minecraft:stripped_oak_log", Map.of(), List.of("axis")),
                new Creative(true, false));
    }

    @Test
    void requiresTargetStateActivationAndEnoughInput() {
        InteractionView matching = new InteractionView(
                "minecraft:oak_log", Map.of("axis", "y"), ActivationHand.MAIN, true, false, false);
        assertTrue(BlockTransformMatcher.matches(rule(), matching, 2));
        assertFalse(BlockTransformMatcher.matches(rule(), matching, 1));
        assertFalse(BlockTransformMatcher.matches(rule(), new InteractionView(
                "minecraft:oak_log", Map.of("axis", "x"), ActivationHand.MAIN, true, false, false), 2));
        assertFalse(BlockTransformMatcher.matches(rule(), new InteractionView(
                "minecraft:oak_log", Map.of("axis", "y"), ActivationHand.OFF, true, false, false), 2));
        assertFalse(BlockTransformMatcher.matches(rule(), new InteractionView(
                "minecraft:oak_log", Map.of("axis", "y"), ActivationHand.MAIN, false, false, false), 2));
        assertFalse(BlockTransformMatcher.matches(rule(), new InteractionView(
                "minecraft:oak_log", Map.of("axis", "y"), ActivationHand.MAIN, true, true, false), 2));
    }

    @Test
    void creativeMayExplicitlySkipInputButDisabledRulesNeverMatch() {
        BlockTransformRule noCreativeInput = new BlockTransformRule(
                "creative",
                true,
                0,
                new Target("minecraft:stone", Map.of(), "reject"),
                new Catalyst("minecraft:stick", "", "{}", 64, CatalystSource.INVENTORY, true),
                new Activation(ActivationHand.ANY, false, false),
                new Result("minecraft:dirt", Map.of(), List.of()),
                new Creative(false, false));
        InteractionView creative = new InteractionView(
                "minecraft:stone", Map.of(), ActivationHand.OFF, false, false, true);
        assertTrue(BlockTransformMatcher.matches(noCreativeInput, creative, 0));

        BlockTransformRule disabled = new BlockTransformRule(
                noCreativeInput.id(), false, noCreativeInput.priority(), noCreativeInput.target(),
                noCreativeInput.catalyst(), noCreativeInput.activation(), noCreativeInput.result(), noCreativeInput.creative());
        assertFalse(BlockTransformMatcher.matches(disabled, creative, 64));
    }
}
