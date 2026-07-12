package com.ymrsl.utdassetmanager.common.blocktransform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** Pure candidate inspection plus fail-closed, same-directory atomic promotion. */
final class BlockTransformStaging {
    private static final Pattern PINNED_SHA256 = Pattern.compile("[0-9a-f]{64}");

    private BlockTransformStaging() {
    }

    static Candidate inspect(Path candidatePath) {
        if (!Files.isRegularFile(candidatePath)) {
            return Candidate.failed(candidatePath, "candidate is missing");
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(candidatePath);
        } catch (IOException error) {
            return Candidate.failed(candidatePath, "cannot read candidate: " + error.getMessage());
        }
        String sha256 = sha256(bytes);
        try {
            String json = decodeUtf8Strict(bytes);
            BlockTransformConfig config = BlockTransformConfigParser.parse(json);
            return new Candidate(candidatePath, sha256, config.rules(), bytes, "");
        } catch (RuntimeException | CharacterCodingException error) {
            return new Candidate(candidatePath, sha256, List.of(), bytes,
                    "invalid candidate: " + error.getMessage());
        }
    }

    static Promotion promote(
            Path activePath,
            Path candidatePath,
            String expectedSha256,
            CandidateValidator validator,
            AtomicMover mover) {
        activePath = activePath.toAbsolutePath().normalize();
        candidatePath = candidatePath.toAbsolutePath().normalize();
        String expected = expectedSha256 == null ? "" : expectedSha256;
        if (!isPinnedSha256(expected)) {
            return Promotion.failed("", "expected SHA-256 must be exactly 64 lowercase hexadecimal characters");
        }

        // Intentionally re-read for every promotion. A previous validate command is informative only;
        // the hash pin and the live compiler are the authorization boundary.
        Candidate candidate = inspect(candidatePath);
        if (!candidate.usable()) return Promotion.failed(candidate.sha256(), candidate.error());
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                candidate.sha256().getBytes(StandardCharsets.US_ASCII))) {
            return Promotion.failed(candidate.sha256(),
                    "candidate SHA-256 mismatch: expected " + expected + ", actual " + candidate.sha256());
        }

        String compileError;
        try {
            compileError = validator.validate(candidate.rules());
        } catch (RuntimeException error) {
            compileError = "runtime validation failed: " + error.getMessage();
        }
        if (compileError != null && !compileError.isBlank()) {
            return Promotion.failed(candidate.sha256(), compileError);
        }
        if (!Files.isRegularFile(activePath)) {
            return Promotion.failed(candidate.sha256(), "active config is missing");
        }

        Path parent = activePath.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            return Promotion.failed(candidate.sha256(), "active config has no parent directory");
        }
        Path backupPath = BlockTransformPaths.backupFor(activePath);
        Path stagedBackup = null;
        Path stagedActive = null;
        try {
            byte[] activeBytes = Files.readAllBytes(activePath);
            stagedBackup = stage(parent, "." + BlockTransformPaths.BACKUP_FILE_NAME + ".", activeBytes);
            stagedActive = stage(parent, "." + BlockTransformPaths.ACTIVE_FILE_NAME + ".", candidate.bytes());

            // Backup is committed first. If the active replacement then fails, active is untouched and
            // the newly committed .bak is a complete copy of that still-active document.
            mover.replace(stagedBackup, backupPath);
            stagedBackup = null;
            mover.replace(stagedActive, activePath);
            stagedActive = null;
            return Promotion.succeeded(candidate.sha256());
        } catch (IOException error) {
            return Promotion.failed(candidate.sha256(), "atomic promotion failed; active config was not activated: "
                    + error.getMessage());
        } finally {
            deleteQuietly(stagedBackup);
            deleteQuietly(stagedActive);
        }
    }

    static boolean isPinnedSha256(String value) {
        return value != null && PINNED_SHA256.matcher(value).matches();
    }

    /** Restores active from a copied staging file while retaining the only .bak in place. */
    static Restore restoreBackup(Path activePath, AtomicMover mover) {
        activePath = activePath.toAbsolutePath().normalize();
        Path backupPath = BlockTransformPaths.backupFor(activePath);
        if (!Files.isRegularFile(backupPath)) {
            return new Restore(false, "backup is missing");
        }
        Path parent = activePath.toAbsolutePath().normalize().getParent();
        if (parent == null) return new Restore(false, "active config has no parent directory");
        Path stagedRestore = null;
        try {
            stagedRestore = stage(parent, "." + BlockTransformPaths.ACTIVE_FILE_NAME + ".rollback.",
                    Files.readAllBytes(backupPath));
            mover.replace(stagedRestore, activePath);
            stagedRestore = null;
            return new Restore(true, "");
        } catch (IOException error) {
            return new Restore(false, "cannot restore active config from retained backup: " + error.getMessage());
        } finally {
            deleteQuietly(stagedRestore);
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    static void atomicReplace(Path source, Path target) throws IOException {
        // Deliberately no non-atomic fallback: on Windows/NTFS this is supported for same-directory
        // files, while silently falling back could delete the active document on a failed move.
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String decodeUtf8Strict(byte[] bytes) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private static Path stage(Path parent, String prefix, byte[] bytes) throws IOException {
        Path temporary = Files.createTempFile(parent, prefix, ".tmp");
        boolean complete = false;
        try (FileChannel channel = FileChannel.open(temporary,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
            complete = true;
            return temporary;
        } finally {
            if (!complete) deleteQuietly(temporary);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A stale uniquely named temp is safer than deleting an unrelated file.
        }
    }

    @FunctionalInterface
    interface CandidateValidator {
        /** Returns an empty string when the parsed candidate compiles successfully. */
        String validate(List<BlockTransformRule> rules);
    }

    @FunctionalInterface
    interface AtomicMover {
        void replace(Path source, Path target) throws IOException;
    }

    record Candidate(Path path, String sha256, List<BlockTransformRule> rules, byte[] bytes, String error) {
        Candidate {
            sha256 = sha256 == null ? "" : sha256;
            rules = rules == null ? List.of() : List.copyOf(rules);
            bytes = bytes == null ? new byte[0] : bytes.clone();
            error = error == null ? "" : error;
        }

        static Candidate failed(Path path, String error) {
            return new Candidate(path, "", List.of(), new byte[0], error);
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        boolean usable() {
            return error.isBlank();
        }
    }

    record Promotion(boolean promoted, String sha256, String error) {
        Promotion {
            sha256 = sha256 == null ? "" : sha256;
            error = error == null ? "" : error;
        }

        static Promotion succeeded(String sha256) {
            return new Promotion(true, sha256, "");
        }

        static Promotion failed(String sha256, String error) {
            return new Promotion(false, sha256, error);
        }
    }

    record Restore(boolean restored, String error) {
        Restore {
            error = error == null ? "" : error;
        }
    }
}
