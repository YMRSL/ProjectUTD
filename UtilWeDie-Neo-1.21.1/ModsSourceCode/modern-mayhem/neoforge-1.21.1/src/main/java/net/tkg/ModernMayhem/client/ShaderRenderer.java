package net.tkg.ModernMayhem.client;

import com.mojang.blaze3d.shaders.Uniform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.server.mixinaccessor.PostChainAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.util.tuples.Pair;

public class ShaderRenderer {
    private final ResourceLocation shaderLocation;
    private PostChain postChain = null;
    private boolean isActive = false;
    private int lastFrameScreenWidth = -1;
    private int lastFrameScreenHeight = -1;
    private final Map<String, Pair<String, Object>> modifiedUniforms = new HashMap<String, Pair<String, Object>>();
    private static final Minecraft mc = Minecraft.getInstance();

    public ShaderRenderer(@NotNull ResourceLocation shaderLocation) {
        this.shaderLocation = shaderLocation;
    }

    public ResourceLocation getShaderLocation() {
        return this.shaderLocation;
    }

    public boolean isActive() {
        return this.isActive;
    }

    public String getShaderName() {
        return this.shaderLocation.getNamespace() + ":" + this.shaderLocation.getPath().substring(this.shaderLocation.getPath().lastIndexOf(47) + 1, this.shaderLocation.getPath().lastIndexOf(46));
    }

