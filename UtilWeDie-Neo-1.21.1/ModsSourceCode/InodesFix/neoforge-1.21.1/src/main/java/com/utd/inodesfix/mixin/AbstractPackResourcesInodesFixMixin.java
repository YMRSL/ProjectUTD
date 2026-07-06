package com.utd.inodesfix.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.spongepowered.asm.mixin.Mixin;
import com.utd.inodesfix.InodesFixGuard;

import java.io.IOException;

/**
 * Defense-in-depth guard for {@code AbstractPackResources#getMetadataSection} (reads
 * {@code pack.mcmeta}). The same closed/disposed {@code ZipFileSystem} condition described
 * in {@link PathPackResourcesInodesFixMixin} can surface here, because
 * {@code MultiPackResourceManager.<init>} calls {@code getPackFilterSection} →
 * {@code getMetadataSection} → {@code getRootResource} very early in a reload. For
 * {@code PathPackResources} the inner {@code getRootResource} is already guarded; this wrap
 * additionally protects any other {@code AbstractPackResources} subclass whose
 * {@code getRootResource}/stream read hits a closed zip filesystem, so a stale pack is
 * skipped (treated as having no metadata) instead of aborting the whole reload.
 *
 * <p>Matching is the same narrow closed-zip-filesystem test used everywhere in this patch,
 * so healthy packs are never affected; any other exception is rethrown unchanged.
 */
@Mixin(AbstractPackResources.class)
public class AbstractPackResourcesInodesFixMixin {

    @WrapMethod(method = "getMetadataSection")
    private <T> T inodesfix$guardMetadata(MetadataSectionSerializer<T> serializer, Operation<T> original) throws IOException {
        try {
            return original.call(serializer);
        } catch (Throwable t) {
            if (InodesFixGuard.isClosedZipFs(t)) {
                InodesFixGuard.warnSkipped("getMetadataSection", t);
                return null;
            }
            throw t;
        }
    }
}
