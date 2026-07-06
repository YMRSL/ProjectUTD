package net.mcreator.survivalinstinct.item;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

public class BaseballBatSwordItem
extends SwordItem {
    private static final Tier TIER = new Tier(){

            public int getUses() {
                return 80;
            }

            public float getSpeed() {
                return 4.0f;
            }

            public float getAttackDamageBonus() {
                return 1.0f;
            }

            public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
        }

            public int getEnchantmentValue() {
                return 15;
            }

            public Ingredient getRepairIngredient() {
                return Ingredient.of((TagKey)ItemTags.create((ResourceLocation)ResourceLocation.parse("minecraft:planks")));
            }
        };

    public BaseballBatSwordItem() {
        super(TIER, new Item.Properties().attributes(SwordItem.createAttributes(TIER, 3, -2.2f)));
    }
}

