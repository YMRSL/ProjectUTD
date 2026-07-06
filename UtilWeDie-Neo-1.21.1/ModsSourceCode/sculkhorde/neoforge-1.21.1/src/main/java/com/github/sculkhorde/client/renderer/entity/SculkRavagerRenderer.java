package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkRavagerModel;
import com.github.sculkhorde.common.entity.SculkRavagerEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;

public class SculkRavagerRenderer extends GeoEntityRenderer<SculkRavagerEntity>
{
    public SculkRavagerRenderer(EntityRendererProvider.Context renderManager)
    {
        super(renderManager, new SculkRavagerModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }
}
