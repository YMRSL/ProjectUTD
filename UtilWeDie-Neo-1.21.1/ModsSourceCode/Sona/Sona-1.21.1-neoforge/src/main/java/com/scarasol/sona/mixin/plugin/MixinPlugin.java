package com.scarasol.sona.mixin.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * NeoForge's {@link IMixinConfigPlugin} contract is unchanged from Forge.
 *
 * Only the external-mod branches for the mixins we KEEP remain:
 * <ul>
 *   <li>{@code tacz.*}  -> gated on TaCZ being present.</li>
 *   <li>{@code sbw.*}   -> gated on SuperbWarfare being present.</li>
 *   <li>{@code embeddium.*} (the Sodium infection-fog shader mixin) -> gated on the 1.21.1 Sodium
 *       {@code ShaderLoader} (class moved to {@code net.caffeinemc.mods.sodium...}).</li>
 * </ul>
 * Dropped branches: {@code travelersbackpack.*} (mod not in the 1.21.1 pack) and {@code geckolib.*}
 * (geckolib render mixins were dropped from this migration).
 */
public class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("tacz")) return classExists("com.tacz.guns.GunMod");
        if (mixinClassName.contains("sbw")) return classExists("com.atsuishio.superbwarfare.Mod");
        if (mixinClassName.contains("embeddium")) return classExists("net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader");
        return true;
    }

    /**
     * 仅检查类文件是否存在于 classpath，<b>不加载/不链接该类</b>。
     * 关键：旧实现用 {@code Class.forName} 会真正加载目标类，导致 voxy 等同样 mixin
     * {@code net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader} 的 mod 因
     * "target was loaded too early" 在 PREPARE 阶段崩溃。改为读 .class 资源即可避免提前加载。
     */
    private boolean classExists(String className) {
        return this.getClass().getClassLoader().getResource(className.replace('.', '/') + ".class") != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
