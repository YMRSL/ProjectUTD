package com.scarasol.zombiekit.item.bonus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 1.20.1 KonnGara extends RecordItem(comparatorValue=15, sound=konn_gara, length=204s)。
 * 1.21 移除 RecordItem，唱片改为 jukebox_playable 组件 + 数据包 JukeboxSong。
 * 注册时在 Properties 上 .jukeboxPlayable(ZombieKitJukeboxSongs.KONN_GARA)；
 * 比较器输出/时长在 data/zombiekit/jukebox_song/konn_gara.json。
 * 本类只保留 tooltip 与附魔光效。
 */
public class KonnGara extends Item {

    public KonnGara(Properties builder) {
        super(builder);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.konn_gara.description"));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isFoil(ItemStack itemstack) {
        return true;
    }
}
