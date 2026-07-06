package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkGuardianModel;
import com.github.sculkhorde.common.entity.SculkGuardianEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class SculkGuardianRenderer extends GeoEntityRenderer<SculkGuardianEntity> {


    public SculkGuardianRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkGuardianModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }

}
