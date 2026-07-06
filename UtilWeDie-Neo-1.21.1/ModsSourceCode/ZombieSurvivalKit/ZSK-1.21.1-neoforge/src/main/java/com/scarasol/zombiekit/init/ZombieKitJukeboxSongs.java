package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.JukeboxSong;

/**
 * 1.21 移除了 RecordItem，唱片改为数据包驱动的 JukeboxSong + 物品上的 jukebox_playable 组件。
 * 上游 KonnGara extends RecordItem(comparator=15, sound=konn_gara, length=204s)。
 * 这里仅保留 ResourceKey；具体的 JukeboxSong 由数据包定义：
 *   data/zombiekit/jukebox_song/konn_gara.json
 * （sound_event=zombiekit:konn_gara, length_in_seconds=204, comparator_output=15）。
 */
public class ZombieKitJukeboxSongs {
    public static final ResourceKey<JukeboxSong> KONN_GARA =
            ResourceKey.create(Registries.JUKEBOX_SONG, ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "konn_gara"));
}
