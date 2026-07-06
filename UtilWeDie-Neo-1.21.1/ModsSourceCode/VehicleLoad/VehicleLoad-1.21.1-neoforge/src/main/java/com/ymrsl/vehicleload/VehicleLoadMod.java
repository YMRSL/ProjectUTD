package com.ymrsl.vehicleload;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(VehicleLoadMod.MOD_ID)
public class VehicleLoadMod {
    public static final String MOD_ID = "vehicleload";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VehicleLoadMod(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, VehicleLoadConfig.SPEC);
        AutoSeatHandler handler = new AutoSeatHandler();
        NeoForge.EVENT_BUS.register(handler);
        NeoForge.EVENT_BUS.register(new SableSeatLockManager());
        NeoForge.EVENT_BUS.register(new ReplayVehicleCalmer());
        NeoForge.EVENT_BUS.register(new CrowbarInteractHandler());
        NeoForge.EVENT_BUS.register(new VehicleLoadCommands(handler));
    }
}
