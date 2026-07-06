package net.mcreator.survivalinstinct.capability;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.items.ComponentItemHandler;

/**
 * Component-backed item handler for the survival_instinct bags. Restores the per-slot
 * stack limit (empty bag = 16, garbage bag = 1) and the no-bag-inside-itself rule that
 * the original Forge {@code ItemStackHandler} enforced before the NeoForge port.
 */
public class BagItemHandler extends ComponentItemHandler {
    private final int slotLimit;
    private final Supplier<Item> selfItem;

    public BagItemHandler(MutableDataComponentHolder parent, DataComponentType<ItemContainerContents> component, int slots, int slotLimit, Supplier<Item> selfItem) {
        super(parent, component, slots);
        this.slotLimit = slotLimit;
        this.selfItem = selfItem;
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.slotLimit;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() != this.selfItem.get() && super.isItemValid(slot, stack);
    }
}
