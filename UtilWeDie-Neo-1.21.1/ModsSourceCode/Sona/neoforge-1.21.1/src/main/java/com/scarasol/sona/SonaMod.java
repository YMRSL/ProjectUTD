package com.scarasol.sona;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(SonaMod.MODID)
public class SonaMod {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "sona";

    public SonaMod(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Sona Survival 101 (NeoForge 1.21.1 skeleton) loading");
    }
}
