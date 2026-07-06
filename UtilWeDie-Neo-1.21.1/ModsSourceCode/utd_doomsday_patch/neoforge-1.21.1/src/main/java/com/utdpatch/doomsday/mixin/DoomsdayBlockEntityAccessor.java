package com.utdpatch.doomsday.mixin;

import net.mcreator.doomsdaydecoration.functionality.DoomsdayBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   value = {DoomsdayBlockEntity.class},
   remap = false
)
public interface DoomsdayBlockEntityAccessor {
   @Accessor("filledFromLoot")
   boolean utd$isFilledFromLoot();
}
