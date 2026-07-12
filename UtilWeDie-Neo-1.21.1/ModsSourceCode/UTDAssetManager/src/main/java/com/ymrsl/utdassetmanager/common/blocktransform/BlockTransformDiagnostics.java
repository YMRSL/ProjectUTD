package com.ymrsl.utdassetmanager.common.blocktransform;

import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRepository.Snapshot;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRepository.ValidationSnapshot;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRuntimeRules.CompiledSet;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRuntimeRules.ValidationResult;
import java.nio.file.Path;

/** Read-only diagnostics facade used by server commands and acceptance checks. */
public final class BlockTransformDiagnostics {
    private BlockTransformDiagnostics() {
    }

    /** Returns the active file generation and the runtime compilation state. */
    public static Status status() {
        Snapshot source = BlockTransformRepository.get().snapshot();
        CompiledSet runtime = BlockTransformRuntimeRules.current();
        return summarize(source, runtime.error());
    }

    /** Forces the source file into a new active generation and compiles that generation. */
    public static Status reload() {
        Snapshot source = BlockTransformRepository.get().forceReload();
        CompiledSet runtime = BlockTransformRuntimeRules.current();
        return summarize(source, runtime.error());
    }

    /** Parses and compiles a detached candidate without changing the active generation. */
    public static Validation validate() {
        ValidationSnapshot source = BlockTransformRepository.get().validationSnapshot();
        ValidationResult runtime = source.usable()
                ? BlockTransformRuntimeRules.validate(source.rules())
                : new ValidationResult(0, source.error());
        return summarize(source, runtime);
    }

    /** Hash-pins, re-validates and atomically activates the fixed candidate. */
    public static Promotion promote(String expectedSha256) {
        BlockTransformRepository.PromotionAttempt attempt = BlockTransformRepository.get().promoteCandidate(
                expectedSha256,
                rules -> BlockTransformRuntimeRules.validate(rules).error());
        CompiledSet runtime = BlockTransformRuntimeRules.current();
        Status active = summarize(attempt.active(), runtime.error());
        String error = attempt.error().isBlank() ? active.error() : attempt.error();
        boolean usable = attempt.promoted() && active.usable() && error.isBlank();
        return new Promotion(
                BlockTransformPaths.candidateFor(attempt.active().path()),
                attempt.active().path(),
                attempt.candidateSha256(),
                attempt.active().generation(),
                attempt.active().rules().size(),
                enabledCount(attempt.active().rules()),
                attempt.promoted(),
                usable,
                error);
    }

    static Status summarize(Snapshot source, String runtimeError) {
        String error = source.error().isBlank() ? safe(runtimeError) : source.error();
        return new Status(
                source.path(),
                source.generation(),
                source.rules().size(),
                enabledCount(source.rules()),
                error.isBlank(),
                error);
    }

    static Validation summarize(ValidationSnapshot source, ValidationResult runtime) {
        String error = source.error().isBlank() ? runtime.error() : source.error();
        return new Validation(
                source.path(),
                source.sha256(),
                source.rules().size(),
                enabledCount(source.rules()),
                error.isBlank(),
                error);
    }

    private static int enabledCount(Iterable<BlockTransformRule> rules) {
        int enabled = 0;
        for (BlockTransformRule rule : rules) {
            if (rule.enabled()) enabled++;
        }
        return enabled;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public record Status(Path path, long generation, int total, int enabled, boolean usable, String error) {
        public Status {
            error = safe(error);
        }
    }

    public record Validation(Path path, String sha256, int total, int enabled, boolean usable, String error) {
        public Validation {
            sha256 = safe(sha256);
            error = safe(error);
        }
    }

    public record Promotion(
            Path candidatePath,
            Path activePath,
            String sha256,
            long generation,
            int total,
            int enabled,
            boolean promoted,
            boolean usable,
            String error) {
        public Promotion {
            sha256 = safe(sha256);
            error = safe(error);
        }
    }
}
