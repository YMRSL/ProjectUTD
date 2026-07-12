package com.ymrsl.utdassetmanager.common.blocktransform;

import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.ActivationHand;
import java.util.Map;

/** Pure rule matching used by both the game adapter and unit tests. */
public final class BlockTransformMatcher {
    private BlockTransformMatcher() {
    }

    public static boolean matches(BlockTransformRule rule, InteractionView interaction, int availableCatalystCount) {
        if (rule == null || interaction == null || !rule.enabled()) return false;
        if (!rule.target().block().equals(interaction.targetBlock())) return false;
        for (Map.Entry<String, String> required : rule.target().state().entrySet()) {
            if (!required.getValue().equals(interaction.targetState().get(required.getKey()))) return false;
        }
        if (!handMatches(rule.activation().hand(), interaction.hand())) return false;
        if (rule.activation().requireSneak() && !interaction.sneaking()) return false;
        if (interaction.fakePlayer() && !rule.activation().allowFakePlayer()) return false;
        boolean inputRequired = !interaction.creative() || rule.creative().requireInput();
        return !inputRequired || availableCatalystCount >= rule.catalyst().count();
    }

    private static boolean handMatches(ActivationHand expected, ActivationHand actual) {
        return expected == ActivationHand.ANY || expected == actual;
    }

    public record InteractionView(
            String targetBlock,
            Map<String, String> targetState,
            ActivationHand hand,
            boolean sneaking,
            boolean fakePlayer,
            boolean creative) {
        public InteractionView {
            targetState = targetState == null ? Map.of() : Map.copyOf(targetState);
        }
    }
}
