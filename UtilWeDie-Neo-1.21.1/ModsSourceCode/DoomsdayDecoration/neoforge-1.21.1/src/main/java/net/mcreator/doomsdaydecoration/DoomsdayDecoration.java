package net.mcreator.doomsdaydecoration;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Doomsday Decoration - NeoForge 1.21.1 data-driven port.
 * All block/item/tab registration is driven by
 * {@code doomsday_decoration_manifest.json} on the classpath, parsed by
 * {@link net.mcreator.doomsdaydecoration.init.ModRegistry}.
 */
@Mod(DoomsdayDecoration.MODID)
public class DoomsdayDecoration {
    public static final String MODID = "doomsday_decoration";

    public DoomsdayDecoration(IEventBus modBus, ModContainer container) {
        net.mcreator.doomsdaydecoration.init.ModRegistry.init(modBus);
        // Container/loot layer (BlockEntityType + MenuType registers).
        net.mcreator.doomsdaydecoration.functionality.ModFunctionality.init(modBus);
        // Inject lootable blocks into the BE type's validBlocks (mod-bus, both sides).
        // Mandatory in 1.21.1: an empty validBlocks set makes the BlockEntity ctor throw.
        modBus.addListener(
                net.mcreator.doomsdaydecoration.init.ModRegistry::registerLootableBlocks);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::onClientSetup);
            modBus.addListener(this::onRegisterMenuScreens);
        }
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(net.mcreator.doomsdaydecoration.init.ModRegistry::applyCutoutRenderLayers);
    }

    private void onRegisterMenuScreens(
            final net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(
                net.mcreator.doomsdaydecoration.functionality.ModFunctionality.DOOMSDAY_MENU.get(),
                net.mcreator.doomsdaydecoration.functionality.DoomsdayScreen::new);
    }
}
