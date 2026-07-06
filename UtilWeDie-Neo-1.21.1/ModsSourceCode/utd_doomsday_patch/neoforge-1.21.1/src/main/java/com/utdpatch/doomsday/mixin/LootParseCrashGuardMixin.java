package com.utdpatch.doomsday.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

/**
 * A loot-family element (loot table / predicate / item modifier) that fails to
 * PARSE normally just logs "Couldn't parse element" and is skipped — but only
 * codec-level failures come back as DataResult errors. A raw RuntimeException
 * thrown mid-decode (seen in this pack: a ClassCastException from a registry
 * holder lookup while Flashback's replay loader force-enables every datapack)
 * escapes {@code LootDataType.deserialize}, aborts the whole registry load and
 * crashes the game — with no hint of WHICH file blew up.
 *
 * <p>This guard makes hard throws behave like parse failures: log the exact
 * element id (+ the exception, so the culprit mod's frames are visible) and
 * skip it, keeping world/replay loading alive.
 */
@Mixin(LootDataType.class)
public class LootParseCrashGuardMixin {
    private static final Logger UTD$LOGGER = LogManager.getLogger("utd_doomsday_patch");

    @WrapMethod(method = "deserialize")
    private Optional<Object> utd$lootParseGuard(ResourceLocation id, DynamicOps<Object> ops, Object value,
                                                Operation<Optional<Object>> original) {
        try {
            return original.call(id, ops, value);
        } catch (RuntimeException | LinkageError e) {
            UTD$LOGGER.error("[UTD-PATCH] Loot element '{}' of {} threw during parse - skipped. Full error:",
                    id, ((LootDataType<?>) (Object) this).registryKey(), e);
            return Optional.empty();
        }
    }
}
