package net.mcreator.survivalinstinct.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class MelonSeedsPackageItem
extends Item {
    public MelonSeedsPackageItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    }
}

