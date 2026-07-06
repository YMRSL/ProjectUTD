package com.scarasol.zombiekit.item.api;

import com.scarasol.zombiekit.item.weapon.parts.BattleParts;
import com.scarasol.zombiekit.item.weapon.parts.ChargingParts;
import com.scarasol.zombiekit.item.weapon.parts.GripParts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;


public interface ModifiableWeapon {

    @Nullable
    default Item getGripParts(ItemStack itemStack) {
        return getParts(itemStack, "GripParts");
    }

    @Nullable
    default Item getChargingParts(ItemStack itemStack) {
        return getParts(itemStack, "ChargingParts");
    }

    @Nullable
    default Item getBattleParts(ItemStack itemStack) {
        return getParts(itemStack, "BattleParts");
    }

    @Nullable
    default Item getParts(ItemStack itemStack, String parts) {
        if (itemStack.getItem() instanceof ModifiableWeapon && itemStack.hasTag()) {
            String id = itemStack.getTag().getString(parts);
            return ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        }
        return null;
    }

    default void clearGripParts(ItemStack itemStack) {
        if (itemStack.hasTag() && itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.getTag().remove("GripParts");
        }
    }

    default void clearChargingParts(ItemStack itemStack) {
        if (itemStack.hasTag() && itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.getTag().remove("ChargingParts");
        }
    }

    default void clearBattleParts(ItemStack itemStack) {
        if (itemStack.hasTag() && itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.getTag().remove("BattleParts");
        }
    }

    default void setGripParts(ItemStack itemStack, GripParts gripParts) {
        setParts(itemStack, "GripParts", ForgeRegistries.ITEMS.getKey(gripParts).toString());
    }

    default void setChargingParts(ItemStack itemStack, ChargingParts gripParts) {
        setParts(itemStack, "ChargingParts", ForgeRegistries.ITEMS.getKey(gripParts).toString());
    }

    default void setBattleParts(ItemStack itemStack, BattleParts gripParts) {
        setParts(itemStack, "BattleParts", ForgeRegistries.ITEMS.getKey(gripParts).toString());
    }

    default void setParts(ItemStack itemStack, String parts, String partsId) {
        if (itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.getOrCreateTag().putString(parts, partsId);
        }
    }
}
