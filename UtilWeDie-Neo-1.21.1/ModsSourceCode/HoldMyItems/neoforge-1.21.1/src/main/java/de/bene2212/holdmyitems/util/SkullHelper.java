package de.bene2212.holdmyitems.util;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;

public class SkullHelper {
    public static Map<SkullBlock.Type, SkullModelBase> SKULL_MODELS;

    public static void init(EntityModelSet modelSet) {
        SKULL_MODELS = SkullBlockRenderer.createSkullRenderers(modelSet);
    }

    @Nullable
    public static ResolvableProfile getSkullOwner(ItemStack stack) {
        return stack.get(DataComponents.PROFILE);
    }
}
