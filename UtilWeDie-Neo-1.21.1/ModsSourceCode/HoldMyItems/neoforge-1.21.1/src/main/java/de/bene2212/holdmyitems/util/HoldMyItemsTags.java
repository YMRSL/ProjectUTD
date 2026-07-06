package de.bene2212.holdmyitems.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class HoldMyItemsTags {
    public static final TagKey<Item> LANTERNS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "lanterns"));
    public static final TagKey<Item> BUCKETS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "buckets"));
    public static final TagKey<Block> GLASS_PANES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "glass_panes"));
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "tools"));
    public static final TagKey<Item> GEMS_TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "gems"));
    public static final TagKey<Block> CHAINS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "chains"));
    public static final TagKey<Block> REPLACEABLE_BY_MUSHROOMS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "replaceable_by_mushrooms"));
}
