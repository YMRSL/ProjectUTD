package net.tkg.ModernMayhem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.tkg.ModernMayhem.client.config.ClientConfig;
import net.tkg.ModernMayhem.server.config.ServerConfig;
import net.tkg.ModernMayhem.server.item.curios.facewear.NVGGogglesItem;
import net.tkg.ModernMayhem.server.item.curios.facewear.TVGGogglesItem;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

public class Darkness {
    public static final double MIN = 0.03;
    public static boolean enabled = false;
    public static boolean tvgActive = false;
    private static final float[][] LUMINANCE = new float[16][16];

    public static Vec3 getFogColor(Vec3 vanilla, double factor) {
        if (factor == 1.0) {
            return vanilla;
        }
        return new Vec3(Math.max(0.03, vanilla.x * factor), Math.max(0.03, vanilla.y * factor), Math.max(0.03, vanilla.z * factor));
    }

    private static boolean isDark(Level world) {
        if (Config.getMode() == DarkMode.VANILLA) {
            return false;
        }
        ResourceKey dimType = world.dimension();
        if (dimType == Level.OVERWORLD) {
            return Config.getOnOverworld();
        }
        if (dimType == Level.NETHER) {
            return Config.getOnNether();
        }
        if (dimType == Level.END) {
            return Config.getOnEnd();
        }
        if (world.dimensionType().hasSkyLight()) {
            return Config.getByDefault();
        }
        return Config.getOnNoSkyLight();
    }

    private static float skyFactor(Level world) {
        if (!Darkness.isDark(world)) {
            return 1.0f;
        }
        if (!world.dimensionType().hasSkyLight()) {
            return 0.0f;
        }
        float angle = world.getTimeOfDay(0.0f);
        if (!(angle > 0.25f) || !(angle < 0.75f)) {
            return 1.0f;
        }
        float oldWeight = Math.max(0.0f, Math.abs(angle - 0.5f) - 0.2f) * 20.0f;
        float moon = Config.getAffectedByMoonPhase() ? world.getMoonBrightness() : 0.0f;
        float moonInterpolated = (float)Mth.lerp((double)moon, (double)Config.getNewMoonBright(), (double)Config.getFullMoonBright());
        return Mth.lerp((float)(oldWeight * oldWeight * oldWeight), (float)moonInterpolated, (float)1.0f);
    }

    public static int darken(int c, int blockIndex, int skyIndex) {
        float lTarget = LUMINANCE[blockIndex][skyIndex];
        float r = (float)(c & 0xFF) / 255.0f;
        float g = (float)(c >> 8 & 0xFF) / 255.0f;
        float b = (float)(c >> 16 & 0xFF) / 255.0f;
        float l = Darkness.luminance(r, g, b);
        float f = l > 0.0f ? Math.min(1.0f, lTarget / l) : 0.0f;
        return f == 1.0f ? c : 0xFF000000 | Math.round(f * r * 255.0f) | Math.round(f * g * 255.0f) << 8 | Math.round(f * b * 255.0f) << 16;
    }

    public static float luminance(float r, float g, float b) {
        return r * 0.2126f + g * 0.7152f + b * 0.0722f;
    }

