package com.utdpatch.doomsday.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Aeronautics' HotAirBurnerBlockEntity.write casts its balloon reference to
 * ServerBalloon unconditionally. Vanilla only calls getUpdateTag (-> write)
 * on the server, so it never crashes in normal play — but the sable x
 * Flashback bridge builds chunk packets from CLIENT chunks at record time,
 * where the balloon is a ClientBalloon -> ClassCastException, losing the
 * whole chunk from the replay. Skip the burner's custom write on the client;
 * the replayed burner just shows default state.
 */
@Pseudo
@Mixin(targets = "dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity", remap = false)
public class AeronauticsBurnerClientWriteGuardMixin {
    @Inject(method = "write", at = @At("HEAD"), cancellable = true, require = 0)
    private void utd$skipClientSideWrite(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket,
                                         CallbackInfo ci) {
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level != null && level.isClientSide()) {
            ci.cancel();
        }
    }
}
