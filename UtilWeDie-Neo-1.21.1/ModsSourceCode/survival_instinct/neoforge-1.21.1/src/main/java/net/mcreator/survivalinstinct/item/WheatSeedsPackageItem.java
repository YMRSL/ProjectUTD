package net.mcreator.survivalinstinct.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class WheatSeedsPackageItem
extends Item {
    public WheatSeedsPackageItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    }
}