    public static void updateLuminance(float tickDelta, Minecraft client, GameRenderer gameRenderer, float prevFlicker) {
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return;
        }
        boolean nvgActive = false;
        tvgActive = false;
        ItemStack facewearItem = CuriosUtil.getFaceWearItem((Player)client.player);
        if (facewearItem != null) {
            boolean isPowered = GenericSpecialGogglesItem.getNVGCheck(facewearItem);
            if (facewearItem.getItem() instanceof NVGGogglesItem && isPowered) {
                nvgActive = true;
            } else if (facewearItem.getItem() instanceof TVGGogglesItem && isPowered) {
                tvgActive = true;
            }
        }
        if (nvgActive && client.options.getCameraType().isFirstPerson()) {
            enabled = false;
            return;
        }
        if (tvgActive && client.options.getCameraType().isFirstPerson()) {
            enabled = true;
            return;
        }
        boolean isDarkOnLevel = Darkness.isDark((Level)level);
        float ambient = level.getSkyDarken(tickDelta);
        boolean hasVanillaNV = client.player.hasEffect(MobEffects.NIGHT_VISION) || client.player.hasEffect(MobEffects.CONDUIT_POWER) && client.player.getWaterVision() > 0.0f;
        boolean bl = enabled = !(!isDarkOnLevel || hasVanillaNV || level.getSkyFlashTime() > 0 || level.effects().forceBrightLightmap() || !Config.getOnFullBrightBiomes() && ambient >= 0.99f && level.getBrightness(LightLayer.SKY, client.player.blockPosition()) < 8);
        if (!enabled) {
            return;
        }
        DimensionType dim = level.dimensionType();
        float dimSkyFactor = Darkness.skyFactor((Level)level);
        for (int skyIndex = 0; skyIndex < 16; ++skyIndex) {
            float v;
            float skyFactor = 1.0f - (float)skyIndex / 15.0f;
            skyFactor = 1.0f - skyFactor * skyFactor * skyFactor * skyFactor;
            skyFactor *= dimSkyFactor;
            float value = Config.getMode().value;
            if (value == -1.0f) {
                throw new IllegalStateException("Darkness value can't be negative");
            }
            float min = Math.max(skyFactor * 0.05f, value);
            float rawAmbient = ambient * skyFactor;
            float minAmbient = rawAmbient * (1.0f - min) + min;
            float skyBase = LightTexture.getBrightness((DimensionType)dim, (int)skyIndex) * minAmbient;
            min = Math.max(0.35f * skyFactor, value);
            float skyRed = v = skyBase * (rawAmbient * (1.0f - min) + min);
            float skyGreen = v;
            float skyBlue = skyBase;
            if (gameRenderer.getDarkenWorldAmount(tickDelta) > 0.0f) {
                float skyDarkness = gameRenderer.getDarkenWorldAmount(tickDelta);
                skyRed = skyRed * (1.0f - skyDarkness) + skyRed * 0.7f * skyDarkness;
                skyGreen = skyGreen * (1.0f - skyDarkness) + skyGreen * 0.6f * skyDarkness;
                skyBlue = skyBlue * (1.0f - skyDarkness) + skyBlue * 0.6f * skyDarkness;
            }
            for (int blockIndex = 0; blockIndex < 16; ++blockIndex) {
                float blockFactor = 1.0f - (float)blockIndex / 15.0f;
                blockFactor = 1.0f - blockFactor * blockFactor * blockFactor * blockFactor;
                float blockBase = blockFactor * LightTexture.getBrightness((DimensionType)dim, (int)blockIndex) * (prevFlicker * 0.1f + 1.5f);
                min = 0.4f * blockFactor;
                float blockGreen = blockBase * ((blockBase * (1.0f - min) + min) * (1.0f - min) + min);
                float blockBlue = blockBase * (blockBase * blockBase * (1.0f - min) + min);
                float red = skyRed + blockBase;
                float green = skyGreen + blockGreen;
                float blue = skyBlue + blockBlue;
                float f = Math.max(skyFactor, blockFactor);
                min = 0.03f * f;
                red = red * (0.99f - min) + min;
                green = green * (0.99f - min) + min;
                blue = blue * (0.99f - min) + min;
                if (level.dimension() == Level.END) {
                    red = skyFactor * 0.22f + blockBase * 0.75f;
                    green = skyFactor * 0.28f + blockGreen * 0.75f;
                    blue = skyFactor * 0.25f + blockBlue * 0.75f;
                }
                if (red > 1.0f) {
                    red = 1.0f;
                }
                if (green > 1.0f) {
                    green = 1.0f;
                }
                if (blue > 1.0f) {
                    blue = 1.0f;
                }
                float gamma = ((Double)client.options.gamma().get()).floatValue() * f;
                float invRed = 1.0f - red;
                float invGreen = 1.0f - green;
                float invBlue = 1.0f - blue;
                invRed = 1.0f - invRed * invRed * invRed * invRed;
                invGreen = 1.0f - invGreen * invGreen * invGreen * invGreen;
                invBlue = 1.0f - invBlue * invBlue * invBlue * invBlue;
                red = red * (1.0f - gamma) + invRed * gamma;
                green = green * (1.0f - gamma) + invGreen * gamma;
                blue = blue * (1.0f - gamma) + invBlue * gamma;
                min = Math.max(0.03f * f, Config.getMode().value);
                red = red * (0.99f - min) + min;
                green = green * (0.99f - min) + min;
                blue = blue * (0.99f - min) + min;
                red = Mth.clamp((float)red, (float)0.0f, (float)1.0f);
                green = Mth.clamp((float)green, (float)0.0f, (float)1.0f);
                blue = Mth.clamp((float)blue, (float)0.0f, (float)1.0f);
                Darkness.LUMINANCE[blockIndex][skyIndex] = Darkness.luminance(red, green, blue);
            }
        }
    }

    public static class Config {
        private static boolean useServer() {
            try {
                return (Boolean)ServerConfig.OVERRIDE_CLIENT_CONFIG_OPTIONS.get();
            }
            catch (Exception e) {
                return false;
            }
        }

        public static DarkMode getMode() {
            return Config.useServer() ? (DarkMode)((Object)ServerConfig.DARKNESS_MODE.get()) : (DarkMode)((Object)ClientConfig.DARKNESS_MODE.get());
        }

        public static boolean getOnOverworld() {
            return Config.useServer() ? (Boolean)ServerConfig.DARKNESS_ON_OVERWORLD.get() : (Boolean)ClientConfig.DARKNESS_ON_OVERWORLD.get();
        }

        public static boolean getOnNether() {
            return Config.useServer() ? (Boolean)ServerConfig.DARKNESS_ON_NETHER.get() : (Boolean)ClientConfig.DARKNESS_ON_NETHER.get();
        }

        public static double getNetherFogBright() {
            return Config.useServer() ? (Double)ServerConfig.DARKNESS_NETHER_FOG_BRIGHT.get() : (Double)ClientConfig.DARKNESS_NETHER_FOG_BRIGHT.get();
        }

        public static boolean getOnEnd() {
            return Config.useServer() ? (Boolean)ServerConfig.DARKNESS_ON_END.get() : (Boolean)ClientConfig.DARKNESS_ON_END.get();
        }

        public static double getEndFogBright() {
            return Config.useServer() ? (Double)ServerConfig.DARKNESS_END_FOG_BRIGHT.get() : (Double)ClientConfig.DARKNESS_END_FOG_BRIGHT.get();
        }

        public static boolean getByDefault() {
            return Config.useServer() ? (Boolean)ServerConfig.DARKNESS_BY_DEFAULT.get() : (Boolean)ClientConfig.DARKNESS_BY_DEFAULT.get();
        }

        public static boolean getOnNoSkyLight() {
            return Config.useServer() ? (Boolean)ServerConfig.DARKNESS_ON_NO_SKY_LIGHT.get() : (Boolean)ClientConfig.DARKNESS_ON_NO_SKY_LIGHT.get();
        }

        public static boolean getAffectedByMoonPhase() {
            return Config.useServer() ? (Boolean)ServerConfig.DARKNESS_AFFECTED_BY_MOON_PHASE.get() : (Boolean)ClientConfig.DARKNESS_AFFECTED_BY_MOON_PHASE.get();
        }

        public static double getNewMoonBright() {
            return Config.useServer() ? (Double)ServerConfig.DARKNESS_NEW_MOON_BRIGHT.get() : (Double)ClientConfig.DARKNESS_NEW_MOON_BRIGHT.get();
        }

        public static double getFullMoonBright() {
            return Config.useServer() ? (Double)ServerConfig.DARKNESS_FULL_MOON_BRIGHT.get() : (Double)ClientConfig.DARKNESS_FULL_MOON_BRIGHT.get();
        }

        public static boolean getOnFullBrightBiomes() {
            return Config.useServer() ? (Boolean)ServerConfig.DARKNESS_ON_FULL_BRIGHT_BIOMES.get() : (Boolean)ClientConfig.DARKNESS_ON_FULL_BRIGHT_BIOMES.get();
        }
    }

    public static enum DarkMode {
        VANILLA(-1.0f),
        DIM(0.18f),
        DARK(0.12f),
        DARKNESS(0.08f),
        BLACK(0.04f),
        BLACKNESS(0.0f);

        public final float value;

        private DarkMode(float value) {
            this.value = value;
        }
    }

    public static interface DynamicTextureHook {
        public void darkness$enableDarkness();
    }
}

