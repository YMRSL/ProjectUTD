package net.tkg.ModernMayhem.client.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.tkg.ModernMayhem.client.Darkness;

public class ClientConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec CONFIG;
    public static final ModConfigSpec.EnumValue<Darkness.DarkMode> DARKNESS_MODE;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_ON_OVERWORLD;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_ON_NETHER;
    public static final ModConfigSpec.DoubleValue DARKNESS_NETHER_FOG_BRIGHT;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_ON_END;
    public static final ModConfigSpec.DoubleValue DARKNESS_END_FOG_BRIGHT;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_BY_DEFAULT;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_ON_NO_SKY_LIGHT;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_BLOCK_LIGHT_ONLY;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_ON_FULL_BRIGHT_BIOMES;
    public static final ModConfigSpec.ConfigValue<Boolean> DARKNESS_AFFECTED_BY_MOON_PHASE;
    public static final ModConfigSpec.DoubleValue DARKNESS_NEW_MOON_BRIGHT;
    public static final ModConfigSpec.DoubleValue DARKNESS_FULL_MOON_BRIGHT;
    // 夜视仪 IR 红外照明锥 (SDDL 世界光, 只有透过夜视仪可见; 按镜头档位)
    // 范围受 SDDL 迭代上限约束, 收在 ≤15。
    public static final ModConfigSpec.DoubleValue IR_SINGLE_RANGE;
    public static final ModConfigSpec.DoubleValue IR_DUAL_RANGE;
    public static final ModConfigSpec.DoubleValue IR_QUAD_RANGE;
    public static final ModConfigSpec.DoubleValue IR_SINGLE_LUM;
    public static final ModConfigSpec.DoubleValue IR_DUAL_LUM;
    public static final ModConfigSpec.DoubleValue IR_QUAD_LUM;
    public static final ModConfigSpec.DoubleValue IR_INNER_ANGLE;
    public static final ModConfigSpec.DoubleValue IR_OUTER_ANGLE;
    public static final ModConfigSpec.ConfigValue<Boolean> IR_OCCLUSION;
    // IR 测试实体(末影螨)/方块的全向光亮度
    public static final ModConfigSpec.DoubleValue IR_BLOCK_LUMINANCE;
    // 夜视画面整体亮度缩放 (压低可减少过曝、让 IR 对比更明显)
    public static final ModConfigSpec.DoubleValue NVG_BRIGHTNESS_SCALE;
    // 第三人称镜片蒙版相对第一人称的缩放
    public static final ModConfigSpec.DoubleValue NVG_THIRD_PERSON_MASK_SCALE;

    static {
        BUILDER.push("Modern Mayhem Client Config Settings");
        BUILDER.push("True Darkness Settings");
        DARKNESS_MODE = BUILDER.comment("Mode of darkness to apply. Options: VANILLA, DIM, DARK, DARKNESS, BLACK, BLACKNESS").defineEnum("darknessMode", (Enum)Darkness.DarkMode.VANILLA);
        BUILDER.push("Dimensions");
        DARKNESS_ON_OVERWORLD = BUILDER.comment("Apply darkness to the Overworld").define("darknessOnOverworld", true);
        DARKNESS_ON_NETHER = BUILDER.comment("Apply darkness to the Nether").define("darknessOnNether", false);
        DARKNESS_NETHER_FOG_BRIGHT = BUILDER.comment("Fog brightness in the Nether when darkness is enabled (0.0 to 1.0)").defineInRange("darknessNetherFogBright", 0.5, 0.0, 1.0);
        DARKNESS_ON_END = BUILDER.comment("Apply darkness to the End").define("darknessOnEnd", false);
        DARKNESS_END_FOG_BRIGHT = BUILDER.comment("Fog brightness in the End when darkness is enabled (0.0 to 1.0)").defineInRange("darknessEndFogBright", 0.5, 0.0, 1.0);
        DARKNESS_BY_DEFAULT = BUILDER.comment("Apply darkness to dimensions with sky light by default").define("darknessByDefault", false);
        DARKNESS_ON_NO_SKY_LIGHT = BUILDER.comment("Apply darkness to dimensions without sky light").define("darknessOnNoSkyLight", false);
        BUILDER.pop();
        BUILDER.push("Sky & Rendering");
        DARKNESS_BLOCK_LIGHT_ONLY = BUILDER.comment("If true, only modifies block light, ignoring sky light calculation").define("darknessBlockLightOnly", false);
        DARKNESS_ON_FULL_BRIGHT_BIOMES = BUILDER.comment("Apply darkness in biomes that are naturally full bright").define("darknessOnFullBrightBiomes", false);
        DARKNESS_AFFECTED_BY_MOON_PHASE = BUILDER.comment("If true, darkness brightness is affected by the moon phase").define("darknessAffectedByMoonPhase", true);
        DARKNESS_NEW_MOON_BRIGHT = BUILDER.comment("Brightness factor during a new moon (0.0 to 1.0)").defineInRange("darknessNewMoonBright", 0.0, 0.0, 1.0);
        DARKNESS_FULL_MOON_BRIGHT = BUILDER.comment("Brightness factor during a full moon (0.0 to 1.0)").defineInRange("darknessFullMoonBright", 0.25, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.pop();
        BUILDER.push("NVG IR Illuminator Settings");
        IR_SINGLE_RANGE = BUILDER.comment("IR cone range (blocks) for single-tube NVG (PVS-7). Larger = more chunk relighting cost on head-turn.").defineInRange("irSingleRange", 36.0, 3.0, 64.0);
        IR_DUAL_RANGE = BUILDER.comment("IR cone range (blocks) for dual-tube NVG (PVS-14).").defineInRange("irDualRange", 48.0, 3.0, 64.0);
        IR_QUAD_RANGE = BUILDER.comment("IR cone range (blocks) for quad-tube NVG (GPNVG). Toggleable.").defineInRange("irQuadRange", 60.0, 3.0, 64.0);
        IR_SINGLE_LUM = BUILDER.comment("IR cone brightness (0-15) for single-tube NVG (PVS-7). Lower = less NVG over-exposure.").defineInRange("irSingleLuminance", 4.0, 1.0, 15.0);
        IR_DUAL_LUM = BUILDER.comment("IR cone brightness (0-15) for dual-tube NVG (PVS-14).").defineInRange("irDualLuminance", 5.0, 1.0, 15.0);
        IR_QUAD_LUM = BUILDER.comment("IR cone brightness (0-15) for quad-tube NVG (GPNVG).").defineInRange("irQuadLuminance", 6.0, 1.0, 15.0);
        IR_INNER_ANGLE = BUILDER.comment("IR cone inner half-angle in radians (full brightness inside).").defineInRange("irInnerAngle", 0.5, 0.05, 1.4);
        IR_OUTER_ANGLE = BUILDER.comment("IR cone outer half-angle in radians (fades to zero at edge).").defineInRange("irOuterAngle", 0.7, 0.1, 1.5);
        IR_OCCLUSION = BUILDER.comment("If true, IR cone is blocked by solid blocks (raycast). Costlier but matters for balance.").define("irOcclusion", true);
        IR_BLOCK_LUMINANCE = BUILDER.comment("Omni light level (0-15) for the IR test entity (endermite) / block, only visible through NVG.").defineInRange("irBlockLuminance", 15.0, 1.0, 15.0);
        NVG_BRIGHTNESS_SCALE = BUILDER.comment("Overall NVG image brightness multiplier (1.0 = original). Lower reduces over-exposure / max auto-gain and improves IR contrast.").defineInRange("nvgBrightnessScale", 0.7, 0.1, 1.5);
        NVG_THIRD_PERSON_MASK_SCALE = BUILDER.comment("Third-person NVG lens mask size relative to first-person (1.0 = same; <1 smaller, >1 bigger).").defineInRange("nvgThirdPersonMaskScale", 0.85, 0.3, 2.0);
        BUILDER.pop();
        CONFIG = BUILDER.build();
    }

    /** IR 锥光范围(格), 按镜头档位。 */
    public static double irRangeFor(net.tkg.ModernMayhem.server.item.NVGGoggleList.IrTier tier) {
        switch (tier) {
            case QUAD: return IR_QUAD_RANGE.get();
            case DUAL: return IR_DUAL_RANGE.get();
            case SINGLE: return IR_SINGLE_RANGE.get();
            default: return 0.0;
        }
    }

    /** IR 锥光亮度(0~15), 按镜头档位。 */
    public static double irLumFor(net.tkg.ModernMayhem.server.item.NVGGoggleList.IrTier tier) {
        switch (tier) {
            case QUAD: return IR_QUAD_LUM.get();
            case DUAL: return IR_DUAL_LUM.get();
            case SINGLE: return IR_SINGLE_LUM.get();
            default: return 0.0;
        }
    }
}

