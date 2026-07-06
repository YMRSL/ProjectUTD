package com.scarasol.sona.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * @author Scarasol
 *
 * 1.21.1: VertexConsumer 接口由 1.20.1 的链式 vertex/color/uv/endVertex 改为
 * addVertex/setColor/setUv/setNormal... (无 endVertex)。本类按新接口重写,
 * 在所有写颜色的方法上乘以 alphaMultiplier。
 */
public class AlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alphaMultiplier;

    public AlphaVertexConsumer(VertexConsumer delegate, float alphaMultiplier) {
        this.delegate = delegate;
        this.alphaMultiplier = alphaMultiplier;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(r, g, b, (int) (a * alphaMultiplier));
        return this;
    }

    @Override
    public VertexConsumer setColor(int packedColor) {
        int a = FastColor.ARGB32.alpha(packedColor);
        int r = FastColor.ARGB32.red(packedColor);
        int g = FastColor.ARGB32.green(packedColor);
        int b = FastColor.ARGB32.blue(packedColor);
        return setColor(r, g, b, a);
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        delegate.setColor(r, g, b, Mth.clamp(a * alphaMultiplier, 0.0F, 1.0F));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setOverlay(int packedOverlay) {
        delegate.setOverlay(packedOverlay);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setLight(int packedLight) {
        delegate.setLight(packedLight);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        delegate.setNormal(x, y, z);
        return this;
    }
}
