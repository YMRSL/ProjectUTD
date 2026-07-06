package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkSheepModel;
import com.github.sculkhorde.common.entity.SculkSheepEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class SculkSheepRenderer extends GeoEntityRenderer<SculkSheepEntity> {


    public SculkSheepRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkSheepModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
