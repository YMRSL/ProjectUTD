package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.UTDAssetManagerMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@EventBusSubscriber(modid = UTDAssetManagerMod.MOD_ID, value = Dist.CLIENT)
public final class ClientRuntimeEvents {
    private ClientRuntimeEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientKeyMappings.OPEN_MANAGER.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
                continue;
            }
            minecraft.setScreen(new AssetManagerScreen(null));
        }
    }

    @SubscribeEvent
    public static void onContainerKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null
                || !(event.getScreen() instanceof AbstractContainerScreen<?>)) {
            return;
        }
        if (!ClientKeyMappings.OPEN_MANAGER.matches(event.getKeyCode(), event.getScanCode())) {
            return;
        }
        event.setCanceled(true);
        minecraft.setScreen(new AssetManagerScreen(event.getScreen()));
    }
}
