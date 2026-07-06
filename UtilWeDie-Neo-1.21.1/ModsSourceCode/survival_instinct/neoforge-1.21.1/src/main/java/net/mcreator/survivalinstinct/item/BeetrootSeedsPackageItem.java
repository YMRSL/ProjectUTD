package net.mcreator.survivalinstinct.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class BeetrootSeedsPackageItem
extends Item {
    public BeetrootSeedsPackageItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    }
}

