package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZombieKitSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, ZombieKitMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> acid_spray = REGISTRY.register("acid_spray", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "acid_spray")));
    public static final DeferredHolder<SoundEvent, SoundEvent> turn_on = REGISTRY.register("turn_on", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "turn_on")));
    public static final DeferredHolder<SoundEvent, SoundEvent> radio_static = REGISTRY.register("radio_static", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "radio_static")));
    public static final DeferredHolder<SoundEvent, SoundEvent> radio_response = REGISTRY.register("radio_response", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "radio_response")));
    public static final DeferredHolder<SoundEvent, SoundEvent> radio_static_long = REGISTRY.register("radio_static_long", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "radio_static_long")));
    public static final DeferredHolder<SoundEvent, SoundEvent> flare_gun_fire = REGISTRY.register("flare_gun_fire", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "flare_gun_fire")));
    public static final DeferredHolder<SoundEvent, SoundEvent> konn_gara = REGISTRY.register("konn_gara", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "konn_gara")));
    public static final DeferredHolder<SoundEvent, SoundEvent> drone_idle = REGISTRY.register("drone_idle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "drone_idle")));
    public static final DeferredHolder<SoundEvent, SoundEvent> drone_switch = REGISTRY.register("drone_switch", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "drone_switch")));
    public static final DeferredHolder<SoundEvent, SoundEvent> drone_crush = REGISTRY.register("drone_crush", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "drone_crush")));
    public static final DeferredHolder<SoundEvent, SoundEvent> heavy_machine_gun_fire = REGISTRY.register("heavy_machine_gun_fire", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "heavy_machine_gun_fire")));
    public static final DeferredHolder<SoundEvent, SoundEvent> heavy_machine_gun_overload = REGISTRY.register("heavy_machine_gun_overload", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "heavy_machine_gun_overload")));
    public static final DeferredHolder<SoundEvent, SoundEvent> heavy_machine_gun_trigger = REGISTRY.register("heavy_machine_gun_trigger", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "heavy_machine_gun_trigger")));
    public static final DeferredHolder<SoundEvent, SoundEvent> heavy_machine_gun_deploy = REGISTRY.register("heavy_machine_gun_deploy", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "heavy_machine_gun_deploy")));
    public static final DeferredHolder<SoundEvent, SoundEvent> chainsaw_idle = REGISTRY.register("chainsaw_idle", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "chainsaw_idle")));
    public static final DeferredHolder<SoundEvent, SoundEvent> chainsaw_start_failed = REGISTRY.register("chainsaw_start_failed", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "chainsaw_start_failed")));
    public static final DeferredHolder<SoundEvent, SoundEvent> chainsaw_attack = REGISTRY.register("chainsaw_attack", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "chainsaw_attack")));
    public static final DeferredHolder<SoundEvent, SoundEvent> exo_fly = REGISTRY.register("exo_fly", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "exo_fly")));
    public static final DeferredHolder<SoundEvent, SoundEvent> reactive_armor_ready = REGISTRY.register("reactive_armor_ready", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "reactive_armor_ready")));
    public static final DeferredHolder<SoundEvent, SoundEvent> reactive_armor_release = REGISTRY.register("reactive_armor_release", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "reactive_armor_release")));
    public static final DeferredHolder<SoundEvent, SoundEvent> mode_switch = REGISTRY.register("mode_switch", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "mode_switch")));
    public static final DeferredHolder<SoundEvent, SoundEvent> radar_activated = REGISTRY.register("radar_activated", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "radar_activated")));
    public static final DeferredHolder<SoundEvent, SoundEvent> flamethrower = REGISTRY.register("flamethrower", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "flamethrower")));
    public static final DeferredHolder<SoundEvent, SoundEvent> flamethrower_reload = REGISTRY.register("flamethrower_reload", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "flamethrower_reload")));
    public static final DeferredHolder<SoundEvent, SoundEvent> flamethrower_reload_empty = REGISTRY.register("flamethrower_reload_empty", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "flamethrower_reload_empty")));
    public static final DeferredHolder<SoundEvent, SoundEvent> mortar_launch = REGISTRY.register("mortar_launch", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "mortar_launch")));


}
