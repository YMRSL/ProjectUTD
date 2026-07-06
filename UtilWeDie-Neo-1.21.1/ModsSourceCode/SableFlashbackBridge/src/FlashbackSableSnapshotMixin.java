package com.utdpatch.doomsday.mixin;

import com.utdpatch.doomsday.compat.SableFlashbackBridge;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.neoforged.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks Flashback's OFFICIAL mod-integration point for snapshot data
 * ("Mods can mixin here if they want to add custom actions ... or packets")
 * and appends sable's structure full-sync packets — see
 * {@link SableFlashbackBridge}. Runs on every snapshot (recording start and
 * pause-resume), so structures exist in the replay no matter when recording
 * began.
 */
@Pseudo
@Mixin(targets = "com.moulberry.flashback.record.Recorder", remap = false)
public class FlashbackSableSnapshotMixin {
    private static final Logger UTD$LOGGER = LogManager.getLogger("utd_doomsday_patch");

    @Inject(method = "writeCustomSnapshot", at = @At("HEAD"), remap = false, require = 0)
    private void utd$injectSableStructures(Consumer<Packet<? super ClientGamePacketListener>> consumer, CallbackInfo ci) {
        if (!ModList.get().isLoaded("sable")) {
            return;
        }
        try {
            SableFlashbackBridge.writeSableSnapshot(consumer);
        } catch (Throwable t) {
            UTD$LOGGER.error("[UTD-PATCH] Failed to inject sable structures into Flashback snapshot", t);
        }
    }
}
