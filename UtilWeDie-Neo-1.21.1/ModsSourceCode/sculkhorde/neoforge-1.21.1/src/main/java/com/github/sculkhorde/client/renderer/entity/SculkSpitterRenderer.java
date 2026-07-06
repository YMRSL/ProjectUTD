package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkSpitterModel;
import com.github.sculkhorde.common.entity.SculkSpitterEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;

public class SculkSpitterRenderer extends GeoEntityRenderer<SculkSpitterEntity> {

    public SculkSpitterRenderer(EntityRendererProvider.Context renderManager)
    {
        super(renderManager, new SculkSpitterModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
