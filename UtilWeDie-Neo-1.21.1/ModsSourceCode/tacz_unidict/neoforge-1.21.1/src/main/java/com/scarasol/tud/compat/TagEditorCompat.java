package com.scarasol.tud.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Set;

/**
 * @author Scarasol
 *
 * NOTE: The "tag-editor" mod is NOT present in this pack, so the original
 * com.scarasol.tageditor.compat.tacz.TaczTagHelper dependency does not resolve.
 * This class has been gutted to a safe no-op stub returning empty sets.
 */
public class TagEditorCompat {

    public static Set<ResourceLocation> getAllTags(ItemStack itemStack) {
        return Collections.emptySet();
    }

    public static Set<ResourceLocation> getAllTags(ResourceLocation gunId) {
        return Collections.emptySet();
    }
}
