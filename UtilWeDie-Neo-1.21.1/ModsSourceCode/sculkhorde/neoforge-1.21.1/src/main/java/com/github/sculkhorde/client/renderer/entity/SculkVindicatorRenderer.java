package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkVindicatorModel;
import com.github.sculkhorde.common.entity.SculkVindicatorEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class SculkVindicatorRenderer extends GeoEntityRenderer<SculkVindicatorEntity> {


    public SculkVindicatorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkVindicatorModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
