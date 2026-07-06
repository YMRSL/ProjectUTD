package net.tejty.just_barricades;

import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.tejty.just_barricades.block.ModBlocks;
import net.tejty.just_barricades.config.JustBarricadesCommonConfig;
import net.tejty.just_barricades.item.ModItems;
import org.slf4j.Logger;

@Mod(JustBarricades.MODID)
public class JustBarricades {
    public static final String MODID = "just_barricades";
    private static final Logger LOGGER = LogUtils.getLogger();

    public JustBarricades() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, JustBarricadesCommonConfig.SPEC, "just_barricades-common.toml");

        MinecraftForge.EVENT_BUS.register(this);
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
