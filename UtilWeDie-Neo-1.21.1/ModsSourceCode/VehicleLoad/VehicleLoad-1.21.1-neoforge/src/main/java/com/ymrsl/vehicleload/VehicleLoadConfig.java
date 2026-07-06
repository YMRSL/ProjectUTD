package com.ymrsl.vehicleload;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VehicleLoadConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue SEAT_SLIME_Y_OFFSET;
    public static final ModConfigSpec.BooleanValue DEBUG_LOG;
    public static final ModConfigSpec.BooleanValue SABLE_SEAT_ATTACH;
    public static final ModConfigSpec.DoubleValue SABLE_SEAT_Y_OFFSET;
    public static final ModConfigSpec.DoubleValue SABLE_TURRET_YAW_FIX;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");

        SEAT_SLIME_Y_OFFSET = builder
            .comment("Vertical offset applied to vehicles riding seat mounts (negative lowers).")
            .defineInRange("seatSlimeYOffset", -1.0D, -4.0D, 4.0D);

        DEBUG_LOG = builder
            .comment("Enable extra log output for troubleshooting.")
            .define("debugLog", false);

        SABLE_SEAT_ATTACH = builder
            .comment("Attach vehicles to Create seat blocks placed on Sable physics structures (airships etc.).")
            .define("sableSeatAttach", true);

        SABLE_SEAT_Y_OFFSET = builder
            .comment("Extra vertical offset (in structure-local space) for vehicles locked to Sable structure seats.")
            .defineInRange("sableSeatYOffset", 0.0D, -4.0D, 4.0D);

        SABLE_TURRET_YAW_FIX = builder
            .comment("Turret model yaw correction for vehicles seated on Sable structures.",
                     "0 = off (default; the render-rotation exemption keeps the model in the world",
                     "frame already). -1/1 subtract/add the structure yaw at the model layer.")
            .defineInRange("sableTurretYawFix", 0.0D, -1.0D, 1.0D);

        builder.pop();
        SPEC = builder.build();
    }

    private VehicleLoadConfig() {
    }
}
