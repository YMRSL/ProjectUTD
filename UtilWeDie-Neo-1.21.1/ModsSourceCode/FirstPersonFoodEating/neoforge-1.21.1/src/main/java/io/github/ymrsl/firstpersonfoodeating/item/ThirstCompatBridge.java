package io.github.ymrsl.firstpersonfoodeating.item;

import dev.ghen.thirst.foundation.common.capability.IThirst;
import dev.ghen.thirst.foundation.common.capability.ModAttachment;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * Compatibility bridge for the "Thirst Was Taken" mod (modid {@code thirst}, package
 * {@code dev.ghen.thirst}) — the NeoForge 1.21.1 continuation of the same dev.ghen.thirst
 * project the original 1.20.1 build integrated with.
 *
 * <p>On 1.21.1 that mod stores per-player thirst via a NeoForge data attachment
 * ({@link ModAttachment#PLAYER_THIRST}) whose value implements {@link IThirst}. We read the
 * attachment off the player and apply the food's thirst/water deltas through
 * {@link IThirst#drink(int, int)} (add-with-clamp, i.e. the "consuming restores thirst"
 * semantics), then push the change to the client via {@link IThirst#updateThirstData(Player)}.
 *
 * <p>Everything is gated on the mod being loaded and wrapped defensively, so the bridge degrades
 * to a safe no-op if {@code thirst} is absent — without touching the call sites in
 * {@link ProfiledConsumableItem}.
 */
public final class ThirstCompatBridge {
    private static final boolean THIRST_PRESENT = ModList.get().isLoaded("thirst");

    private ThirstCompatBridge() {
    }

    public static boolean isAvailable() {
        return THIRST_PRESENT;
    }

    public static int applyThirstDelta(Player player, int delta) {
        return applyThirstDelta(player, delta, 0);
    }

    public static int applyThirstDelta(Player player, int thirstDelta, int waterDelta) {
        if (!THIRST_PRESENT || player == null) {
            return 0;
        }
        // Thirst data is server-authoritative; only mutate on the logical server.
        if (player.level().isClientSide()) {
            return 0;
        }
        try {
            IThirst thirst = player.getData(ModAttachment.PLAYER_THIRST.get());
            if (thirst == null) {
                return 0;
            }
            thirst.drink(thirstDelta, waterDelta);
            thirst.updateThirstData(player);
            return thirstDelta;
        } catch (Throwable t) {
            // An optional-integration failure must never break eating.
            return 0;
        }
    }
}
