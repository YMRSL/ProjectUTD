package com.scarasol.zombiekit.item.api;

import com.scarasol.zombiekit.init.ZombieKitDataComponents;
import com.scarasol.zombiekit.item.weapon.parts.BattleParts;
import com.scarasol.zombiekit.item.weapon.parts.ChargingParts;
import com.scarasol.zombiekit.item.weapon.parts.GripParts;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Supplier;


public interface ModifiableWeapon {

    @Nullable
    default Item getGripParts(ItemStack itemStack) {
        return getParts(itemStack, ZombieKitDataComponents.GRIP_PARTS);
    }

    @Nullable
    default Item getChargingParts(ItemStack itemStack) {
        return getParts(itemStack, ZombieKitDataComponents.CHARGING_PARTS);
    }

    @Nullable
    default Item getBattleParts(ItemStack itemStack) {
        return getParts(itemStack, ZombieKitDataComponents.BATTLE_PARTS);
    }

    @Nullable
    default Item getParts(ItemStack itemStack, Supplier<DataComponentType<String>> parts) {
        if (itemStack.getItem() instanceof ModifiableWeapon && itemStack.has(parts.get())) {
            String id = itemStack.get(parts.get());
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        }
        return null;
    }

    default void clearGripParts(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.remove(ZombieKitDataComponents.GRIP_PARTS.get());
        }
    }

    default void clearChargingParts(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.remove(ZombieKitDataComponents.CHARGING_PARTS.get());
        }
    }

    default void clearBattleParts(ItemStack itemStack) {
        if (itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.remove(ZombieKitDataComponents.BATTLE_PARTS.get());
        }
    }

    default void setGripParts(ItemStack itemStack, GripParts gripParts) {
        setParts(itemStack, ZombieKitDataComponents.GRIP_PARTS, BuiltInRegistries.ITEM.getKey(gripParts).toString());
    }

    default void setChargingParts(ItemStack itemStack, ChargingParts gripParts) {
        setParts(itemStack, ZombieKitDataComponents.CHARGING_PARTS, BuiltInRegistries.ITEM.getKey(gripParts).toString());
    }

    default void setBattleParts(ItemStack itemStack, BattleParts gripParts) {
        setParts(itemStack, ZombieKitDataComponents.BATTLE_PARTS, BuiltInRegistries.ITEM.getKey(gripParts).toString());
    }

    default void setParts(ItemStack itemStack, Supplier<DataComponentType<String>> parts, String partsId) {
        if (itemStack.getItem() instanceof ModifiableWeapon) {
            itemStack.set(parts.get(), partsId);
        }
    }
}
