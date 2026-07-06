package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.data.ItemGridDatapackLoader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = BlockZ.MODID, bus = EventBusSubscriber.Bus.GAME)
public class DataReloadEvents {
    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ItemGridDatapackLoader());
    }
}
