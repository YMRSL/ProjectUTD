package com.ymrsl.utdassetmanager.common.blocktransform;

import java.util.List;

public record BlockTransformConfig(String schema, List<BlockTransformRule> rules) {
    public static final String SCHEMA = "utd-block-transforms/v1";

    public BlockTransformConfig {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public static BlockTransformConfig empty() {
        return new BlockTransformConfig(SCHEMA, List.of());
    }
}
