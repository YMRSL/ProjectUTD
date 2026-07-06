package net.mcreator.survivalinstinct.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SurvivalInstinctModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, (String)"survival_instinct");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEAR_TRAP_CLOSE = REGISTRY.register("bear_trap_close", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "bear_trap_close")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW_SWING = REGISTRY.register("chainsaw_swing", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "chainsaw_swing")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_GUITAR_SWING = REGISTRY.register("electric_guitar_swing", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "electric_guitar_swing")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_GUITAR_SMASH = REGISTRY.register("electric_guitar_smash", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "electric_guitar_smash")));
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE_SLAM = REGISTRY.register("pipe_slam", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "pipe_slam")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TRHOW_MOLOTOV = REGISTRY.register("trhow_molotov", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "trhow_molotov")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOTE_MOLOTOV = REGISTRY.register("explote_molotov", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "explote_molotov")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CIRCULARSAW_SWING = REGISTRY.register("circularsaw_swing", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "circularsaw_swing")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CROWBAR_SWING = REGISTRY.register("crowbar_swing", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "crowbar_swing")));
    public static final DeferredHolder<SoundEvent, SoundEvent> CROWBAR_SLAM = REGISTRY.register("crowbar_slam", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "crowbar_slam")));
    public static final DeferredHolder<SoundEvent, SoundEvent> NAILGUN_SHOOT = REGISTRY.register("nailgun_shoot", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "nailgun_shoot")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXO_DASH_01 = REGISTRY.register("exo_dash_01", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "exo_dash_01")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXO_DASH_02 = REGISTRY.register("exo_dash_02", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "exo_dash_02")));
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_FIST_01 = REGISTRY.register("electric_fist_01", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "electric_fist_01")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCUFF_SOUND = REGISTRY.register("handcuff_sound", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "handcuff_sound")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HANDCUFF_OFF_SOUND = REGISTRY.register("handcuff_off_sound", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath("survival_instinct", "handcuff_off_sound")));
}

