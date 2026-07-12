package com.ymrsl.utdassetmanager.common.blocktransform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BlockTransformRepositoryTest {
    @TempDir
    Path temporary;

    @Test
    void missingFileGetsSafeEmptyDocument() throws Exception {
        Path path = temporary.resolve("config/utd_asset_manager/block_transforms.json");
        BlockTransformRepository.Snapshot snapshot = new BlockTransformRepository(path).snapshot();
        assertTrue(snapshot.usable());
        assertTrue(snapshot.rules().isEmpty());
        assertTrue(Files.exists(path));
        String document = Files.readString(path);
        assertTrue(document.contains("\"schema_version\""));
        assertTrue(document.contains(BlockTransformConfig.SCHEMA));
    }

    @Test
    void malformedExistingFileIsPreservedAndDisablesEverything() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        String malformed = "{ definitely not valid json";
        Files.writeString(path, malformed, StandardCharsets.UTF_8);

        BlockTransformRepository.Snapshot snapshot = new BlockTransformRepository(path).snapshot();
        assertFalse(snapshot.usable());
        assertTrue(snapshot.rules().isEmpty());
        assertTrue(snapshot.error().contains("invalid config"));
        assertEquals(malformed, Files.readString(path, StandardCharsets.UTF_8));
    }
}
