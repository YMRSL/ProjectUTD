package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkLeechModel;
import com.github.sculkhorde.common.entity.SculkLeechEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class SculkLeechRenderer extends GeoEntityRenderer<SculkLeechEntity> {


    public SculkLeechRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkLeechModel());
    }

}
