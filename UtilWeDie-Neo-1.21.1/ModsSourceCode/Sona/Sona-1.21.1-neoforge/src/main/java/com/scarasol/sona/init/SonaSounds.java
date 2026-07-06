package com.scarasol.sona.init;

import com.scarasol.sona.SonaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class SonaSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, SonaMod.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> TINNITUS = REGISTRY.register("tinnitus", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "tinnitus")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE = REGISTRY.register("crate", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "crate")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SLIDER_ZIPPER_BAG = REGISTRY.register("slider_zipper_bag", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "slider_zipper_bag")));
    public static final DeferredHolder<SoundEvent, SoundEvent> LACERATION_LOOP = REGISTRY.register("laceration_loop", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "laceration_loop")));

}
