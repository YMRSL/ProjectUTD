package com.ymrsl.utdassetmanager;

import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRepository;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(UTDAssetManagerMod.MOD_ID)
public final class UTDAssetManagerMod {
    public static final String MOD_ID = "utd_asset_manager";

    public UTDAssetManagerMod(IEventBus modEventBus, ModContainer modContainer) {
        // Client UI and the common transform runner are registered through event subscribers.
        // This creates only the safe empty transform document when no user config exists.
        BlockTransformRepository.get().snapshot();
    }
}
