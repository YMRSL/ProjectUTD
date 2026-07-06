package org.yanbwe.searchcarefully.manager;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.yanbwe.searchcarefully.Config;
import org.yanbwe.searchcarefully.util.ItemStackHelper;
import org.yanbwe.searchcarefully.util.MouseTargetDetector;

@OnlyIn(Dist.CLIENT)
public class HybridSearchManager {

    public enum SearchMode {
        MOUSE_TARGET,
        AUTO_SEQUENTIAL
    }

    private static SearchMode currentMode = SearchMode.AUTO_SEQUENTIAL;
    private static Integer currentTargetSlot = null;
    private static int autoModeCurrentSlot = -1;
    private static boolean modeChangedThisTick = false;

    public static void updateSearchTarget(AbstractContainerScreen<?> screen) {
        if (screen == null || screen.getMenu() == null) {
            reset();
            return;
        }

        boolean singleSlotEnabled = Config.ENABLE_SINGLE_SLOT_SEARCH.get();
        if (!singleSlotEnabled) {
            reset();
            return;
        }

        modeChangedThisTick = false;

        // Update mouse target detector
        MouseTargetDetector.updateMouseTarget(screen);

        // Check mouse target
        boolean mouseTargetEnabled = Config.ENABLE_MOUSE_TARGET_SEARCH.get();
        Integer mouseTarget = null;
        if (mouseTargetEnabled) {
            mouseTarget = MouseTargetDetector.getCurrentMouseTarget(screen);
        }

        if (mouseTarget != null) {
            // Switch to mouse target mode
            if (currentMode != SearchMode.MOUSE_TARGET || !mouseTarget.equals(currentTargetSlot)) {
                currentMode = SearchMode.MOUSE_TARGET;
                currentTargetSlot = mouseTarget;
                modeChangedThisTick = true;
            }
        } else {
            // Auto sequential mode
            if (currentMode != SearchMode.AUTO_SEQUENTIAL) {
                currentMode = SearchMode.AUTO_SEQUENTIAL;
                autoModeCurrentSlot = -1;
                modeChangedThisTick = true;
            }
            updateAutoModeTarget(screen);
        }
    }

    private static void updateAutoModeTarget(AbstractContainerScreen<?> screen) {
        int nextSlot = findNextSearchableSlot(screen);
        if (nextSlot != currentTargetSlot) {
            currentTargetSlot = nextSlot;
            modeChangedThisTick = true;
        }
        autoModeCurrentSlot = nextSlot >= 0 ? nextSlot : autoModeCurrentSlot;
    }

    private static int findNextSearchableSlot(AbstractContainerScreen<?> screen) {
        if (screen == null || screen.getMenu() == null) return -1;

        var slots = screen.getMenu().slots;
        if (slots.isEmpty()) return -1;

        int startIndex = Math.max(0, autoModeCurrentSlot);
        int totalSlots = slots.size();

        // Search forward from current position
        for (int i = startIndex; i < totalSlots; i++) {
            Slot slot = slots.get(i);
            if (slot.hasItem() && ItemStackHelper.hasRemainingSearchTime(slot.getItem()) &&
                ItemStackHelper.getRemainingSearchTime(slot.getItem()) > 0) {
                return i;
            }
        }

        // Wrap around
        for (int i = 0; i < startIndex; i++) {
            Slot slot = slots.get(i);
            if (slot.hasItem() && ItemStackHelper.hasRemainingSearchTime(slot.getItem()) &&
                ItemStackHelper.getRemainingSearchTime(slot.getItem()) > 0) {
                return i;
            }
        }

        return -1;
    }

    public static Integer getCurrentSearchSlot() {
        return currentTargetSlot;
    }

    public static Slot getCurrentSearchSlotObject(AbstractContainerScreen<?> screen) {
        Integer index = getCurrentSearchSlot();
        if (index == null || screen == null || screen.getMenu() == null) return null;
        var slots = screen.getMenu().slots;
        if (index >= 0 && index < slots.size()) {
            return slots.get(index);
        }
        return null;
    }

    public static void forceSwitchToMode(SearchMode mode, Integer slotIndex) {
        currentMode = mode;
        currentTargetSlot = slotIndex;
        modeChangedThisTick = true;
    }

    public static void reset() {
        currentMode = SearchMode.AUTO_SEQUENTIAL;
        currentTargetSlot = null;
        autoModeCurrentSlot = -1;
        modeChangedThisTick = false;
        MouseTargetDetector.reset();
    }

    public static boolean isInMouseTargetMode() {
        return currentMode == SearchMode.MOUSE_TARGET;
    }

    public static boolean isInAutoMode() {
        return currentMode == SearchMode.AUTO_SEQUENTIAL;
    }

    public static boolean isCurrentTargetValid(AbstractContainerScreen<?> screen) {
        Slot slot = getCurrentSearchSlotObject(screen);
        return slot != null && slot.hasItem() &&
               ItemStackHelper.hasRemainingSearchTime(slot.getItem()) &&
               ItemStackHelper.getRemainingSearchTime(slot.getItem()) > 0;
    }

    public static boolean isModeChangedThisTick() {
        return modeChangedThisTick;
    }

    public static SearchMode getCurrentMode() {
        return currentMode;
    }

    public static void clearModeChangedFlag() {
        modeChangedThisTick = false;
    }
}
