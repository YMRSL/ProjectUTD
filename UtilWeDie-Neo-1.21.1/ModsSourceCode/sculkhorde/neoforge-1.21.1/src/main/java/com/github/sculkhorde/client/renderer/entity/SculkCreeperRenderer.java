package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkCreeperModel;
import com.github.sculkhorde.common.entity.SculkCreeperEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class SculkCreeperRenderer extends GeoEntityRenderer<SculkCreeperEntity> {


    public SculkCreeperRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkCreeperModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
