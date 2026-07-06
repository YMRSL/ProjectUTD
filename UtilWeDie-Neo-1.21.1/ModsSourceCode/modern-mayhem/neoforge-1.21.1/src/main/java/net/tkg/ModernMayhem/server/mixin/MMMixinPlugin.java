package net.tkg.ModernMayhem.server.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * mm 的 mixin 配置插件。唯一作用: 把接触 SodiumDynamicLights 的 sdl.* mixin 做软门控 ——
 * 仅当整合包里存在 SDDL 时才应用, 否则静默跳过 (mm 其余功能不受影响, IR 锥光静默关闭)。
 * 这样 mm 对 SDDL 只是软依赖, 专用服务器/无 SDDL 环境照常运行。
 */
public class MMMixinPlugin implements IMixinConfigPlugin {
    private static final boolean SDDL_PRESENT = detectSddl();

    private static boolean detectSddl() {
        try {
            return net.neoforged.fml.loading.LoadingModList.get().getModFileById("sodiumdynamiclights") != null;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".sdl.")) {
            return SDDL_PRESENT;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
