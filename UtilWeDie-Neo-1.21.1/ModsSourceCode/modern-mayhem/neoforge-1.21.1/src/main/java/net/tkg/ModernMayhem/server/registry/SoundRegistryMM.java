package net.tkg.ModernMayhem.server.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SoundRegistryMM {
    public static final DeferredRegister<SoundEvent> MOD_SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, (String)"mm");
    public static DeferredHolder<SoundEvent, SoundEvent> SOUND_NVG_ON = SoundRegistryMM.registerSoundsEvent("sound_nvg_on");
    public static DeferredHolder<SoundEvent, SoundEvent> SOUND_NVG_OFF = SoundRegistryMM.registerSoundsEvent("sound_nvg_off");
    public static DeferredHolder<SoundEvent, SoundEvent> SMALL_CLICK = SoundRegistryMM.registerSoundsEvent("small_click");
    public static DeferredHolder<SoundEvent, SoundEvent> SOUND_NVG_PUT_ON = SoundRegistryMM.registerSoundsEvent("sound_nvg_put_on");
    public static DeferredHolder<SoundEvent, SoundEvent> SOUND_NVG_PUT_OFF = SoundRegistryMM.registerSoundsEvent("sound_nvg_put_off");
    public static DeferredHolder<SoundEvent, SoundEvent> SOUND_VISOR_OPEN = SoundRegistryMM.registerSoundsEvent("sound_visor_open");
    public static DeferredHolder<SoundEvent, SoundEvent> SOUND_VISOR_CLOSE = SoundRegistryMM.registerSoundsEvent("sound_visor_close");

    public static void init(IEventBus eventBus) {
        MOD_SOUNDS.register(eventBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> registerSoundsEvent(String name) {
        return MOD_SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"mm", (String)name)));
    }
}

