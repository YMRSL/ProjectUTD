package com.ymrsl.vehicleload;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.Entity;

public final class SeatCooldowns {
    private static final long COOLDOWN_TICKS = 200L;
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    private SeatCooldowns() {
    }

    public static void block(Entity entity, long gameTime) {
        if (entity == null) {
            return;
        }
        COOLDOWNS.put(entity.getUUID(), gameTime + COOLDOWN_TICKS);
    }

    public static boolean isBlocked(Entity entity, long gameTime) {
        if (entity == null) {
            return false;
        }
        Long expiresAt = COOLDOWNS.get(entity.getUUID());
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= gameTime) {
            COOLDOWNS.remove(entity.getUUID());
            return false;
        }
        return true;
    }
}
