package com.scarasol.sona.init;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;

/**
 * @author Scarasol
 */
@EventBusSubscriber(modid = "sona", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SonaShaders {

    // 保存我们的核心着色器实例
    public static ShaderInstance entityDitherShader;
    public static ShaderInstance infectionShaderSkyPostShader;

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("sona", "rendertype_entity_dither"), DefaultVertexFormat.NEW_ENTITY),
                shaderInstance -> entityDitherShader = shaderInstance
        );
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("sona", "infection_shader_sky_post"), DefaultVertexFormat.POSITION_TEX_COLOR),
                shaderInstance -> infectionShaderSkyPostShader = shaderInstance
        );
    }
}
