package com.utd.inodesfix;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.ClosedFileSystemException;

/**
 * Shared detection + logging for the "closed / disposed {@code ZipFileSystem}" failure
 * that InodesFix exists to tolerate.
 *
 * <p><b>Must live OUTSIDE the mixin package</b> ({@code com.utd.inodesfix}, not
 * {@code com.utd.inodesfix.mixin}). Classes inside a declared mixin package can only be
 * mixins; a plain helper referenced from mixin bodies there triggers
 * {@code IllegalClassLoadError: ... is in a defined mixin package ... and cannot be
 * referenced directly}. Hence this is a public class in the parent package, imported by
 * the mixins.
 *
 * <p>Detection is intentionally narrow so healthy packs are never affected: a throwable is
 * treated as the closed-zip-filesystem condition only when, anywhere in its cause chain,
 * there is either a {@link ClosedFileSystemException} (from {@code ZipFileSystem.ensureOpen})
 * or a {@link NullPointerException} whose message references {@code inodes} — the private
 * {@code LinkedHashMap} that {@code jdk.nio.zipfs.ZipFileSystem} nulls on {@code close()} and
 * then dereferences in {@code getInode}. Every other throwable is reported as "not handled"
 * so the caller rethrows it unchanged.
 */
public final class InodesFixGuard {
    private static final Logger INODESFIX_LOG = LogUtils.getLogger();

    private InodesFixGuard() {
    }

    /**
     * @return {@code true} iff {@code t} (or any cause in its chain) is the closed/disposed
     *         {@code ZipFileSystem} condition this patch is meant to skip past.
     */
    public static boolean isClosedZipFs(Throwable t) {
        // Guard against pathological self-referential cause chains.
        for (Throwable cur = t; cur != null && cur != cur.getCause(); cur = cur.getCause()) {
            if (cur instanceof ClosedFileSystemException) {
                return true;
            }
            if (cur instanceof NullPointerException) {
                String msg = cur.getMessage();
                if (msg != null && msg.contains("inodes")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void warnSkipped(String method, Throwable t) {
        INODESFIX_LOG.warn("[InodesFix] {} skipped for a pack with a closed/disposed ZipFileSystem: {}",
                method, rootString(t));
    }

    private static String rootString(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.toString();
    }
}
