package org.yanbwe.searchcarefully.util;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.yanbwe.searchcarefully.Config;
import org.yanbwe.searchcarefully.mixin.ContainerAccessMixin;

@OnlyIn(Dist.CLIENT)
public class MouseTargetDetector {

    private static int lastHoveredSlotIndex = -1;
    private static int switchDelayCounter = 0;
    private static Integer pendingTargetSlot = null;

    public static Slot getHoveredSlot(AbstractContainerScreen<?> screen) {
        if (screen == null) return null;
        return ((ContainerAccessMixin) screen).getHoveredSlot();
    }

    public static int getHoveredSlotIndex(AbstractContainerScreen<?> screen) {
        Slot slot = getHoveredSlot(screen);
        return slot != null ? slot.index : -1;
    }

    public static boolean hasSearchableItem(Slot slot) {
        if (slot == null || !slot.hasItem()) return false;
        return ItemStackHelper.hasRemainingSearchTime(slot.getItem()) &&
               ItemStackHelper.getRemainingSearchTime(slot.getItem()) > 0;
    }

    public static boolean isHoveringSearchableItem(AbstractContainerScreen<?> screen) {
        Slot slot = getHoveredSlot(screen);
        return hasSearchableItem(slot);
    }

    public static void updateMouseTarget(AbstractContainerScreen<?> screen) {
        if (!isMouseTargetEnabled()) {
            pendingTargetSlot = null;
            switchDelayCounter = 0;
            return;
        }

        int currentHovered = getHoveredSlotIndex(screen);

        if (currentHovered >= 0) {
            // Check if the hovered slot has a searchable item
            Slot slot = getHoveredSlot(screen);
            if (hasSearchableItem(slot)) {
                if (currentHovered != lastHoveredSlotIndex) {
                    // Mouse moved to a different slot
                    pendingTargetSlot = currentHovered;
                    switchDelayCounter = 0;
                } else {
                    // Same slot still hovered
                    if (pendingTargetSlot != null) {
                        double delayTicks = Config.MOUSE_TARGET_SWITCH_DELAY.get() * 20.0;
                        switchDelayCounter++;
                        if (switchDelayCounter >= (int) delayTicks) {
                            // Delay passed, clear pending - target is now active
                            pendingTargetSlot = null;
                        }
                    }
                }
            } else if (currentHovered == lastHoveredSlotIndex) {
                // No searchable item on current slot, keep existing state
            } else {
                // Moved to a non-searchable slot
                pendingTargetSlot = null;
                switchDelayCounter = 0;
            }
        } else {
            pendingTargetSlot = null;
            switchDelayCounter = 0;
        }

        lastHoveredSlotIndex = currentHovered;
    }

    public static Integer getPendingTargetSlot() {
        return pendingTargetSlot;
    }

    public static void clearPendingTarget() {
        pendingTargetSlot = null;
        switchDelayCounter = 0;
    }

    public static boolean isMouseTargetEnabled() {
        return Config.ENABLE_MOUSE_TARGET_SEARCH.get() && Config.ENABLE_SINGLE_SLOT_SEARCH.get();
    }

    public static Integer getCurrentMouseTarget(AbstractContainerScreen<?> screen) {
        if (!isMouseTargetEnabled()) return null;

        Integer pending = getPendingTargetSlot();
        if (pending != null) {
            return pending;
        }

        Slot slot = getHoveredSlot(screen);
        if (slot != null && hasSearchableItem(slot)) {
            return slot.index;
        }

        return null;
    }

    public static void reset() {
        lastHoveredSlotIndex = -1;
        switchDelayCounter = 0;
        pendingTargetSlot = null;
    }
}
