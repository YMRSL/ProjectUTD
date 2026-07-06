package com.codinglitch.simpleradio;

import com.codinglitch.simpleradio.core.FabricLoader;
import com.codinglitch.simpleradio.core.registry.SimpleRadioBlocks;
import com.codinglitch.simpleradio.core.registry.blocks.RadiosmitherBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SimpleRadio implements ModInitializer {
    
    @Override
    public void onInitialize() {
        CommonSimpleRadio.initialize();

        FabricLoader.load();
    }
}