    public String getFullShaderName() {
        return this.shaderLocation.getNamespace() + ":" + this.shaderLocation.getPath();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void toggleActive() {
        this.isActive = !this.isActive;
    }

    public void render() {
        GameRenderer gameRenderer = ShaderRenderer.mc.gameRenderer;
        if (!this.isActive && this.isCurrentEffect()) {
            gameRenderer.shutdownEffect();
            return;
        }
        if (!this.isActive || this.isCurrentEffect()) {
            return;
        }
        if (gameRenderer.currentEffect() != null && this.isCurrentEffect() && !this.hasWindowSizeChanged()) {
            return;
        }
        gameRenderer.loadEffect(this.shaderLocation);
        if (this.hasWindowSizeChanged()) {
            this.reapplyModifiedUniforms();
        }
    }

    public Uniform getUniform(@Nullable String passName, String uniformName) {
        if (!this.isCurrentEffect() || ShaderRenderer.mc.gameRenderer.currentEffect() == null) {
            return null;
        }
        List<PostPass> passes = ((PostChainAccess)Objects.requireNonNull(ShaderRenderer.mc.gameRenderer.currentEffect())).test_master$getPasses();
        for (PostPass pass : passes) {
            Uniform uniform;
            if (passName != null && !pass.getName().equals(passName) || (uniform = pass.getEffect().getUniform(uniformName)) == null) continue;
            return uniform;
        }
        return null;
    }

    public Uniform getUniform(String uniformName) {
        return this.getUniform(null, uniformName);
    }

    public void setSampler2dUniform(String samplerName, ResourceLocation textureLocation) {
        if (!this.isCurrentEffect() || ShaderRenderer.mc.gameRenderer.currentEffect() == null) {
            return;
        }
        AbstractTexture texture = mc.getTextureManager().getTexture(textureLocation);
        int textureId = texture.getId();
        List<PostPass> passes = ((PostChainAccess)Objects.requireNonNull(ShaderRenderer.mc.gameRenderer.currentEffect())).test_master$getPasses();
        for (PostPass pass : passes) {
            if (!pass.getName().equals(this.getShaderName())) continue;
            pass.getEffect().setSampler(samplerName, () -> textureId);
        }
    }

    public void setFloatUniform(String passName, String uniformName, float value) {
        Uniform uniform = this.getUniform(passName, uniformName);
        if (uniform != null) {
            uniform.set(value);
            this.modifiedUniforms.put("float:" + passName + ":" + uniformName, (Pair<String, Object>)new Pair((Object)(passName + ":" + uniformName), (Object)Float.valueOf(value)));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in pass {}", (Object)uniformName, (Object)passName);
        }
    }

    public void setFloatUniform(String uniformName, float value) {
        Uniform uniform = this.getUniform(null, uniformName);
        if (uniform != null) {
            uniform.set(value);
            this.modifiedUniforms.put("float:" + uniformName, (Pair<String, Object>)new Pair((Object)uniformName, (Object)Float.valueOf(value)));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in shader {}", (Object)uniformName, (Object)this.getShaderName());
        }
    }

    public void setIntUniform(String passName, String uniformName, int value) {
        Uniform uniform = this.getUniform(passName, uniformName);
        if (uniform != null) {
            uniform.set(value);
            this.modifiedUniforms.put("int:" + passName + ":" + uniformName, (Pair<String, Object>)new Pair((Object)(passName + ":" + uniformName), (Object)value));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in pass {}", (Object)uniformName, (Object)passName);
        }
    }

    public void setIntUniform(String uniformName, int value) {
        Uniform uniform = this.getUniform(null, uniformName);
        if (uniform != null) {
            uniform.set(value);
            this.modifiedUniforms.put("int:" + uniformName, (Pair<String, Object>)new Pair((Object)uniformName, (Object)value));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in shader {}", (Object)uniformName, (Object)this.getShaderName());
        }
    }

    public void setBooleanUniform(String passName, String uniformName, boolean value) {
        Uniform uniform = this.getUniform(passName, uniformName);
        if (uniform != null) {
            uniform.set(value ? 1 : 0);
            this.modifiedUniforms.put("bool:" + passName + ":" + uniformName, (Pair<String, Object>)new Pair((Object)(passName + ":" + uniformName), (Object)(value ? 1 : 0)));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in pass {}", (Object)uniformName, (Object)passName);
        }
    }

    public void setBooleanUniform(String uniformName, boolean value) {
        Uniform uniform = this.getUniform(null, uniformName);
        if (uniform != null) {
            uniform.set(value ? 1 : 0);
            this.modifiedUniforms.put("bool:" + uniformName, (Pair<String, Object>)new Pair((Object)uniformName, (Object)(value ? 1 : 0)));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in shader {}", (Object)uniformName, (Object)this.getShaderName());
        }
    }

    public void setVec2Uniform(String passName, String uniformName, float x, float y) {
        Uniform uniform = this.getUniform(passName, uniformName);
        if (uniform != null) {
            uniform.set(x, y);
            this.modifiedUniforms.put("vec2:" + passName + ":" + uniformName, (Pair<String, Object>)new Pair((Object)(passName + ":" + uniformName), (Object)new float[]{x, y}));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in pass {}", (Object)uniformName, (Object)passName);
        }
    }

    public void setVec2Uniform(String uniformName, float x, float y) {
        Uniform uniform = this.getUniform(null, uniformName);
        if (uniform != null) {
            uniform.set(x, y);
            this.modifiedUniforms.put("vec2:" + uniformName, (Pair<String, Object>)new Pair((Object)uniformName, (Object)new float[]{x, y}));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in shader {}", (Object)uniformName, (Object)this.getShaderName());
        }
    }

    public void setVec3Uniform(String passName, String uniformName, float x, float y, float z) {
        Uniform uniform = this.getUniform(passName, uniformName);
        if (uniform != null) {
            uniform.set(x, y, z);
            this.modifiedUniforms.put("vec3:" + passName + ":" + uniformName, (Pair<String, Object>)new Pair((Object)(passName + ":" + uniformName), (Object)new float[]{x, y, z}));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in pass {}", (Object)uniformName, (Object)passName);
        }
    }

    public void setVec3Uniform(String uniformName, float x, float y, float z) {
        Uniform uniform = this.getUniform(null, uniformName);
        if (uniform != null) {
            uniform.set(x, y, z);
            this.modifiedUniforms.put("vec3:" + uniformName, (Pair<String, Object>)new Pair((Object)uniformName, (Object)new float[]{x, y, z}));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in shader {}", (Object)uniformName, (Object)this.getShaderName());
        }
    }

    public void setVec4Uniform(String passName, String uniformName, float x, float y, float z, float w) {
        Uniform uniform = this.getUniform(passName, uniformName);
        if (uniform != null) {
            uniform.set(x, y, z, w);
            this.modifiedUniforms.put("vec4:" + passName + ":" + uniformName, (Pair<String, Object>)new Pair((Object)(passName + ":" + uniformName), (Object)new float[]{x, y, z, w}));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in pass {}", (Object)uniformName, (Object)passName);
        }
    }

    public void setVec4Uniform(String uniformName, float x, float y, float z, float w) {
        Uniform uniform = this.getUniform(null, uniformName);
        if (uniform != null) {
            uniform.set(x, y, z, w);
            this.modifiedUniforms.put("vec4:" + uniformName, (Pair<String, Object>)new Pair((Object)uniformName, (Object)new float[]{x, y, z, w}));
        } else {
            ModernMayhemMod.LOGGER.warn("Uniform {} not found in shader {}", (Object)uniformName, (Object)this.getShaderName());
        }
    }

    public String toString() {
        return "ShaderRenderer{shaderLocation=" + this.shaderLocation + ", isActive=" + this.isActive + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShaderRenderer)) {
            return false;
        }
        ShaderRenderer that = (ShaderRenderer)o;
        return this.shaderLocation.equals((Object)that.shaderLocation);
    }

    private void reapplyModifiedUniforms() {
        for (Map.Entry<String, Pair<String, Object>> entry : this.modifiedUniforms.entrySet()) {
            String uniformName;
            String[] keyParts = entry.getKey().split(":", 3);
            String type = keyParts[0];
            String passAndUniform = (String)entry.getValue().getA();
            String passName = null;
            if (passAndUniform.contains(":")) {
                String[] parts = passAndUniform.split(":", 2);
                passName = parts[0];
                uniformName = parts[1];
            } else {
                uniformName = passAndUniform;
            }
            switch (type) {
                case "float": {
                    float value = ((Float)entry.getValue().getB()).floatValue();
                    if (passName != null) {
                        this.setFloatUniform(passName, uniformName, value);
                        break;
                    }
                    this.setFloatUniform(uniformName, value);
                    break;
                }
                case "int": {
                    int value = (Integer)entry.getValue().getB();
                    if (passName != null) {
                        this.setIntUniform(passName, uniformName, value);
                        break;
                    }
                    this.setIntUniform(uniformName, value);
                    break;
                }
                case "bool": {
                    boolean value;
                    boolean bl = value = (Integer)entry.getValue().getB() == 1;
                    if (passName != null) {
                        this.setBooleanUniform(passName, uniformName, value);
                        break;
                    }
                    this.setBooleanUniform(uniformName, value);
                    break;
                }
                case "vec2": {
                    float[] vec2Values = (float[])entry.getValue().getB();
                    if (passName != null) {
                        this.setVec2Uniform(passName, uniformName, vec2Values[0], vec2Values[1]);
                        break;
                    }
                    this.setVec2Uniform(uniformName, vec2Values[0], vec2Values[1]);
                    break;
                }
                case "vec3": {
                    float[] vec3Values = (float[])entry.getValue().getB();
                    if (passName != null) {
                        this.setVec3Uniform(passName, uniformName, vec3Values[0], vec3Values[1], vec3Values[2]);
                        break;
                    }
                    this.setVec3Uniform(uniformName, vec3Values[0], vec3Values[1], vec3Values[2]);
                    break;
                }
                case "vec4": {
                    float[] vec4Values = (float[])entry.getValue().getB();
                    if (passName != null) {
                        this.setVec4Uniform(passName, uniformName, vec4Values[0], vec4Values[1], vec4Values[2], vec4Values[3]);
                        break;
                    }
                    this.setVec4Uniform(uniformName, vec4Values[0], vec4Values[1], vec4Values[2], vec4Values[3]);
                    break;
                }
                default: {
                    ModernMayhemMod.LOGGER.warn("Unknown uniform type: {}", (Object)type);
                }
            }
        }
    }

    public void reset() {
        this.isActive = false;
        this.lastFrameScreenWidth = -1;
        this.lastFrameScreenHeight = -1;
        this.modifiedUniforms.clear();
    }

    private boolean hasWindowSizeChanged() {
        int currentScreenWidth = mc.getWindow().getGuiScaledWidth();
        int currentScreenHeight = mc.getWindow().getGuiScaledHeight();
        return this.lastFrameScreenWidth != currentScreenWidth || this.lastFrameScreenHeight != currentScreenHeight;
    }

    private boolean isCurrentEffect() {
        GameRenderer gameRenderer = ShaderRenderer.mc.gameRenderer;
        PostChain currentEffect = gameRenderer.currentEffect();
        return currentEffect != null && Objects.equals(currentEffect.getName(), this.getFullShaderName());
    }
}

