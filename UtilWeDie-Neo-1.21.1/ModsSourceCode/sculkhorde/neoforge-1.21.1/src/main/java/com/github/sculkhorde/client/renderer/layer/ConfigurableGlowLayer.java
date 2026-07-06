package com.github.sculkhorde.client.renderer.layer;

import com.github.sculkhorde.core.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * 发光层, 在「渲染时」尊重 SERVER 配置 {@code enable_gpu_compatibility_mode}。
 *
 * 原代码在每个 GeoEntityRenderer 的构造器里读 {@code ModConfig.SERVER.enable_gpu_compatibility_mode.get()}
 * 来决定是否添加 {@link AutoGlowingGeoLayer}。但在 NeoForge 1.21, 渲染器是在(很早的)资源重载阶段构造的,
 * 此时 SERVER 配置尚未加载 → 抛 "Cannot get config value before config is loaded" → 模型烘焙失败 →
 * 整个 ModLoader 进入损坏态 → 客户端渲染管线(Veil 着色器/纹理图集/资源重载收尾)被跳过 → 画面冻结/全黑。
 *
 * 解决: 构造器恒添加本层, 把配置读取推迟到 render() (届时配置已加载), 既不崩又仍尊重该开关。
 * 配置未加载时(理论上实体不会在此阶段渲染)默认显示发光层。
 */
public class ConfigurableGlowLayer<T extends GeoAnimatable> extends AutoGlowingGeoLayer<T> {

    public ConfigurableGlowLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, T animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        if (ModConfig.SERVER_SPEC.isLoaded() && ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {
            return; // GPU 兼容模式: 跳过发光层
        }
        super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick, packedLight, packedOverlay);
    }
}
