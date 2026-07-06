/*
 * Copyright (C) 2021 Nick Iacullo
 *
 * This file is part of Hourglass.
 *
 * Hourglass is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Hourglass is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Hourglass.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.lavabucket.hourglass;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import net.lavabucket.hourglass.command.HourglassCommand;
import net.lavabucket.hourglass.config.HourglassConfig;
import net.lavabucket.hourglass.message.HourglassMessages;
import net.lavabucket.hourglass.registry.TimeEffects;
import net.lavabucket.hourglass.time.TimeServiceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

/** This class contains the mod entry point, as well as some constants related to the mod itself. */
@Mod(Hourglass.MOD_ID)
public class Hourglass {

    /** Mod identifier. The value here should match an entry in the META-INF/neoforge.mods.toml file. */
    public static final String MOD_ID = "hourglass";
    /** Log4j marker for Hourglass logs. */
    public static final Marker MARKER = MarkerManager.getMarker(MOD_ID);

    /** Mod entry point. */
    public Hourglass(IEventBus modBus, ModContainer modContainer) {
        final IEventBus forgeBus = NeoForge.EVENT_BUS;

        HourglassConfig.register(modContainer);

        modBus.register(TimeEffects.class);
        TimeEffects.register(modBus);

        forgeBus.register(TimeServiceManager.class);
        forgeBus.register(HourglassMessages.class);
        forgeBus.register(HourglassCommand.class);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            new HourglassClient(modBus, modContainer);
        }
    }

}
