package net.tejty.just_barricades.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.tejty.just_barricades.JustBarricades;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> BARRICADE = tag("barricade");

        private static TagKey<Block> tag(String name) {
            return BlockTags.create(new ResourceLocation(JustBarricades.MODID, name));
        }
    }
}
