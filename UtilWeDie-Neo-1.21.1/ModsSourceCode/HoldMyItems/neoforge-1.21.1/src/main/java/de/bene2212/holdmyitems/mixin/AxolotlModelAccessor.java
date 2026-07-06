package de.bene2212.holdmyitems.mixin;

import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = {AxolotlModel.class})
public interface AxolotlModelAccessor {
    @Accessor(value = "head")
    ModelPart getHead();

    @Accessor(value = "leftFrontLeg")
    ModelPart getLeftLeg();

    @Accessor(value = "rightFrontLeg")
    ModelPart getRightLeg();

    @Accessor(value = "tail")
    ModelPart getTail();

    @Accessor(value = "leftHindLeg")
    ModelPart getLeftLegB();

    @Accessor(value = "rightHindLeg")
    ModelPart getRightLegB();
}
