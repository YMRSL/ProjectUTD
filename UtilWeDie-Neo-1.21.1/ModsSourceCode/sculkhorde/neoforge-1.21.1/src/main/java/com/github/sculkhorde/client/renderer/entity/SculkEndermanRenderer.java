package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkEndermanModel;
import com.github.sculkhorde.common.entity.boss.sculk_enderman.SculkEndermanEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class SculkEndermanRenderer extends GeoEntityRenderer<SculkEndermanEntity> {


    public SculkEndermanRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkEndermanModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
