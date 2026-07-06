package com.scarasol.zombiekit.client.model;


import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;

/**
 * @author Scarasol
 */
public abstract class AbstractArmorModel<T extends Entity> extends EntityModel<T> {
    public final ModelPart Head;
    public final ModelPart Body;
    public final ModelPart RightArm;
    public final ModelPart LeftArm;
    public final ModelPart RightLeg;
    public final ModelPart LeftLeg;
    public final ModelPart LeftShoes;
    public final ModelPart RightShoes;

    public AbstractArmorModel(ModelPart root) {
        this.Head = root.getChild("Head");
        this.Body = root.getChild("Body");
        this.RightArm = root.getChild("RightArm");
        this.LeftArm = root.getChild("LeftArm");
        this.RightLeg = root.getChild("RightLeg");
        this.LeftLeg = root.getChild("LeftLeg");
        this.LeftShoes = root.getChild("LeftShoes");
        this.RightShoes = root.getChild("RightShoes");
    }
}
