package net.tkg.ModernMayhem.server.attribute;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tkg.ModernMayhem.server.registry.AttributesRegistryMM;

@EventBusSubscriber(modid="mm", bus=EventBusSubscriber.Bus.MOD)
public class AttributeAttacher {
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, AttributesRegistryMM.SAFE_FALL_DISTANCE);
    }
}

