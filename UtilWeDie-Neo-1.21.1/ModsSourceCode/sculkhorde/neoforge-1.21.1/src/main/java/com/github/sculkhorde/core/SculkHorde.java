package com.github.sculkhorde.core;

import com.github.sculkhorde.common.loot.ModLootModifier;
import com.github.sculkhorde.common.pools.PoolBlocks;
import com.github.sculkhorde.misc.StatisticsData;
import com.github.sculkhorde.misc.contributions.ContributionHandler;
import com.github.sculkhorde.systems.*;
import com.github.sculkhorde.systems.chunk_cursor_system.ChunkInfestationSystem;
import com.github.sculkhorde.systems.cursor_system.CursorSystem;
import com.github.sculkhorde.systems.debugger_system.DebuggerSystem;
import com.github.sculkhorde.systems.event_system.EventSystem;
import com.github.sculkhorde.systems.event_system.events.HitSquadEvent.HitSquadDispatcherSystem;
import com.github.sculkhorde.systems.gravemind_system.Gravemind;
import com.github.sculkhorde.systems.gravemind_system.entity_factory.EntityFactory;
import com.github.sculkhorde.systems.path_builder_system.PathBuilderSystem;
import com.github.sculkhorde.systems.squad_system.SquadSystem;
import com.github.sculkhorde.util.ChunkLoading.BlockEntityChunkLoaderHelper;
import com.github.sculkhorde.util.ChunkLoading.EntityChunkLoaderHelper;
import com.github.sculkhorde.util.DeathAreaInvestigator;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.slf4j.Logger;
import software.bernie.geckolib.GeckoLib;
//HOW TO EXPORT MOD: https://www.youtube.com/watch?v=x3wKsiQ37Wc

//The @Mod tag is here to let the compiler know that this is our main mod class
//It takes in our mod id so it knows what mod it is loading.
@Mod(SculkHorde.MOD_ID)
public class SculkHorde {

    //Here I've created a variable of our mod id so we can use it throughout our project
    public static final String MOD_ID = "sculkhorde";
    //The file name in the world data folder.
    public static final String SAVE_DATA_ID = SculkHorde.MOD_ID + "_gravemind_memory";
    //The Creative Tab that all the items appear in
    private static boolean DEBUG_MODE = false;
    public static EntityFactory entityFactory = new EntityFactory();
    public static Gravemind gravemind;
    public static DebugSlimeSystem debugSlimeSystem;
    public static final Logger LOGGER = LogUtils.getLogger();
    public static PoolBlocks randomSculkFlora;
    public static DeathAreaInvestigator deathAreaInvestigator;
    public static EventSystem eventSystem;
    public static CursorSystem cursorSystem = new CursorSystem();
    public static SculkNodesSystem sculkNodesSystem;
    public static StatisticsData statisticsData;
    public static BlockEntityChunkLoaderHelper blockEntityChunkLoaderHelper;
    public static EntityChunkLoaderHelper entityChunkLoaderHelper = new EntityChunkLoaderHelper();
    public static final ContributionHandler contributionHandler = new ContributionHandler();
    public static final SculkPopulationSystem populationHandler = new SculkPopulationSystem();
    public static final HitSquadDispatcherSystem hitSquadDispatcherSystem = new HitSquadDispatcherSystem();
    public static BeeNestActivitySystem beeNestActivitySystem;
    public static SquadSystem squadSystem = new SquadSystem();

    public static AutoPerformanceSystem autoPerformanceSystem;
    public static ChunkInfestationSystem chunkInfestationSystem;
    public static PathBuilderSystem pathBuilderSystem;
    public static DebuggerSystem debuggerSystem = new DebuggerSystem();

    public static AmbientSFXSystem ambientSFXSystem = new AmbientSFXSystem();

    //This is the instance of our class, and we register it to the ModEventBus (which I have stored in a variable).
    public SculkHorde(IEventBus bus, ModContainer modContainer)
    {
        bus.register(this);

        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, ModConfig.SERVER_SPEC, MOD_ID + "_config.toml");

        ModItems.ITEMS.register(bus); //Load Items
        ModBlockEntities.register(bus); //Load Tile Entities
        ModBlocks.BLOCKS.register(bus); //Load Blocks
        ModEntities.register(bus); //Load Entities
        // NeoForge 严格校验: bus.register(类) 要求该类有 @SubscribeEvent 方法。
        // ModEntities 仅持 DeferredRegister(已由上一行注册), 无事件处理器; 实体属性在 util/ModEventSubscriber
        // 的 entityAttributes(EntityAttributeCreationEvent) 注册。原 Forge 的 bus.register(ModEntities.class) 冗余, 删除。
        ModStructures.STRUCTURES.register(bus); //Load Structures
        ModStructures.STRUCTURE_PIECES.register(bus); //Load Structure Pieces
        ModStructureProcessors.PROCESSORS.register(bus); //Load Processors
        ModCommands.init();
        ModPotions.register(bus); //Load Potions
        ModMenuTypes.register(bus); //Load Menus
        ModMobEffects.EFFECTS.register(bus); //Load Effects
        ModParticles.PARTICLE_TYPES.register(bus); //Load Particles
        ModSounds.SOUND_EVENTS.register(bus); //Load Sounds
        ModCreativeModeTab.TABS.register(bus); //Load Creative Tabs
        ModRecipes.register(bus); //Load Recipes
        ModLootModifier.register(bus);
        ModTriggers.register(bus); //Load Advancement Triggers (1.21: trigger_type 注册表, 须在注册阶段, 不能在 common setup)
    }

    // 1.21 NeoForge capability system: block entity item handlers are now exposed via RegisterCapabilitiesEvent
    // instead of an overridden getCapability(...) on the BlockEntity.
    @SubscribeEvent
    public void registerCapabilities(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.SOUL_HARVESTER_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getItemHandler());
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }

    public static void setDebugMode(boolean debugMode) {
        DEBUG_MODE = debugMode;
    }

}