package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.UTDAssetManagerMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = UTDAssetManagerMod.MOD_ID, value = Dist.CLIENT)
public final class ClientKeyMappings {
    public static final KeyMapping OPEN_MANAGER = new KeyMapping(
            "key.utd_asset_manager.open",
            GLFW.GLFW_KEY_O,
            "key.categories.utd_asset_manager"
    );

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MANAGER);
    }
}
