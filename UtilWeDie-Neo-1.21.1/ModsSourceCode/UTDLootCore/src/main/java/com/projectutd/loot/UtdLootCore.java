package com.projectutd.loot;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(UtdLootCore.MOD_ID)
public final class UtdLootCore {
    public static final String MOD_ID = "utd_loot_core";
    public static final Logger LOGGER = LogManager.getLogger("UTD Loot Core");

    private final LootCatalog catalog;

    public UtdLootCore(IEventBus modBus) {
        this.catalog = LootCatalog.loadBundled();
        NeoForge.EVENT_BUS.register(new LootPityHandler(catalog));
        NeoForge.EVENT_BUS.register(new UtdLootCommands(catalog));
        LOGGER.info(
            "Loaded UTD Loot Core catalog: {} registry rows ({} enabled), {} containers, {} templates",
            catalog.registryCount(),
            catalog.enabledCount(),
            catalog.containerCount(),
            catalog.templateCount()
        );
    }
}
