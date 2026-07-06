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

package net.lavabucket.hourglass.registry;

import java.util.function.Supplier;

import net.lavabucket.hourglass.Hourglass;
import net.lavabucket.hourglass.time.effects.BlockEntityTimeEffect;
import net.lavabucket.hourglass.time.effects.HungerTimeEffect;
import net.lavabucket.hourglass.time.effects.PotionTimeEffect;
import net.lavabucket.hourglass.time.effects.RandomTickSleepEffect;
import net.lavabucket.hourglass.time.effects.TimeEffect;
import net.lavabucket.hourglass.time.effects.WeatherSleepEffect;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * This class registers all of the first-party time effects that come with Hourglass.
 */
public class TimeEffects {

    /** The resource key for the time effect registry. */
    public static final ResourceKey<Registry<TimeEffect>> KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Hourglass.MOD_ID, "time_effect"));

    /** Registry for time effects. See {@link TimeEffect} for details on time effects. */
    public static final Registry<TimeEffect> REGISTRY = new RegistryBuilder<>(KEY).create();

    private static final DeferredRegister<TimeEffect> DEFERRED_REGISTRY = DeferredRegister.create(KEY, Hourglass.MOD_ID);

    public static final DeferredHolder<TimeEffect, WeatherSleepEffect> WEATHER_EFFECT =
            DEFERRED_REGISTRY.register("weather", WeatherSleepEffect::new);
    public static final DeferredHolder<TimeEffect, RandomTickSleepEffect> RANDOM_TICK_EFFECT =
            DEFERRED_REGISTRY.register("random_tick", RandomTickSleepEffect::new);
    public static final DeferredHolder<TimeEffect, PotionTimeEffect> POTION_EFFECT =
            DEFERRED_REGISTRY.register("potion", PotionTimeEffect::new);
    public static final DeferredHolder<TimeEffect, HungerTimeEffect> HUNGER_EFFECT =
            DEFERRED_REGISTRY.register("hunger", HungerTimeEffect::new);
    public static final DeferredHolder<TimeEffect, BlockEntityTimeEffect> BLOCK_ENTITY_EFFECT =
            DEFERRED_REGISTRY.register("block_entity", BlockEntityTimeEffect::new);

    /** Convenience supplier for the time effect registry, for callers that expect a supplier. */
    public static final Supplier<Registry<TimeEffect>> REGISTRY_SUPPLIER = () -> REGISTRY;

    /**
     * Registers the custom time effect registry to the game registry.
     * @param event  the event, provided by the mod event bus
     */
    @SubscribeEvent
    public static void onNewRegistry(NewRegistryEvent event) {
        event.register(REGISTRY);
    }

    /**
     * Registers the deferred register to the given mod event bus.
     * @param modBus  the mod event bus
     */
    public static void register(net.neoforged.bus.api.IEventBus modBus) {
        DEFERRED_REGISTRY.register(modBus);
    }

    // Private constructor to prohibit instantiation.
    private TimeEffects() {}

}
