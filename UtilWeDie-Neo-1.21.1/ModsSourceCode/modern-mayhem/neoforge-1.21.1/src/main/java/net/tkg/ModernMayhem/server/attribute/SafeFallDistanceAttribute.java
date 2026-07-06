package net.tkg.ModernMayhem.server.attribute;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tkg.ModernMayhem.server.registry.AttributesRegistryMM;

@EventBusSubscriber(modid="mm", bus=EventBusSubscriber.Bus.GAME)
public class SafeFallDistanceAttribute {
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        AttributeInstance attribute = player.getAttribute(AttributesRegistryMM.SAFE_FALL_DISTANCE);
        if (attribute == null) {
            return;
        }
        double safeFall = attribute.getValue() - 3.0;
        float distance = event.getDistance();
        if ((double)distance <= safeFall) {
            event.setCanceled(true);
        } else {
            float newDistance = (float)((double)distance - safeFall);
            event.setDistance(newDistance);
        }
    }
}

