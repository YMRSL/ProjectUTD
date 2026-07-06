package com.ymrsl.vehicleload;

import com.ymrsl.vehicleload.compat.FlashbackReplayCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Keeps SuperbWarfare vehicles still inside Flashback replays.
 *
 * In a replay the viewer's client never receives the Create SeatEntity a
 * seated vehicle was riding (it lives at plot coordinates — the replay server
 * re-streams entities by vanilla view distance, which never covers plots), so
 * the riding link that normally suppresses the vehicle's physics is missing.
 * Flashback's own accurate per-tick entity position channel keeps snapping
 * the vehicle to its recorded world position, while SW's client-side physics
 * applies gravity in between — the tug-of-war shows as twitching/falling.
 *
 * Replays are pure playback: recorded positions ARE the truth. So while in a
 * replay, zero out client physics state for every target vehicle each tick
 * and let Flashback's position stream drive it exclusively. This also makes
 * driving (non-seated) vehicles follow their recorded paths more faithfully.
 */
public class ReplayVehicleCalmer {
    private static long calmed;

    @SubscribeEvent
    public void onEntityTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide()) {
            return;
        }
        if (!VehicleCompat.isTargetVehicle(entity) || entity.isPassenger()) {
            return;
        }
        if (!FlashbackReplayCompat.isInReplay()) {
            return;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        entity.setOnGround(true);
        entity.fallDistance = 0.0F;
        calmed++;
        if (calmed == 1 || calmed % 1200 == 0) {
            VehicleLoadMod.LOGGER.info("[REPLAY-CALM] suppressing client physics for replay vehicles (events={})", calmed);
        }
    }
}
