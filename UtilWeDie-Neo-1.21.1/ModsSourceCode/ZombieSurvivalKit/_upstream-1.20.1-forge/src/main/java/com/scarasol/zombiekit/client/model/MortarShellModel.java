package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.entity.projectile.MortarShellEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

public class MortarShellModel extends GeoModel<MortarShellEntity> {

	@Override
	public ResourceLocation getAnimationResource(MortarShellEntity entity) {
		return new ResourceLocation("zombiekit", "animations/mortar_shell.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MortarShellEntity entity) {
		return new ResourceLocation("zombiekit", "geo/mortar_shell.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MortarShellEntity entity) {
		return new ResourceLocation("zombiekit", "textures/entities/mortar_shell.png");
	}

	@Override
	public void setCustomAnimations(MortarShellEntity animatable, long instanceId, AnimationState animationState) {
		CoreGeoBone bone = getAnimationProcessor().getBone("shell");
		if (bone != null) {
			bone.setRotX(animatable.getXRot() * Mth.DEG_TO_RAD);
			bone.setRotY(animatable.getYRot() * Mth.DEG_TO_RAD);
		}
	}
}