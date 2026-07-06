package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkSporeSpewerModel;
import com.github.sculkhorde.common.entity.SculkSporeSpewerEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;

public class SculkSporeSpewerRenderer extends GeoEntityRenderer<SculkSporeSpewerEntity> {

    public SculkSporeSpewerRenderer(EntityRendererProvider.Context renderManager)
    {
        super(renderManager, new SculkSporeSpewerModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
