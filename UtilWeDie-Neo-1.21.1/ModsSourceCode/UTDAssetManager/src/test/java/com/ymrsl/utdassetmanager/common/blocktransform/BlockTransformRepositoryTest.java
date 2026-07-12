package com.ymrsl.utdassetmanager.common.blocktransform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Test
    void statusAndRightClickSnapshotNeverHotReloadDiskChanges() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        String priorityOne = documentWithPriority(1);
        String priorityTwo = documentWithPriority(2);
        Files.writeString(path, priorityOne, StandardCharsets.UTF_8);

        BlockTransformRepository repository = new BlockTransformRepository(path);
        BlockTransformRepository.Snapshot first = repository.snapshot();
        assertEquals(1, first.rules().getFirst().priority());

        Files.writeString(path, priorityTwo, StandardCharsets.UTF_8);
        BlockTransformRepository.Snapshot stillActive = repository.snapshot();
        assertEquals(first.generation(), stillActive.generation());
        assertEquals(1, stillActive.rules().getFirst().priority());

        BlockTransformRepository.Snapshot forced = repository.forceReload();
        assertEquals(first.generation() + 1, forced.generation());
        assertEquals(2, forced.rules().getFirst().priority());
    }

    @Test
    void validationSnapshotDoesNotActivateCandidate() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        Path candidatePath = BlockTransformPaths.candidateFor(path);
        String priorityOne = documentWithPriority(1);
        String priorityTwo = documentWithPriority(2);
        Files.writeString(path, priorityOne, StandardCharsets.UTF_8);
        Files.writeString(candidatePath, priorityTwo, StandardCharsets.UTF_8);

        BlockTransformRepository repository = new BlockTransformRepository(path);
        BlockTransformRepository.Snapshot active = repository.snapshot();

        BlockTransformRepository.ValidationSnapshot candidate = repository.validationSnapshot();
        assertTrue(candidate.usable());
        assertEquals(candidatePath, candidate.path());
        assertEquals(BlockTransformStaging.sha256(Files.readAllBytes(candidatePath)), candidate.sha256());
        assertEquals(2, candidate.rules().getFirst().priority());
        BlockTransformRepository.Snapshot stillActive = repository.snapshot();
        assertEquals(active.generation(), stillActive.generation());
        assertEquals(1, stillActive.rules().getFirst().priority());
    }

    @Test
    void failedRuntimeValidationNeverActivatesCandidate() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        String activeDocument = documentWithPriority(1);
        String candidateDocument = documentWithPriority(2);
        Files.writeString(path, activeDocument, StandardCharsets.UTF_8);
        Files.writeString(BlockTransformPaths.candidateFor(path), candidateDocument, StandardCharsets.UTF_8);
        BlockTransformRepository repository = new BlockTransformRepository(path);
        long generation = repository.snapshot().generation();
        String sha = BlockTransformStaging.sha256(candidateDocument.getBytes(StandardCharsets.UTF_8));

        BlockTransformRepository.PromotionAttempt result = repository.promoteCandidate(
                sha, rules -> "runtime validation failed: rejected by test registry");

        assertFalse(result.promoted());
        assertEquals(generation, result.active().generation());
        assertEquals(activeDocument, Files.readString(path, StandardCharsets.UTF_8));
        assertFalse(Files.exists(BlockTransformPaths.backupFor(path)));
    }

    @Test
    void hashMismatchNeverChangesActiveOrBackup() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        String activeDocument = documentWithPriority(1);
        String candidateDocument = documentWithPriority(2);
        String existingBackup = "previous-known-good";
        Files.writeString(path, activeDocument, StandardCharsets.UTF_8);
        Files.writeString(BlockTransformPaths.candidateFor(path), candidateDocument, StandardCharsets.UTF_8);
        Files.writeString(BlockTransformPaths.backupFor(path), existingBackup, StandardCharsets.UTF_8);
        BlockTransformRepository repository = new BlockTransformRepository(path);
        long generation = repository.snapshot().generation();

        BlockTransformRepository.PromotionAttempt result = repository.promoteCandidate("0".repeat(64), rules -> "");

        assertFalse(result.promoted());
        assertTrue(result.error().contains("mismatch"));
        assertEquals(generation, result.active().generation());
        assertEquals(activeDocument, Files.readString(path, StandardCharsets.UTF_8));
        assertEquals(existingBackup, Files.readString(BlockTransformPaths.backupFor(path), StandardCharsets.UTF_8));
    }

    @Test
    void successfulPromotionBacksUpActiveAndLoadsExactlyPinnedCandidate() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        String activeDocument = documentWithPriority(1);
        String candidateDocument = documentWithPriority(2);
        Files.writeString(path, activeDocument, StandardCharsets.UTF_8);
        Files.writeString(BlockTransformPaths.candidateFor(path), candidateDocument, StandardCharsets.UTF_8);
        BlockTransformRepository repository = new BlockTransformRepository(path);
        long generation = repository.snapshot().generation();
        String sha = BlockTransformStaging.sha256(candidateDocument.getBytes(StandardCharsets.UTF_8));

        BlockTransformRepository.PromotionAttempt result = repository.promoteCandidate(sha, rules -> "");

        assertTrue(result.promoted());
        assertEquals(sha, result.candidateSha256());
        assertEquals(generation + 1, result.active().generation());
        assertEquals(2, result.active().rules().getFirst().priority());
        assertEquals(candidateDocument, Files.readString(path, StandardCharsets.UTF_8));
        assertEquals(activeDocument, Files.readString(BlockTransformPaths.backupFor(path), StandardCharsets.UTF_8));
    }

    @Test
    void activeMoveFailureLeavesActiveUntouchedAndCompleteBackupAvailable() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        String activeDocument = documentWithPriority(1);
        String candidateDocument = documentWithPriority(2);
        Files.writeString(path, activeDocument, StandardCharsets.UTF_8);
        Files.writeString(BlockTransformPaths.candidateFor(path), candidateDocument, StandardCharsets.UTF_8);
        AtomicInteger moves = new AtomicInteger();
        BlockTransformRepository repository = new BlockTransformRepository(path, (source, target) -> {
            if (moves.incrementAndGet() == 2) throw new java.io.IOException("simulated active move failure");
            BlockTransformStaging.atomicReplace(source, target);
        });
        long generation = repository.snapshot().generation();
        String sha = BlockTransformStaging.sha256(candidateDocument.getBytes(StandardCharsets.UTF_8));

        BlockTransformRepository.PromotionAttempt result = repository.promoteCandidate(sha, rules -> "");

        assertFalse(result.promoted());
        assertEquals(generation, result.active().generation());
        assertEquals(activeDocument, Files.readString(path, StandardCharsets.UTF_8));
        assertEquals(activeDocument, Files.readString(BlockTransformPaths.backupFor(path), StandardCharsets.UTF_8));
    }

    @Test
    void postMoveReloadFailureAtomicallyRestoresBackupAndPreviousGeneration() throws Exception {
        Path path = temporary.resolve("block_transforms.json");
        String activeDocument = documentWithPriority(1);
        String candidateDocument = documentWithPriority(2);
        Files.writeString(path, activeDocument, StandardCharsets.UTF_8);
        Files.writeString(BlockTransformPaths.candidateFor(path), candidateDocument, StandardCharsets.UTF_8);
        AtomicInteger loads = new AtomicInteger();
        BlockTransformRepository repository = new BlockTransformRepository(
                path,
                BlockTransformStaging::atomicReplace,
                source -> {
                    if (loads.incrementAndGet() == 2) {
                        throw new java.io.IOException("simulated post-move read failure");
                    }
                    return BlockTransformConfigParser.parse(Files.readString(source, StandardCharsets.UTF_8));
                });
        BlockTransformRepository.Snapshot before = repository.snapshot();
        String sha = BlockTransformStaging.sha256(candidateDocument.getBytes(StandardCharsets.UTF_8));

        BlockTransformRepository.PromotionAttempt result = repository.promoteCandidate(sha, rules -> "");

        assertFalse(result.promoted());
        assertTrue(result.error().contains("restored"));
        assertEquals(before.generation(), result.active().generation());
        assertEquals(1, result.active().rules().getFirst().priority());
        assertEquals(activeDocument, Files.readString(path, StandardCharsets.UTF_8));
        assertEquals(activeDocument, Files.readString(BlockTransformPaths.backupFor(path), StandardCharsets.UTF_8));
    }

    private static String documentWithPriority(int priority) {
        return """
                {
                  "schema_version": "utd-block-transforms/v1",
                  "rules": [{
                    "id": "reload_test",
                    "enabled": true,
                    "priority": %d,
                    "target": {"block": "minecraft:stone"},
                    "catalyst": {"registryId": "minecraft:stick"},
                    "result": {"block": "minecraft:cobblestone"}
                  }]
                }
                """.formatted(priority);
    }
}
