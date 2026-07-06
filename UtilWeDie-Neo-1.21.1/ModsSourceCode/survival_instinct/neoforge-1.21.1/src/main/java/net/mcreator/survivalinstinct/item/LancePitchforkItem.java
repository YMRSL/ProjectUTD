package net.mcreator.survivalinstinct.item;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DiggerItem;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public class LancePitchforkItem
extends HoeItem {
    private static final Tier TIER = new Tier(){

            public int getUses() {
                return 321;
            }

            public float getSpeed() {
                return 7.5f;
            }

            public float getAttackDamageBonus() {
                return 5.8f;
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

    public LancePitchforkItem() {
        super(TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TIER, 0, -2.8f)));
    }
}

