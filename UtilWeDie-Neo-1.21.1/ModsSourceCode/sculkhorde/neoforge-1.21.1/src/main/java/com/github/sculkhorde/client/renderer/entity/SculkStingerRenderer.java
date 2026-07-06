package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkStingerModel;
import com.github.sculkhorde.common.entity.SculkStingerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class SculkStingerRenderer extends GeoEntityRenderer<SculkStingerEntity> {


    public SculkStingerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkStingerModel());
        //if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
