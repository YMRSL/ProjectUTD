package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.UTDAssetManagerMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = UTDAssetManagerMod.MOD_ID, value = Dist.CLIENT)
public final class ClientRuntimeEvents {
    private ClientRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientKeyMappings.OPEN_MANAGER.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null
                    || minecraft.screen instanceof AssetManagerScreen) {
                continue;
            }
            minecraft.setScreen(new AssetManagerScreen(minecraft.screen));
        }
    }
}
