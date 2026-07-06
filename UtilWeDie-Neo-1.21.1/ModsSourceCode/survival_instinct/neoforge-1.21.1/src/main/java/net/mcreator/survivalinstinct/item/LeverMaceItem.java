package net.mcreator.survivalinstinct.item;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class LeverMaceItem
extends SwordItem {
    private static final Tier TIER = new Tier(){

            public int getUses() {
                return 670;
            }

            public float getSpeed() {
                return 4.0f;
            }

            public float getAttackDamageBonus() {
                return 2.5f;
            }

            public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_IRON_TOOL;
        }

            public int getEnchantmentValue() {
                return 12;
            }

            public Ingredient getRepairIngredient() {
                return Ingredient.of((ItemStack[])new ItemStack[]{new ItemStack((ItemLike)SurvivalInstinctModItems.STEELLIUM.get())});
            }
        };

    public LeverMaceItem() {
        super(TIER, new Item.Properties().attributes(SwordItem.createAttributes(TIER, 3, -2.2f)));
    }
}

