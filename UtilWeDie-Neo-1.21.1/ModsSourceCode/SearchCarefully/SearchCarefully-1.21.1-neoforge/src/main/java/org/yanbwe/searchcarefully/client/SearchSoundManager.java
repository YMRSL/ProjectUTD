package org.yanbwe.searchcarefully.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.yanbwe.searchcarefully.sounds.SearchCompletionSound;

@OnlyIn(Dist.CLIENT)
public class SearchSoundManager {

    private static SearchProgressLoopingSound activeSound;

    public static void startSearchLoopSound(Player player) {
        if (activeSound != null && !activeSound.isStopped()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null && player != null) {
            activeSound = new SearchProgressLoopingSound(player);
            mc.getSoundManager().play(activeSound);
        }
    }

    public static void stopSearchLoopSound() {
        if (activeSound != null) {
            activeSound.stop();
            activeSound = null;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().stop(SearchCompletionSound.SEARCH_PROGRESS_SOUND_ID, null);
        }
    }

    public static boolean isSoundPlaying() {
        return activeSound != null && !activeSound.isStopped();
    }
}
