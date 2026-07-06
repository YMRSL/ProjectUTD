package com.scarasol.zombiekit.client.model;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.projectile.MortarShellEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class MortarShellModel extends GeoModel<MortarShellEntity> {

	@Override
	public ResourceLocation getAnimationResource(MortarShellEntity entity) {
		return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "animations/mortar_shell.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MortarShellEntity entity) {
		return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "geo/mortar_shell.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MortarShellEntity entity) {
		return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "textures/entities/mortar_shell.png");
	}

	@Override
	public void setCustomAnimations(MortarShellEntity animatable, long instanceId, AnimationState<MortarShellEntity> animationState) {
		GeoBone bone = getAnimationProcessor().getBone("shell");
		if (bone != null) {
			bone.setRotX(animatable.getXRot() * Mth.DEG_TO_RAD);
			bone.setRotY(animatable.getYRot() * Mth.DEG_TO_RAD);
		}
	}
}
