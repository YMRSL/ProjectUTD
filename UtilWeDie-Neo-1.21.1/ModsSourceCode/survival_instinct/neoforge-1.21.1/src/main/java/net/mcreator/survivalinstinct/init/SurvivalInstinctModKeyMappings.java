package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.SurvivalInstinctMod;
import net.mcreator.survivalinstinct.network.ExoSuitDashMessage;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT}, modid = "survival_instinct")
public class SurvivalInstinctModKeyMappings {
    public static final KeyMapping EXO_SUIT_DASH = new KeyMapping("key.survival_instinct.exo_suit_dash", 88, "key.categories.movement"){
        private boolean isDownOld = false;

        public void setDown(boolean isDown) {
            super.setDown(isDown);
            if (this.isDownOld != isDown && isDown) {
                PacketDistributor.sendToServer(new ExoSuitDashMessage(0, 0));
                ExoSuitDashMessage.pressAction((Player)Minecraft.getInstance().player, 0, 0);
            }
            this.isDownOld = isDown;
        }
    };

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EXO_SUIT_DASH);
    }

    @EventBusSubscriber(value={Dist.CLIENT}, modid = "survival_instinct")
    public static class KeyEventListener {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            if (Minecraft.getInstance().screen == null) {
                EXO_SUIT_DASH.consumeClick();
            }
        }
    }
}

