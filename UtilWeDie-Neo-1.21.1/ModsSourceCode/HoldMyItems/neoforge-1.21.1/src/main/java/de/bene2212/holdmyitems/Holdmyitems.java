package de.bene2212.holdmyitems;

import com.mojang.blaze3d.platform.InputConstants;
import de.bene2212.holdmyitems.config.HoldMyItemsClientConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod(value = "holdmyitems")
public class Holdmyitems {
    private static double prevTime = 0.0;
    public static double deltaTime = 0.0;
    public static final String MODID = "holdmyitems";
    public static final Logger LOGGER = LogManager.getLogger("holdmyitems");
    public static KeyMapping CUSTOM_KEY;
    public static final boolean DEBUG = true;

    private static void updateDeltatime() {
        double currentTime = GLFW.glfwGetTime();
        deltaTime = currentTime - prevTime;
        prevTime = currentTime;
        deltaTime = Minecraft.getInstance().isPaused() ? 0.0 : Math.min(0.05, deltaTime);
    }

    public Holdmyitems(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, HoldMyItemsClientConfig.CLIENT_CONFIG);
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRenderFrame(RenderFrameEvent.Pre event) {
        Holdmyitems.updateDeltatime();
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        CUSTOM_KEY = new KeyMapping("Inspect Key", InputConstants.Type.KEYSYM, 74, "HoldMyItems");
        event.register(CUSTOM_KEY);
    }
}
