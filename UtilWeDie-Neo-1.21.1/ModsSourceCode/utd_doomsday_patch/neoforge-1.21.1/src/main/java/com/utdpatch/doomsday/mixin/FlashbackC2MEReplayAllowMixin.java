package com.utdpatch.doomsday.mixin;

import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.moulberry.flashback.Flashback", remap = false)
public class FlashbackC2MEReplayAllowMixin {

    @Inject(method = "getReplayIncompatibleMods", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void utd$allowC2MEReplay(CallbackInfoReturnable<List<String>> cir) {
        List<String> incompatible = cir.getReturnValue();
        if (incompatible == null || incompatible.isEmpty()) {
            return;
        }
        incompatible.removeIf(name -> name != null && name.contains("Concurrent Chunk Management Engine"));
    }
}
