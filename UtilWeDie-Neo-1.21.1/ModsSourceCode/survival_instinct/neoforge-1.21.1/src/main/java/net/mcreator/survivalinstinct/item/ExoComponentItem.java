package net.mcreator.survivalinstinct.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ExoComponentItem
extends Item {
    public ExoComponentItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON));
    }
}

