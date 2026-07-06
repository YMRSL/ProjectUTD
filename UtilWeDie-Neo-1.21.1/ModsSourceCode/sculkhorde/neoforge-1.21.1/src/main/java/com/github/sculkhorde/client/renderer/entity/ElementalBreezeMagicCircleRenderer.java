package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.ElementalBreezeMagicCircleModel;
import com.github.sculkhorde.common.entity.boss.angel_of_reaping.ElementalBreezeMagicCircleAttackEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import com.github.sculkhorde.client.renderer.layer.ConfigurableGlowLayer;


public class ElementalBreezeMagicCircleRenderer extends GeoEntityRenderer<ElementalBreezeMagicCircleAttackEntity> {

    public ElementalBreezeMagicCircleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ElementalBreezeMagicCircleModel());
        this.addRenderLayer(new ConfigurableGlowLayer<>(this));
    }


}
