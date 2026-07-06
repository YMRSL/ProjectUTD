package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.block.*;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;

public class ZombieKitBlocks {
    public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(ZombieKitMod.MODID);

    public static final DeferredBlock<Block> TRAP_COVER = REGISTRY.registerBlock("trap_cover", props -> new TrapCoverBlock(props.sound(SoundType.AZALEA_LEAVES).strength(1f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> BARBED_WIRE = REGISTRY.registerBlock("barbed_wire", props -> new BarbedWireBlock(props.sound(SoundType.WOOD).strength(1f, 10f).noCollission().jumpFactor(0.5f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false), 0), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> BARBED_WIRE_BROKEN = REGISTRY.registerBlock("barbed_wire_broken", props -> new BarbedWireBlock(props.sound(SoundType.WOOD).strength(1f, 10f).noCollission().jumpFactor(0.5f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false), 1), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> BARBED_WIRE_EXTREMELY_BROKEN = REGISTRY.registerBlock("barbed_wire_extremly_broken", props -> new BarbedWireBlock(props.sound(SoundType.WOOD).strength(1f, 10f).noCollission().jumpFactor(0.5f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false), 2), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> LANDMINE = REGISTRY.registerBlock("landmine", props -> new LandmineBlock(props.sound(SoundType.METAL).strength(1f, 10f).requiresCorrectToolForDrops().noCollission().noOcclusion().isRedstoneConductor((bs, br, bp) -> false).dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ), new ArrayList<>()), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> CHEMICAL_LANDMINE = REGISTRY.registerBlock("chemical_landmine", props -> new LandmineBlock(props.sound(SoundType.METAL).strength(1f, 10f).requiresCorrectToolForDrops().noCollission().noOcclusion().isRedstoneConductor((bs, br, bp) -> false).dynamicShape().offsetType(BlockBehaviour.OffsetType.XZ),
            Arrays.asList(new Tuple<>(new Tuple<>("sona:insane", 400), 0))), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> CHARGER = REGISTRY.registerBlock("charger", props -> new ChargerBlock(props.sound(SoundType.STONE).lightLevel(ChargerBlock.getLightLevel(8)).strength(1f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> ULTRA_WIDEBAND_RADAR = REGISTRY.registerBlock("ultra_wideband_radar", props -> new UltraWidebandRadarBlock(props.sound(SoundType.METAL).strength(1f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> INJECTOR = REGISTRY.registerBlock("injector", props -> new InjectorBlock(props.sound(SoundType.STONE).strength(1f, 3.5f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> SHORTWAVE_RADIO = REGISTRY.registerBlock("shortwave_radio", props -> new ShortwaveRadioBlock(props.sound(SoundType.METAL).strength(1f, 3.6f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> GAS_TANK = REGISTRY.registerBlock("gas_tank", props -> new GasTankBlock(props.sound(SoundType.METAL).strength(1f, 3.6f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> FLARES_LIGHT = REGISTRY.registerBlock("flares_light", props -> new FlaresLightBlock(props.sound(SoundType.STONE).strength(-1, 3600000).lightLevel(s -> 15).noCollission().noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> SALTPETER_CAULDRON = REGISTRY.registerBlock("saltpeter_cauldron", props -> new SaltpeterCauldronBlock(props.sound(SoundType.STONE).randomTicks().strength(2f, 2f).requiresCorrectToolForDrops(), CauldronInteraction.WATER), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> SPREAD_LIGHT = REGISTRY.registerBlock("spread_light", props -> new SpreadLight(props.sound(SoundType.STONE).strength(-1, 3600000).lightLevel(s -> 15).noCollission().noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> VACUUM_PACKAGING_MACHINE = REGISTRY.registerBlock("vacuum_packaging_machine", props -> new VacuumPackagingMachineBlock(props.sound(SoundType.METAL).strength(1f, 3.6f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());
    public static final DeferredBlock<Block> MORTAR_RACK = REGISTRY.registerBlock("mortar_rack", props -> new MortarRackBlock(props.sound(SoundType.METAL).strength(1f, 3.6f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false)), BlockBehaviour.Properties.of());

    @EventBusSubscriber(modid = "zombiekit", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSideHandler {

        @SubscribeEvent
        public static void blockColorLoad(RegisterColorHandlersEvent.Block event) {
            TrapCoverBlock.blockColorLoad(event);
        }

        @SubscribeEvent
        public static void itemColorLoad(RegisterColorHandlersEvent.Item event) {
            TrapCoverBlock.itemColorLoad(event);
        }

    }

}
