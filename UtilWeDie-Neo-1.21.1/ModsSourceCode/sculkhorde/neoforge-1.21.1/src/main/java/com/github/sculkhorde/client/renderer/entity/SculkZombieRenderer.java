package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkZombieModel;
import com.github.sculkhorde.common.entity.SculkZombieEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class SculkZombieRenderer extends GeoEntityRenderer<SculkZombieEntity> {


    public SculkZombieRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkZombieModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
