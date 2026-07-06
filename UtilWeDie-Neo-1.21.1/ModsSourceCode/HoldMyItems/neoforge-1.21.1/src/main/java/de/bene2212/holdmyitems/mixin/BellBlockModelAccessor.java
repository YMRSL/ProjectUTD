package de.bene2212.holdmyitems.mixin;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BellRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = {BellRenderer.class})
public interface BellBlockModelAccessor {
    @Accessor(value = "bellBody")
    ModelPart getBellBody();
}
