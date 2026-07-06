package net.tejty.just_barricades.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

public class JustBarricadesCommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<String> REPAIR_ITEM;
    public static final ForgeConfigSpec.DoubleValue DROP_REPAIR_ITEM_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ZOMBIE_BREAK_CHANCE;
    public static final ForgeConfigSpec.BooleanValue REPAIR_AFTER_ZOMBIE;
    public static final ForgeConfigSpec.BooleanValue PLAYER_CAN_BREAK_COMPLETELY;

    static {
        BUILDER.push("Config for Just Barricades");

        REPAIR_ITEM = BUILDER.comment("The id of item that will be required for repairing. Leave blank for no item requirement").define("repair_item", "minecraft:stick", (name) -> validateItemName(name) || name == "");
        DROP_REPAIR_ITEM_CHANCE = BUILDER.comment("The chance of dropping the repair item (doesn't work if no repair item is needed)").defineInRange("drop_repair_item_chance", 1D, 0D, 1D);
        ZOMBIE_BREAK_CHANCE = BUILDER.comment("The chance that zombie breaks one level each tick").defineInRange("zombie_break_chance", 0.025D, 0D, 1D);
        REPAIR_AFTER_ZOMBIE = BUILDER.comment("If zombies leave repairable leftovers").define("repair_after_zombie", true);
        PLAYER_CAN_BREAK_COMPLETELY = BUILDER.comment("If players can break barricades completely without leftovers").define("player_can_break_completely", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }
}
