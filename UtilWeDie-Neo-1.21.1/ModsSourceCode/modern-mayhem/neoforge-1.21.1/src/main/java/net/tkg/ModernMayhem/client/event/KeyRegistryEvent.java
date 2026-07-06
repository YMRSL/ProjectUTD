package net.tkg.ModernMayhem.client.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tkg.ModernMayhem.server.registry.KeyMappingRegistryMM;

@EventBusSubscriber(modid="mm", value={Dist.CLIENT}, bus=EventBusSubscriber.Bus.MOD)
public class KeyRegistryEvent {
    @SubscribeEvent
    public static void onKeyRegisterEvent(RegisterKeyMappingsEvent event) {
        event.register(KeyMappingRegistryMM.TOGGLE_NVG_KEY);
        event.register(KeyMappingRegistryMM.INCREASE_TUBE_GAIN_KEY);
        event.register(KeyMappingRegistryMM.DECREASE_TUBE_GAIN_KEY);
        event.register(KeyMappingRegistryMM.TOGGLE_AUTO_GAIN_KEY);
        event.register(KeyMappingRegistryMM.TOGGLE_COTI_KEY);
        event.register(KeyMappingRegistryMM.TOGGLE_IR_KEY);
        event.register(KeyMappingRegistryMM.OPEN_BACKPACK_KEY);
        event.register(KeyMappingRegistryMM.OPEN_RIG_KEY);
    }
}

