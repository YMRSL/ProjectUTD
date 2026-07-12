package com.ymrsl.utdassetmanager.common.blocktransform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.ActivationHand;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.CatalystSource;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class BlockTransformConfigParserTest {
    @Test
    void parsesAllV1FieldsAndSortsByPriority() {
        BlockTransformConfig config = BlockTransformConfigParser.parse("""
                {
                  "schema_version": "utd-block-transforms/v1",
                  "rules": [
                    {
                      "id": "low",
                      "enabled": true,
                      "priority": 1,
                      "target": {"block": "minecraft:stone", "state": {}, "blockEntityPolicy": "reject"},
                      "catalyst": {"registryId": "minecraft:stick", "count": 1},
                      "result": {"block": "minecraft:cobblestone"}
                    },
                    {
                      "id": "high",
                      "enabled": true,
                      "priority": 50,
                      "target": {
                        "block": "minecraft:oak_log",
                        "state": {"axis": "Y"},
                        "blockEntityPolicy": "reject"
                      },
                      "catalyst": {
                        "registryId": "minecraft:iron_axe",
                        "variantDiscriminator": "",
                        "componentsSnbt": "{}",
                        "count": 2,
                        "source": "inventory",
                        "consume": false
                      },
                      "activation": {"hand": "any", "requireSneak": true, "allowFakePlayer": true},
                      "result": {
                        "block": "minecraft:stripped_oak_log",
                        "state": {},
                        "copyProperties": ["axis"]
                      },
                      "creative": {"requireInput": true, "consume": true}
                    }
                  ]
                }
                """);

        assertEquals(BlockTransformConfig.SCHEMA, config.schema());
        assertEquals(2, config.rules().size());
        BlockTransformRule rule = config.rules().getFirst();
        assertEquals("high", rule.id());
        assertEquals(50, rule.priority());
        assertEquals("y", rule.target().state().get("axis"));
        assertEquals(CatalystSource.INVENTORY, rule.catalyst().source());
        assertEquals(2, rule.catalyst().count());
        assertFalse(rule.catalyst().consume());
        assertEquals(ActivationHand.ANY, rule.activation().hand());
        assertTrue(rule.activation().requireSneak());
        assertTrue(rule.activation().allowFakePlayer());
        assertEquals("axis", rule.result().copyProperties().getFirst());
        assertTrue(rule.creative().consume());
    }

    @Test
    void safeDefaultsRequireExplicitEnableAndUseClickedMainHand() {
        BlockTransformRule rule = BlockTransformConfigParser.parse("""
                {
                  "schema": "utd-block-transforms/v1",
                  "rules": [{
                    "id": "defaults",
                    "target": {"block": "minecraft:stone"},
                    "catalyst": {"registryId": "minecraft:stick"},
                    "result": {"block": "minecraft:cobblestone"}
                  }]
                }
                """).rules().getFirst();

        assertFalse(rule.enabled());
        assertEquals(0, rule.priority());
        assertEquals("reject", rule.target().blockEntityPolicy());
        assertEquals(CatalystSource.CLICKED_HAND, rule.catalyst().source());
        assertEquals(1, rule.catalyst().count());
        assertTrue(rule.catalyst().consume());
        assertEquals(ActivationHand.MAIN, rule.activation().hand());
        assertTrue(rule.creative().requireInput());
        assertFalse(rule.creative().consume());
    }

    @Test
    void malformedOrAmbiguousFilesFailAsAWhole() {
        assertThrows(IllegalArgumentException.class, () -> BlockTransformConfigParser.parse("""
                {"schema":"wrong","rules":[]}
                """));
        assertThrows(IllegalArgumentException.class, () -> BlockTransformConfigParser.parse("""
                {"schema_version":"utd-block-transforms/v1","schema":"wrong","rules":[]}
                """));
        assertThrows(IllegalArgumentException.class, () -> BlockTransformConfigParser.parse("""
                {
                  "schema":"utd-block-transforms/v1",
                  "rules":[
                    {"id":"same","target":{"block":"minecraft:stone"},"catalyst":{"registryId":"minecraft:stick"},"result":{"block":"minecraft:dirt"}},
                    {"id":"SAME","target":{"block":"minecraft:dirt"},"catalyst":{"registryId":"minecraft:stick"},"result":{"block":"minecraft:stone"}}
                  ]
                }
                """));
        assertThrows(IllegalArgumentException.class, () -> BlockTransformConfigParser.parse("""
                {
                  "schema_version":"utd-block-transforms/v1",
                  "rules":[{
                    "id":"disabled_draft",
                    "enabled":false,
                    "target":{"block":""},
                    "catalyst":{"registryId":"minecraft:stick"},
                    "result":{"block":""}
                  }]
                }
                """));
        assertThrows(IllegalArgumentException.class, () -> BlockTransformConfigParser.parse("""
                {
                  "schema_version":"utd-block-transforms/v1",
                  "rules":[{
                    "id":"bad id!",
                    "enabled":false,
                    "target":{"block":"minecraft:stone"},
                    "catalyst":{"registryId":"minecraft:stick"},
                    "result":{"block":"minecraft:cobblestone"}
                  }]
                }
                """));
        assertThrows(IllegalArgumentException.class, () -> BlockTransformConfigParser.parse("""
                {
                  "schema":"utd-block-transforms/v1",
                  "rules":[{
                    "id":"unsafe_creative",
                    "target":{"block":"minecraft:stone"},
                    "catalyst":{"registryId":"minecraft:stick"},
                    "result":{"block":"minecraft:dirt"},
                    "creative":{"requireInput":false,"consume":true}
                  }]
                }
                """));
    }

    @Test
    void checkedInExampleIsValidAndDisabled() throws Exception {
        try (var reader = Files.newBufferedReader(Path.of("examples/block_transforms.example.json"))) {
            BlockTransformConfig config = BlockTransformConfigParser.parse(reader);
            assertEquals(1, config.rules().size());
            assertFalse(config.rules().getFirst().enabled());
        }
    }

    @Test
    void acceptsWorkbenchNamespacedIdsAndLegacySchemaField() {
        BlockTransformRule rule = BlockTransformConfigParser.parse("""
                {
                  "schema":"utd-block-transforms/v1",
                  "rules":[{
                    "id":"utd:block_transform/strip_oak",
                    "target":{"block":"minecraft:oak_log"},
                    "catalyst":{"registryId":"minecraft:iron_axe"},
                    "result":{"block":"minecraft:stripped_oak_log"}
                  }]
                }
                """).rules().getFirst();
        assertEquals("utd:block_transform/strip_oak", rule.id());
    }

    @Test
    void acceptsWorkbenchExportShapeAndCanonicalizesRuleIdLikeTheExporter() {
        BlockTransformRule rule = BlockTransformConfigParser.parse("""
                {
                  "schema_version":"utd-block-transforms/v1",
                  "rules":[{
                    "id":"UTD:block_transform/Upper_Case",
                    "enabled":false,
                    "priority":0,
                    "target":{"block":"minecraft:stone","state":{},"blockEntityPolicy":"reject"},
                    "catalyst":{"registryId":"minecraft:stick","variantDiscriminator":"","componentsSnbt":"{}","count":1,"source":"clicked_hand","consume":true},
                    "activation":{"hand":"main","requireSneak":false,"allowFakePlayer":false},
                    "result":{"block":"minecraft:cobblestone","state":{},"copyProperties":[]},
                    "creative":{"requireInput":true,"consume":false}
                  }]
                }
                """).rules().getFirst();

        assertEquals("utd:block_transform/upper_case", rule.id());
    }
}
