package com.ymrsl.utdassetmanager.common.blocktransform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRepository.Snapshot;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRepository.ValidationSnapshot;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRuntimeRules.ValidationResult;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class BlockTransformDiagnosticsTest {
    @Test
    void statusReportsPathGenerationCountsAndRuntimeErrors() {
        List<BlockTransformRule> rules = twoRules();
        Path path = Path.of("config/utd_asset_manager/block_transforms.json");
        Snapshot source = new Snapshot(7, rules, "", path);

        BlockTransformDiagnostics.Status usable = BlockTransformDiagnostics.summarize(source, "");
        assertEquals(path, usable.path());
        assertEquals(7, usable.generation());
        assertEquals(2, usable.total());
        assertEquals(1, usable.enabled());
        assertTrue(usable.usable());
        assertTrue(usable.error().isBlank());

        BlockTransformDiagnostics.Status failed = BlockTransformDiagnostics.summarize(
                source, "runtime validation failed: missing item");
        assertFalse(failed.usable());
        assertTrue(failed.error().contains("missing item"));
    }

    @Test
    void detachedValidationReportsRuntimeCompilationWithoutChangingAnActiveGeneration() {
        BlockTransformRule valid = BlockTransformConfigParser.parse(document(
                "minecraft:stone", "minecraft:stick", "minecraft:cobblestone")).rules().getFirst();
        ValidationResult validResult = new ValidationResult(1, "");
        assertTrue(validResult.usable());
        assertEquals(1, validResult.compiledEnabledCount());

        ValidationResult invalidResult = new ValidationResult(
                0, "runtime validation failed: missing block missing:block");
        assertFalse(invalidResult.usable());
        assertTrue(invalidResult.error().contains("missing block missing:block"));

        String sha256 = "a".repeat(64);
        ValidationSnapshot candidate = new ValidationSnapshot(
                List.of(valid), "", Path.of("candidate.json"), sha256);
        BlockTransformDiagnostics.Validation report = BlockTransformDiagnostics.summarize(candidate, validResult);
        assertEquals(sha256, report.sha256());
        assertEquals(1, report.total());
        assertEquals(1, report.enabled());
        assertTrue(report.usable());
    }

    @Test
    void managementCommandsRequireLevelTwoOrSingleplayerOwner() {
        assertFalse(BlockTransformCommandAccess.managementAllowed(false, false));
        assertTrue(BlockTransformCommandAccess.managementAllowed(true, false));
        assertTrue(BlockTransformCommandAccess.managementAllowed(false, true));
    }

    @Test
    void stagingPathsAndCommandDisplayAreFixedAndRelative() {
        Path active = Path.of("D:/games/profile/config/utd_asset_manager/block_transforms.json");
        assertEquals("block_transforms.candidate.json", BlockTransformPaths.candidateFor(active).getFileName().toString());
        assertEquals("block_transforms.json.bak", BlockTransformPaths.backupFor(active).getFileName().toString());
        assertEquals("config/utd_asset_manager/block_transforms.json", BlockTransformPaths.display(false));
        assertEquals("config/utd_asset_manager/block_transforms.candidate.json", BlockTransformPaths.display(true));
        assertFalse(Path.of(BlockTransformPaths.display(false)).isAbsolute());
        assertFalse(Path.of(BlockTransformPaths.display(true)).isAbsolute());
    }

    @Test
    void promotionHashPinRequiresLowercaseFullSha256() {
        assertTrue(BlockTransformStaging.isPinnedSha256("0123456789abcdef".repeat(4)));
        assertFalse(BlockTransformStaging.isPinnedSha256("0123456789ABCDEF".repeat(4)));
        assertFalse(BlockTransformStaging.isPinnedSha256("a".repeat(63)));
        assertFalse(BlockTransformStaging.isPinnedSha256("a".repeat(65)));
        assertFalse(BlockTransformStaging.isPinnedSha256(" " + "a".repeat(64)));
    }

    private static List<BlockTransformRule> twoRules() {
        BlockTransformRule enabled = BlockTransformConfigParser.parse(document(
                "minecraft:stone", "minecraft:stick", "minecraft:cobblestone")).rules().getFirst();
        BlockTransformRule disabled = new BlockTransformRule(
                "disabled",
                false,
                enabled.priority(),
                enabled.target(),
                enabled.catalyst(),
                enabled.activation(),
                enabled.result(),
                enabled.creative());
        return List.of(enabled, disabled);
    }

    private static String document(String target, String catalyst, String result) {
        return """
                {
                  "schema_version": "utd-block-transforms/v1",
                  "rules": [{
                    "id": "diagnostic_test",
                    "enabled": true,
                    "target": {"block": "%s"},
                    "catalyst": {"registryId": "%s"},
                    "result": {"block": "%s"}
                  }]
                }
                """.formatted(target, catalyst, result);
    }
}
