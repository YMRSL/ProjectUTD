package net.tkg.ModernMayhem;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tkg.ModernMayhem.client.compat.ar.ARCompat;
import net.tkg.ModernMayhem.client.compat.oculus.OculusCompat;
import net.tkg.ModernMayhem.client.config.ClientConfig;
import net.tkg.ModernMayhem.client.event.ItemInteractionEvent;
import net.tkg.ModernMayhem.client.event.RenderNVGFirstPerson;
import net.tkg.ModernMayhem.client.registry.ClientItemRegistryMM;
import net.tkg.ModernMayhem.server.config.ArmorConfig;
import net.tkg.ModernMayhem.server.config.CommonConfig;
import net.tkg.ModernMayhem.server.config.ServerConfig;
import net.tkg.ModernMayhem.server.registry.AttributesRegistryMM;
import net.tkg.ModernMayhem.server.registry.BlockEntityRegistryMM;
import net.tkg.ModernMayhem.server.registry.BlockRegistryMM;
import net.tkg.ModernMayhem.server.registry.CreativeTabsRegistryMM;
import net.tkg.ModernMayhem.server.registry.CuriosRendererRegistryMM;
import net.tkg.ModernMayhem.server.registry.DataComponentRegistryMM;
import net.tkg.ModernMayhem.server.registry.EntityRegistryMM;
import net.tkg.ModernMayhem.server.registry.GUIRegistryMM;
import net.tkg.ModernMayhem.server.registry.ItemRegistryMM;
import net.tkg.ModernMayhem.server.registry.PacketsRegistryMM;
import net.tkg.ModernMayhem.server.registry.ScreenRegistryMM;
import net.tkg.ModernMayhem.server.registry.SoundRegistryMM;
import org.slf4j.Logger;

@Mod(value="mm")
public class ModernMayhemMod {
    public static final String ID = "mm";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static boolean isGameReady = false;
    private static ModContainer modContainer = null;

    public ModernMayhemMod(IEventBus modEventBus, ModContainer container) {
        modContainer = container;
        ItemRegistryMM.init(modEventBus);
        BlockRegistryMM.init(modEventBus);
        BlockEntityRegistryMM.init(modEventBus);
        DataComponentRegistryMM.init(modEventBus);
        PacketsRegistryMM.init(modEventBus);
        SoundRegistryMM.init(modEventBus);
        CreativeTabsRegistryMM.init(modEventBus);
        GUIRegistryMM.init(modEventBus);
        EntityRegistryMM.init(modEventBus);
        AttributesRegistryMM.init(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::onGameReady);
        if (FMLEnvironment.dist.isClient()) {
            ClientItemRegistryMM.init(modEventBus);
            ItemInteractionEvent.register();
            modEventBus.addListener(ScreenRegistryMM::register);
        }
        container.registerConfig(ModConfig.Type.COMMON, (IConfigSpec)CommonConfig.CONFIG, "modern-mayhem-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)ClientConfig.CONFIG, "modern-mayhem-client.toml");
        container.registerConfig(ModConfig.Type.SERVER, (IConfigSpec)ServerConfig.CONFIG, "modern-mayhem-server.toml");
        ArmorConfig.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    private void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info("HELLO FROM CLIENT SETUP");
        if (ModList.get().isLoaded("iris")) {
            OculusCompat.initCompat();
            LOGGER.info("[mm] Oculus compatibility layer initialized");
        }
        if (ModList.get().isLoaded("acceleratedrendering")) {
            ARCompat.init();
            LOGGER.info("[mm] Accelerated Rendering compatibility layer initialized");
        }
        if (ModList.get().isLoaded("sodiumdynamiclights")) {
            net.tkg.ModernMayhem.client.compat.sddl.IrBlockSddlCompat.register();
            LOGGER.info("[mm] SodiumDynamicLights IR block handler registered");
        }
        CuriosRendererRegistryMM.register();
        RenderNVGFirstPerson.initialiseFirstPersonRenderer();
    }

    private void onGameReady(FMLLoadCompleteEvent event) {
        isGameReady = true;
    }

    public static boolean isGameReady() {
        return isGameReady;
    }

    public static ModContainer getModContainer() {
        return modContainer;
    }
}

