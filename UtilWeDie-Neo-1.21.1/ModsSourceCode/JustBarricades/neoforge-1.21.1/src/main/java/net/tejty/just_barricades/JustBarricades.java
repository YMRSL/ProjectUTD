package net.tejty.just_barricades;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.tejty.just_barricades.block.ModBlocks;
import net.tejty.just_barricades.config.JustBarricadesCommonConfig;
import net.tejty.just_barricades.item.ModItems;

@Mod(JustBarricades.MODID)
public class JustBarricades {
    public static final String MODID = "just_barricades";

    public JustBarricades(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, JustBarricadesCommonConfig.SPEC, "just_barricades-common.toml");

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModBlocks.OAK_BARRICADE.get());
            event.accept(ModBlocks.SPRUCE_BARRICADE.get());
            event.accept(ModBlocks.BIRCH_BARRICADE.get());
            event.accept(ModBlocks.JUNGLE_BARRICADE.get());
            event.accept(ModBlocks.ACACIA_BARRICADE.get());
            event.accept(ModBlocks.DARK_OAK_BARRICADE.get());
            event.accept(ModBlocks.MANGROVE_BARRICADE.get());
            event.accept(ModBlocks.CHERRY_BARRICADE.get());
            event.accept(ModBlocks.BAMBOO_BARRICADE.get());
        }
    }
}
