package com.ymrsl.utdassetmanager.common.blocktransform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformDeduplicator.Key;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformDeduplicator.Outcome;
import org.junit.jupiter.api.Test;

final class BlockTransformDeduplicatorTest {
    @Test
    void reusesOnlyTheSamePlayerDimensionPositionRuleAndGameTime() {
        BlockTransformDeduplicator deduplicator = new BlockTransformDeduplicator(8);
        Key key = new Key("player", "minecraft:overworld", 42L, "utd:block_transform/test");
        deduplicator.remember(key, 100L, Outcome.SUCCESS);

        assertEquals(Outcome.SUCCESS, deduplicator.find(key, 100L));
        assertNull(deduplicator.find(key, 101L));
        assertNull(deduplicator.find(new Key("other", key.dimension(), key.position(), key.ruleId()), 100L));
        assertNull(deduplicator.find(new Key(key.playerId(), "minecraft:the_nether", key.position(), key.ruleId()), 100L));
        assertNull(deduplicator.find(new Key(key.playerId(), key.dimension(), 43L, key.ruleId()), 100L));
        assertNull(deduplicator.find(new Key(key.playerId(), key.dimension(), key.position(), "other"), 100L));
    }

    @Test
    void separateInstancesKeepClientAndServerOutcomesIndependent() {
        Key key = new Key("player", "minecraft:overworld", 42L, "rule");
        BlockTransformDeduplicator client = new BlockTransformDeduplicator(8);
        BlockTransformDeduplicator server = new BlockTransformDeduplicator(8);
        client.remember(key, 5L, Outcome.SUCCESS);
        server.remember(key, 5L, Outcome.FAIL);
        assertEquals(Outcome.SUCCESS, client.find(key, 5L));
        assertEquals(Outcome.FAIL, server.find(key, 5L));
    }

    @Test
    void cacheIsBounded() {
        BlockTransformDeduplicator deduplicator = new BlockTransformDeduplicator(2);
        Key first = new Key("p", "d", 1L, "r");
        deduplicator.remember(first, 1L, Outcome.SUCCESS);
        deduplicator.remember(new Key("p", "d", 2L, "r"), 1L, Outcome.SUCCESS);
        deduplicator.remember(new Key("p", "d", 3L, "r"), 1L, Outcome.SUCCESS);
        assertNull(deduplicator.find(first, 1L));
    }
}
