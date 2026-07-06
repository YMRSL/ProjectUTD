package net.mcreator.survivalinstinct;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModBlockEntities;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModBlocks;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModEntities;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModMenus;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModMobEffects;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModSounds;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(value = "survival_instinct")
public class SurvivalInstinctMod {
    public static final Logger LOGGER = LogManager.getLogger(SurvivalInstinctMod.class);
    public static final String MODID = "survival_instinct";
    private static final Collection<AbstractMap.SimpleEntry<Runnable, Integer>> workQueue = new ConcurrentLinkedQueue<AbstractMap.SimpleEntry<Runnable, Integer>>();

    public SurvivalInstinctMod(IEventBus modEventBus) {
        SurvivalInstinctModSounds.REGISTRY.register(modEventBus);
        SurvivalInstinctModBlocks.REGISTRY.register(modEventBus);
        SurvivalInstinctModBlockEntities.REGISTRY.register(modEventBus);
        SurvivalInstinctModItems.REGISTRY.register(modEventBus);
        SurvivalInstinctModEntities.REGISTRY.register(modEventBus);
        SurvivalInstinctModTabs.REGISTRY.register(modEventBus);
        SurvivalInstinctModMobEffects.REGISTRY.register(modEventBus);
        SurvivalInstinctModMenus.REGISTRY.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
    }

    public static void queueServerWork(int tick, Runnable action) {
        workQueue.add(new AbstractMap.SimpleEntry<Runnable, Integer>(action, tick));
    }

    @SubscribeEvent
    public void tick(ServerTickEvent.Post event) {
        ArrayList<AbstractMap.SimpleEntry<Runnable, Integer>> actions = new ArrayList<>();
        workQueue.forEach(work -> {
            work.setValue(work.getValue() - 1);
            if (work.getValue() == 0) {
                actions.add(work);
            }
        });
        actions.forEach(e -> e.getKey().run());
        workQueue.removeAll(actions);
    }
}
