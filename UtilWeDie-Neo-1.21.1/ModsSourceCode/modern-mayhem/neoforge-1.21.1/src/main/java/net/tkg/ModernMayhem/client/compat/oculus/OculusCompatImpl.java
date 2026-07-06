package net.tkg.ModernMayhem.client.compat.oculus;

import net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pathways.HandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * 真实的 Iris 兼容实现 —— 唯一直接接触 Iris 类的地方。
 * 只在 iris 实际加载时由 OculusCompat facade 的 IRIS_LOADED 守卫短路后调用,
 * 故 iris 缺席时本类永不被类加载 (避免 NoClassDefFoundError)。
 *
 * 1.21.1 相对 1.20.1 的变化:
 *  - isRenderShadow: 旧 ShadowRenderingState.areShadowsCurrentlyBeingRendered() → 稳定 api.v0 IrisApi.isRenderingShadowPass()
 *  - solid/translucent hand pass: 旧版靠外部 mixin 写 isTranslucentHandPass/isSolidHandPass 字段;
 *    1.21.1 直接查询 HandRenderer.INSTANCE 状态, 无需 mixin。
 */
public final class OculusCompatImpl {
    private OculusCompatImpl() {
    }

    static boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    static boolean isRenderShadow() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    static boolean isSolidHandPass() {
        return HandRenderer.INSTANCE.isActive() && HandRenderer.INSTANCE.isRenderingSolid();
    }

    static boolean isTranslucentHandPass() {
        return HandRenderer.INSTANCE.isActive() && !HandRenderer.INSTANCE.isRenderingSolid();
    }

    static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        if (bufferSource instanceof FullyBufferedMultiBufferSource) {
            ((FullyBufferedMultiBufferSource) bufferSource).endBatch();
            return true;
        }
        return false;
    }
}
