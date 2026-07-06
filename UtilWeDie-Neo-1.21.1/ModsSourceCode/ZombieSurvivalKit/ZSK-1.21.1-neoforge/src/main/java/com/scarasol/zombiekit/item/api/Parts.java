package com.scarasol.zombiekit.item.api;

import net.minecraft.world.item.ItemStack;

public interface Parts {

    int getPartsLevel();

    PartsType getPartsType();

    boolean canUse(ItemStack itemStack);

    enum PartsType {
        BATTLE,
        CHARGING,
        GRIP
    }
}
