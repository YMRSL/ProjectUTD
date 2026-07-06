package net.tkg.ModernMayhem.client.compat.oculus;

import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.fml.ModList;

/**
 * Iris 光影兼容 facade。所有方法以 IRIS_LOADED 守卫短路; 真正接触 Iris 类的逻辑全在 OculusCompatImpl,
 * 故 iris 缺席时 OculusCompatImpl 永不被类加载 (避免 NoClassDefFoundError)。
 * 类名沿用 OculusCompat 是历史原因 (1.20.1 走 Oculus); 1.21.1 整合包用的是 iris, 故判 "iris" modid。
 */
public final class OculusCompat {
    private static final boolean IRIS_LOADED = ModList.get().isLoaded("iris");

    private OculusCompat() {
    }

    public static void initCompat() {
        // 1.21.1 直接查询 Iris 运行期状态, 无需预绑定方法引用; 保留空方法供 clientSetup 调用点。
    }

    public static boolean isShaderPackInUse() {
        return IRIS_LOADED && OculusCompatImpl.isShaderPackInUse();
    }

    public static boolean isRenderShadow() {
        return IRIS_LOADED && OculusCompatImpl.isRenderShadow();
    }

    public static boolean isTranslucentHandPass() {
        return IRIS_LOADED && OculusCompatImpl.isTranslucentHandPass();
    }

    public static boolean isSolidHandPass() {
        return IRIS_LOADED && OculusCompatImpl.isSolidHandPass();
    }

    public static boolean endBatch(MultiBufferSource.BufferSource bufferSource) {
        return IRIS_LOADED && OculusCompatImpl.endBatch(bufferSource);
    }

    public static boolean shouldRenderVisor() {
        if (!OculusCompat.isShaderPackInUse()) {
            return true;
        }
        return OculusCompat.isTranslucentHandPass();
    }

    public static boolean shouldRenderNVG() {
        if (!OculusCompat.isShaderPackInUse()) {
            return true;
        }
        return OculusCompat.isSolidHandPass();
    }
}
