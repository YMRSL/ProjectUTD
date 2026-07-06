package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.FireBallProjectileModel;
import com.github.sculkhorde.common.entity.projectile.FireBallProjectileEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;

public class FireBallProjectileRenderer extends GeoEntityRenderer<FireBallProjectileEntity> {
    public FireBallProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new FireBallProjectileModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }
}