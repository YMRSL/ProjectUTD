package io.github.ymrsl.firstpersonfoodeating.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModCommonConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLE_FLAVOR_MESSAGES;
    public static final ModConfigSpec.BooleanValue CONSUME_FLASH_AFFECTS_NEARBY_PLAYERS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("ui");
        ENABLE_FLAVOR_MESSAGES = builder
                .comment("Whether post-consume flavor messages are shown to players.")
                .define("enableFlavorMessages", true);
        CONSUME_FLASH_AFFECTS_NEARBY_PLAYERS = builder
                .comment("If true, syringe consume flash can be triggered by nearby players; if false, only your own consume triggers your flash.")
                .define("consumeFlashAffectsNearbyPlayers", false);
        builder.pop();
        SPEC = builder.build();
    }

    private ModCommonConfig() {
    }
}
