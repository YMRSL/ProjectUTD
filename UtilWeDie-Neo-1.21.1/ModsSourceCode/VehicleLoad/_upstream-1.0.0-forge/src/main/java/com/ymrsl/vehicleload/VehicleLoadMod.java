package com.ymrsl.vehicleload;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(VehicleLoadMod.MOD_ID)
public class VehicleLoadMod {
    public static final String MOD_ID = "vehicleload";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VehicleLoadMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VehicleLoadConfig.SPEC);
        AutoSeatHandler handler = new AutoSeatHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        MinecraftForge.EVENT_BUS.register(new CrowbarInteractHandler());
        MinecraftForge.EVENT_BUS.register(new VehicleLoadCommands(handler));
    }
}
