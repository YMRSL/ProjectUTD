package com.ymrsl.utdassetmanager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(UTDAssetManagerMod.MOD_ID)
public final class UTDAssetManagerMod {
    public static final String MOD_ID = "utd_asset_manager";

    public UTDAssetManagerMod(IEventBus modEventBus, ModContainer modContainer) {
        // Client-only behavior is registered through dist-gated event subscribers.
    }
}
