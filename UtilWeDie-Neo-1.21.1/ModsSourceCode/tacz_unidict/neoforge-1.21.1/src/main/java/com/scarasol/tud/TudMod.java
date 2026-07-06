package com.scarasol.tud;

import com.mojang.logging.LogUtils;
import com.scarasol.tud.configuration.CommonConfig;
import com.scarasol.tud.init.TudEntities;
import com.scarasol.tud.init.TudModData;
import com.scarasol.tud.manager.EntitySpawnManager;
import com.scarasol.tud.network.NetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

/**
 * @author Scarasol
 */
@Mod(TudMod.MODID)
public class TudMod
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "tacz_unidict";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public TudMod(IEventBus modEventBus, ModContainer modContainer)
    {
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "tacz-unidict-common.toml");
        modEventBus.addListener(this::commonSetup);
        TudEntities.REGISTRY.register(modEventBus);
        modEventBus.addListener(NetworkHandler::register);
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        TudModData.registerType();
        TudModData.registerAmmoGetter();
        TudModData.registerModifierGetter();
        EntitySpawnManager.registerEntityGetter();
    }


}
