package com.utd.inodesfix.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import com.utd.inodesfix.InodesFixGuard;

import java.io.InputStream;
import java.util.Set;

/**
 * Guards vanilla {@link PathPackResources} resource-access methods against access
 * to a <em>closed / disposed</em> backing {@code ZipFileSystem} during
 * {@code Minecraft.reloadResourcePacks} in this modpack.
 *
 * <h2>Real root cause (verified from a full crash trace, not the historical guess)</h2>
 * The {@code inodes} field in the {@code NullPointerException
 * "Cannot invoke java.util.LinkedHashMap.get(Object) because this.inodes is null"}
 * is <strong>not</strong> a Minecraft / NeoForge lazy field. It is the private
 * {@code LinkedHashMap inodes} inside the JDK's own
 * {@code jdk.nio.zipfs.ZipFileSystem}. When a zip-backed resource pack's
 * {@code ZipFileSystem} has been closed (which happens here because of the heavy
 * reload churn: ~250 packs, Sinytra Connector, repeated manual reloads), the next
 * access to that filesystem throws one of two equivalent things depending on which
 * internal method is hit first:
 * <ul>
 *   <li>{@link java.nio.file.ClosedFileSystemException} — from
 *       {@code ZipFileSystem.ensureOpen()} (e.g. via {@code Files.exists} in
 *       {@code getRootResource}), or</li>
 *   <li>{@code NullPointerException} on {@code this.inodes} — from
 *       {@code ZipFileSystem.getInode()} (e.g. via {@code Files.newDirectoryStream}
 *       in {@code getNamespaces}), because {@code close()} nulls {@code inodes}
 *       before that code path reaches {@code ensureOpen}.</li>
 * </ul>
 * There is nothing to "re-initialise": the filesystem is closed and owned by
 * Minecraft/Connector, not by us. The only correct action is the same tolerance
 * vanilla already applies to {@code NoSuchFileException}/{@code NotDirectoryException}
 * in these very methods — treat the offending pack as contributing nothing and let
 * the reload finish.
 *
 * <h2>Why the previous version still let reloads abort</h2>
 * It only wrapped {@code getNamespaces}/{@code listResources}. The crash that bounced
 * the player out of the pack screen actually came through {@code getRootResource}
 * (called by {@code AbstractPackResources.getMetadataSection} →
 * {@code MultiPackResourceManager.getPackFilterSection} → {@code MultiPackResourceManager.<init>}).
 * That uncaught {@code ClosedFileSystemException} aborted the whole reload, so
 * Minecraft rolled the pack selection back — which looked like "every pack fails",
 * even healthy ones. This version guards <em>every</em> filesystem-touching entry
 * point of {@code PathPackResources} ({@code getRootResource}, {@code getResource},
 * {@code getNamespaces}, {@code listResources}) so nothing escapes to abort the reload.
 *
 * <h2>Why this does not harm healthy packs</h2>
 * The guard only swallows the two <em>closed-filesystem</em> signatures
 * ({@code ClosedFileSystemException}, and an {@code NPE} whose message names
 * {@code inodes}). Any other exception from a healthy pack is rethrown unchanged, so
 * a live pack's resources are never silently dropped. A healthy, open pack never hits
 * either signature, so its methods run untouched and return real data.
 *
 * <p>Uses MixinExtras {@code @WrapMethod} because the failure originates below these
 * methods (inside the JDK) and must be caught around the whole original body; a HEAD
 * {@code @Inject} could not catch it.
 */
@Mixin(PathPackResources.class)
public class PathPackResourcesInodesFixMixin {
    private static final Logger INODESFIX_LOG = LogUtils.getLogger();

    @WrapMethod(method = "getNamespaces")
    private Set<String> inodesfix$guardGetNamespaces(PackType type, Operation<Set<String>> original) {
        try {
            return original.call(type);
        } catch (Throwable t) {
            if (InodesFixGuard.isClosedZipFs(t)) {
                InodesFixGuard.warnSkipped("getNamespaces", t);
                return Set.of();
            }
            throw t;
        }
    }

    @WrapMethod(method = "listResources")
    private void inodesfix$guardListResources(PackType type, String namespace, String path,
                                              PackResources.ResourceOutput output, Operation<Void> original) {
        try {
            original.call(type, namespace, path, output);
        } catch (Throwable t) {
            if (InodesFixGuard.isClosedZipFs(t)) {
                InodesFixGuard.warnSkipped("listResources", t);
                return;
            }
            throw t;
        }
    }

    @WrapMethod(method = "getRootResource")
    private IoSupplier<InputStream> inodesfix$guardGetRootResource(String[] elements, Operation<IoSupplier<InputStream>> original) {
        try {
            return original.call((Object) elements);
        } catch (Throwable t) {
            if (InodesFixGuard.isClosedZipFs(t)) {
                InodesFixGuard.warnSkipped("getRootResource", t);
                return null;
            }
            throw t;
        }
    }

    @WrapMethod(method = "getResource")
    private IoSupplier<InputStream> inodesfix$guardGetResource(PackType type, ResourceLocation location, Operation<IoSupplier<InputStream>> original) {
        try {
            return original.call(type, location);
        } catch (Throwable t) {
            if (InodesFixGuard.isClosedZipFs(t)) {
                InodesFixGuard.warnSkipped("getResource", t);
                return null;
            }
            throw t;
        }
    }
}
