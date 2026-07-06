package com.ymrsl.vehicleload.compat;

import com.ymrsl.vehicleload.VehicleLoadMod;
import java.lang.reflect.Method;
import net.neoforged.fml.ModList;

/**
 * Reflection gate to Flashback's replay state (fabric mod under Connector,
 * same reflection idiom as the other compat bridges in this mod).
 */
public final class FlashbackReplayCompat {
    private static boolean initAttempted;
    private static Method isInReplayMethod;

    private FlashbackReplayCompat() {
    }

    /** True while the client is inside a Flashback replay world. */
    public static boolean isInReplay() {
        if (!ModList.get().isLoaded("flashback")) {
            return false;
        }
        if (!initAttempted) {
            initAttempted = true;
            try {
                isInReplayMethod = Class.forName("com.moulberry.flashback.Flashback").getMethod("isInReplay");
            } catch (ReflectiveOperationException e) {
                VehicleLoadMod.LOGGER.warn("FlashbackReplayCompat: reflection init failed", e);
            }
        }
        if (isInReplayMethod == null) {
            return false;
        }
        try {
            return (boolean) isInReplayMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
