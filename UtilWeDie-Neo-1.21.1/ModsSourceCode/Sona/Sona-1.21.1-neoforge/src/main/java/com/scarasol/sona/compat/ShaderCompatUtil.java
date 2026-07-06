package com.scarasol.sona.compat;

import java.lang.reflect.Method;

/**
 * @author Scarasol
 *
 * <p>1.20.1 上游直接 {@code IrisApi.getInstance().isShaderPackInUse()}（编译期依赖 iris/oculus api）。
 * 本工程的 gradle 只把 curios/tacz 作为 compileOnly，iris-neoforge 仅运行时由整合包提供、
 * 不在编译类路径上，故这里改用反射调用 {@code net.irisshaders.iris.api.v0.IrisApi}，
 * 行为与上游一致（着色器包是否启用），iris 缺失/任何异常时一律返回 false。</p>
 *
 * <p>所有调用方（渲染器 / mixin）都先 {@code ModList.get().isLoaded("oculus")} 再调本方法，
 * 所以反射只在 iris(oculus) 实际加载时才会命中。</p>
 */
public final class ShaderCompatUtil {

    private static boolean initialized;
    private static Method getInstanceMethod;
    private static Method isShaderPackInUseMethod;

    private ShaderCompatUtil() {
    }

    public static boolean isShaderActive() {
        if (!initialized) {
            init();
        }
        if (getInstanceMethod == null || isShaderPackInUseMethod == null) {
            return false;
        }
        try {
            Object instance = getInstanceMethod.invoke(null);
            if (instance == null) {
                return false;
            }
            return (boolean) isShaderPackInUseMethod.invoke(instance);
        } catch (Throwable throwable) {
            return false;
        }
    }

    private static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            Class<?> irisApi = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            getInstanceMethod = irisApi.getMethod("getInstance");
            isShaderPackInUseMethod = irisApi.getMethod("isShaderPackInUse");
        } catch (Throwable throwable) {
            getInstanceMethod = null;
            isShaderPackInUseMethod = null;
        }
    }
}
