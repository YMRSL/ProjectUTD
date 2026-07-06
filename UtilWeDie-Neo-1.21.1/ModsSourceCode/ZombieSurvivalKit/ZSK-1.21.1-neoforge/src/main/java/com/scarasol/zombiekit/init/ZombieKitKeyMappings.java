package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.network.KeyBindPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "zombiekit", bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class ZombieKitKeyMappings {
    public static final KeyMapping EXO_SNEAK_MODE = new KeyMapping("key.zombiekit.exo_sneak_mode", GLFW.GLFW_KEY_C, "key.categories.zombiekit");
    public static final KeyMapping EXO_COMBAT_MODE = new KeyMapping("key.zombiekit.exo_combat_mode", GLFW.GLFW_KEY_V, "key.categories.zombiekit");
    public static final KeyMapping EXO_REACTIVE_ARMOR = new KeyMapping("key.zombiekit.exo_reactive_armor", GLFW.GLFW_KEY_X, "key.categories.zombiekit");
    public static final KeyMapping EXO_RADAR = new KeyMapping("key.zombiekit.exo_radar", GLFW.GLFW_KEY_Z, "key.categories.zombiekit");
    public static final KeyMapping EXO_FLY_MODE = new KeyMapping("key.zombiekit.exo_fly_mode", GLFW.GLFW_KEY_G, "key.categories.zombiekit");
    public static final KeyMapping MODIFICATION_GUI = new KeyMapping("key.zombiekit.modification_gui", GLFW.GLFW_KEY_Y, "key.categories.zombiekit");
    public static final KeyMapping CHARGING_PARTS = new KeyMapping("key.zombiekit.charging_parts", GLFW.GLFW_KEY_R, "key.categories.zombiekit");
    public static final KeyMapping FLAMETHROWER_RELOAD = new KeyMapping("key.zombiekit.flamethrower_reload", GLFW.GLFW_KEY_R, "key.categories.zombiekit");


    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(EXO_SNEAK_MODE);
        event.register(EXO_REACTIVE_ARMOR);
        event.register(EXO_COMBAT_MODE);
        event.register(EXO_RADAR);
        event.register(EXO_FLY_MODE);
        event.register(MODIFICATION_GUI);
        event.register(CHARGING_PARTS);
        event.register(FLAMETHROWER_RELOAD);
    }

    @EventBusSubscriber(modid = "zombiekit", value = {Dist.CLIENT})
    public static class KeyEventListener {
        @SubscribeEvent
        public static void onKeyInput(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().screen == null) {
                if (MODIFICATION_GUI.consumeClick())
                    PacketDistributor.sendToServer(new KeyBindPacket(6));
                else if (CHARGING_PARTS.consumeClick())
                    PacketDistributor.sendToServer(new KeyBindPacket(7));
                else if (FLAMETHROWER_RELOAD.consumeClick())
                    PacketDistributor.sendToServer(new KeyBindPacket(8));
                else if (Minecraft.getInstance().player != null && ExoArmor.numberOfSuit(Minecraft.getInstance().player) == 4)
                    if (EXO_SNEAK_MODE.consumeClick()) {
                        PacketDistributor.sendToServer(new KeyBindPacket(1));
                    } else if (EXO_COMBAT_MODE.consumeClick()) {
                        PacketDistributor.sendToServer(new KeyBindPacket(2));
                    } else if (EXO_REACTIVE_ARMOR.consumeClick()) {
                        PacketDistributor.sendToServer(new KeyBindPacket(3));
                    } else if (EXO_RADAR.consumeClick())
                        PacketDistributor.sendToServer(new KeyBindPacket(4));
                    else if (EXO_FLY_MODE.consumeClick())
                        PacketDistributor.sendToServer(new KeyBindPacket(5));
            }
        }
    }

}
