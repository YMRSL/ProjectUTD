package com.utd.crustychunks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CrustyChunksMod.MODID)
public class CrustyChunksMod {
    public static final String MODID = "crusty_chunks";

    public CrustyChunksMod(IEventBus modEventBus) {
        CrustyChunksItems.ITEMS.register(modEventBus);
        CrustyChunksTabs.TABS.register(modEventBus);
    }
}
