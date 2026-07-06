package net.tkg.ModernMayhem.client.outline;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.tkg.ModernMayhem.client.outline.OutlineThermal;
import net.tkg.ModernMayhem.client.outline.render.OutlineRenderer;

@EventBusSubscriber(modid="mm", bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class OutlineSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            OutlineRenderer.init();
            NeoForge.EVENT_BUS.register(OutlineRenderer.class);
            OutlineThermal.setupOutlines();
        });
    }

    @EventBusSubscriber(modid="mm", value={Dist.CLIENT})
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc;
            if ((mc = Minecraft.getInstance()).getWindow() != null) {
                int width = mc.getWindow().getWidth();
                int height = mc.getWindow().getHeight();
                OutlineRenderer.resize(width, height);
            }
        }
    }
}

