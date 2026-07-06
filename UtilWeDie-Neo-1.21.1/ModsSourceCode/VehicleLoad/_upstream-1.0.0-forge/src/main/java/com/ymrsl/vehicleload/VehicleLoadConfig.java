package com.ymrsl.vehicleload;

import net.minecraftforge.common.ForgeConfigSpec;

public final class VehicleLoadConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SEAT_SLIME_Y_OFFSET;
    public static final ForgeConfigSpec.BooleanValue DEBUG_LOG;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        SEAT_SLIME_Y_OFFSET = builder
            .comment("Vertical offset applied to vehicles riding seat mounts (negative lowers).")
            .defineInRange("seatSlimeYOffset", -1.0D, -4.0D, 4.0D);

        DEBUG_LOG = builder
            .comment("Enable extra log output for troubleshooting.")
            .define("debugLog", false);

        builder.pop();
        SPEC = builder.build();
    }

    private VehicleLoadConfig() {
    }
}
