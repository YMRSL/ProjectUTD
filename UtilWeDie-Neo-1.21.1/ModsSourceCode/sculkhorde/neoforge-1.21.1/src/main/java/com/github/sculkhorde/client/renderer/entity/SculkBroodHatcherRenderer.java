package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkBroodHatcherModel;
import com.github.sculkhorde.common.entity.SculkBroodHatcherEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class SculkBroodHatcherRenderer extends GeoEntityRenderer<SculkBroodHatcherEntity> {


    public SculkBroodHatcherRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkBroodHatcherModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
