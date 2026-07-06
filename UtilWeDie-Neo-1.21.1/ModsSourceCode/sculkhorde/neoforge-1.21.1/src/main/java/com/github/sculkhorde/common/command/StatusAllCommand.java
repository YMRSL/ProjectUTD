package com.github.sculkhorde.common.command;

import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.core.SculkHorde;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StatusAllCommand implements Command<CommandSourceStack> {

    public static ArgumentBuilder<CommandSourceStack, ?> register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        return Commands.literal("statusall")
                .executes(new StatusAllCommand());

    }

    @Override
    public int run(CommandContext<CommandSourceStack> context)
    {
        context.getSource().sendSuccess(()->Component.literal(
                "Horde State: " + ModSavedData.getSaveData().getHordeState().toString()
                        + "\n"
                        + "Gravemind State: " + SculkHorde.gravemind.getEvolutionState().toString()
                        + "\n"
                        + "Sculk Mass Accumulated: " + ModSavedData.getSaveData().getSculkAccumulatedMass()
                        + "\n"
                        + "Sculk Nodes Present: " + ModSavedData.getSaveData().getNodeEntries().size()
                        + "\n"
                        + "Nests Count: " + ModSavedData.getSaveData().getBeeNestEntries().size()
                        + "\n"
                        + "Mob Types Considered Hostile Count: " + ModSavedData.getSaveData().getHostileEntries().size()
                        + "\n"
                        + "Death Area Reports Count: " + ModSavedData.getSaveData().getDeathAreaEntries().size()
                        + "\n"
                        + "Areas of Interest Count: " + ModSavedData.getSaveData().getAreasOfInterestEntries().size()
                        + "\n"
                        + "No Raid Zone Entries Count: " + ModSavedData.getSaveData().getNoRaidZoneEntries().size()
                        + "\n"
                        + "Entity Chunk Load Requests: " + SculkHorde.entityChunkLoaderHelper.getEntityChunkLoadRequests().size()
                        + "\n"
                        + "BlockEntity Chunk load Requests: " + SculkHorde.blockEntityChunkLoaderHelper.getBlockChunkLoadRequests().size()
                        + "\n"
                        + "Events in Queue: " + SculkHorde.eventSystem.getEvents().size()
                        + "\n"
                        + "Performance Mode: " + SculkHorde.autoPerformanceSystem.getPerformanceMode().toString()
                        + "\n"
                        + "Performance Mode Cursor Threshold?: " + SculkHorde.autoPerformanceSystem.getInfectorCursorPopulationThreshold()
                        + "\n"
                        + "Performance Mode Cursors to Tick Per Tick: " + SculkHorde.autoPerformanceSystem.getCursorsToTickPerTick()
                        + "\n"
                        + "Performance Mode Delay Between Cursor Ticks: " + SculkHorde.autoPerformanceSystem.getDelayBetweenCursorTicks()
                        + "\n"
                        + "Performance Mode Max Nodes Active: " + SculkHorde.autoPerformanceSystem.getMaxNodesActive()
                        + "\n"
                        + "Cursors being Ticked: " + SculkHorde.cursorSystem.getSizeOfCursorList() + " / " + SculkHorde.autoPerformanceSystem.getMaxInfectorCursorPopulation()
                        + "\n"
                        + "Virtual Cursors being Ticked: " + SculkHorde.cursorSystem.getSizeOfVirtualCursorList() + " / " + SculkHorde.autoPerformanceSystem.getMaxInfectorCursorPopulation()
                        + "\n"
                        + "[\u611f\u67d3\u8bca\u65ad] \u6027\u80fd\u8c41\u514d\u865a\u62df\u6e38\u6807(\u8282\u70b9/\u5927\u8111/3000\u7528): " + SculkHorde.cursorSystem.getSizeOfPerformanceExemptVirtualCursorList()
                        + "\n"
                        + "[\u611f\u67d3\u8bca\u65ad] \u5b9e\u6d4bTPS: " + SculkHorde.autoPerformanceSystem.getTPS() + " (>=15\u5347High\u4e0d\u8282\u6d41)"
                        + "\n"
                        + "[\u611f\u67d3\u8bca\u65ad] \u611f\u67d3\u901f\u5ea6\u500d\u7387(infection_speed_multiplier): " + ModConfig.SERVER.infection_speed_multiplier.get()
                        + "\n"
                        + "[\u611f\u67d3\u8bca\u65ad] \u65b9\u5757\u611f\u67d3\u603b\u5f00\u5173(block_infestation_enabled): " + ModConfig.SERVER.block_infestation_enabled.get()
                        + "\n"
                        + "[\u611f\u67d3\u8bca\u65ad] \u81ea\u52a8\u6027\u80fd\u7cfb\u7edf\u662f\u5426\u7981\u7528(disable_auto_performance_system): " + ModConfig.SERVER.disable_auto_performance_system.get()
                        + "\n"
                        + "Sculk Unit Population: " + SculkHorde.populationHandler.getPopulationSize() + " / " + SculkHorde.populationHandler.getMaxPopulation()
                        + "\n"
                        + "Scouting Phantom Population: " + SculkHorde.populationHandler.getScoutingPhantomsPopulation() + " / " + SculkHorde.populationHandler.getMaxScoutingPhantomsPopulation()
                        + "\n"
                        + "Player Profiles: " + ModSavedData.getSaveData().getPlayerProfileEntries().size()
                        + "\n"
                        + "Mob Profiles: " + ModSavedData.getSaveData().getMobProfileEntries().size()
                ), false);
        return 0;
    }

}
