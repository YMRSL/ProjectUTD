package com.scarasol.zombiekit.block;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;

/**
 * 1.21.1 removed {@code Tier#getLevel()} (the legacy harvest-level int). The upstream ZSK
 * {@code canHarvestBlock} overrides gated harvesting by tool tier level (0 = wood/gold, 1 = stone,
 * 2 = iron, 3 = diamond, 4 = netherite). This helper restores that numeric level faithfully by
 * mapping a {@link Tier} via its {@link Tier#getIncorrectBlocksForDrops()} tag, falling back to the
 * vanilla {@link Tiers} ordinal so modded tiers that reuse the vanilla incorrect-block tags keep
 * the same ordering.
 */
public final class HarvestTiers {

    private HarvestTiers() {}

    public static int getLevel(Tier tier) {
        TagKey<Block> incorrect = tier.getIncorrectBlocksForDrops();
        if (incorrect == BlockTags.INCORRECT_FOR_WOODEN_TOOL || incorrect == BlockTags.INCORRECT_FOR_GOLD_TOOL)
            return 0;
        if (incorrect == BlockTags.INCORRECT_FOR_STONE_TOOL)
            return 1;
        if (incorrect == BlockTags.INCORRECT_FOR_IRON_TOOL)
            return 2;
        if (incorrect == BlockTags.INCORRECT_FOR_DIAMOND_TOOL)
            return 3;
        if (incorrect == BlockTags.INCORRECT_FOR_NETHERITE_TOOL)
            return 4;
        if (tier instanceof Tiers vanilla) {
            return switch (vanilla) {
                case WOOD, GOLD -> 0;
                case STONE -> 1;
                case IRON -> 2;
                case DIAMOND -> 3;
                case NETHERITE -> 4;
            };
        }
        return 0;
    }
}
