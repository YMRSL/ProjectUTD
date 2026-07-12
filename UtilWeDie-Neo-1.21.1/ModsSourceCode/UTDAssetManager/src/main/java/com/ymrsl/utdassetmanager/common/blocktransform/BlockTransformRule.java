package com.ymrsl.utdassetmanager.common.blocktransform;

import java.util.List;
import java.util.Map;

/**
 * Immutable, loader-independent representation of one right-click block transform.
 * Runtime registry and SNBT resolution deliberately happens in {@link BlockTransformRuntimeRules}.
 */
public record BlockTransformRule(
        String id,
        boolean enabled,
        int priority,
        Target target,
        Catalyst catalyst,
        Activation activation,
        Result result,
        Creative creative) {

    public BlockTransformRule {
        target = target == null ? new Target("", Map.of(), "reject") : target;
        catalyst = catalyst == null
                ? new Catalyst("", "", "{}", 1, CatalystSource.CLICKED_HAND, true)
                : catalyst;
        activation = activation == null ? new Activation(ActivationHand.MAIN, false, false) : activation;
        result = result == null ? new Result("", Map.of(), List.of()) : result;
        creative = creative == null ? new Creative(true, false) : creative;
    }

    public record Target(String block, Map<String, String> state, String blockEntityPolicy) {
        public Target {
            state = state == null ? Map.of() : Map.copyOf(state);
        }
    }

    public record Catalyst(
            String registryId,
            String variantDiscriminator,
            String componentsSnbt,
            int count,
            CatalystSource source,
            boolean consume) {
    }

    public record Activation(ActivationHand hand, boolean requireSneak, boolean allowFakePlayer) {
    }

    public record Result(String block, Map<String, String> state, List<String> copyProperties) {
        public Result {
            state = state == null ? Map.of() : Map.copyOf(state);
            copyProperties = copyProperties == null ? List.of() : List.copyOf(copyProperties);
        }
    }

    public record Creative(boolean requireInput, boolean consume) {
    }

    public enum CatalystSource {
        CLICKED_HAND,
        INVENTORY
    }

    public enum ActivationHand {
        MAIN,
        OFF,
        ANY
    }
}
