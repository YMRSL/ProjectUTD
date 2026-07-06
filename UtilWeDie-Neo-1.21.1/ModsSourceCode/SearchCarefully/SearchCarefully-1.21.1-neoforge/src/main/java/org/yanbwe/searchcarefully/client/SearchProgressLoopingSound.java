package org.yanbwe.searchcarefully.client;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.yanbwe.searchcarefully.sounds.SearchCompletionSound;

@OnlyIn(Dist.CLIENT)
public class SearchProgressLoopingSound extends AbstractSoundInstance implements TickableSoundInstance {

    private final Player player;
    private boolean stopped;

    public SearchProgressLoopingSound(Player player) {
        super(SearchCompletionSound.SEARCH_PROGRESS_SOUND_EVENT, SoundSource.PLAYERS, RandomSource.create());
        this.player = player;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.5f;
        this.pitch = 1.0f;
        this.stopped = false;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    @Override
    public void tick() {
        if (stopped || player == null || !player.isAlive()) {
            this.stop();
            return;
        }
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        this.stopped = true;
        this.looping = false;
    }

    public boolean shouldPlay() {
        return !stopped && player != null && player.isAlive();
    }
}
