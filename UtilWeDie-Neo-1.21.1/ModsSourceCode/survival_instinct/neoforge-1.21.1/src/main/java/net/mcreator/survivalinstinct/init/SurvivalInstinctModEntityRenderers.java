package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.client.renderer.NailProyectileRenderer;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT}, modid = "survival_instinct")
public class SurvivalInstinctModEntityRenderers {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer((EntityType)SurvivalInstinctModEntities.HOMEMADE_BOMB_PROYECTILE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer((EntityType)SurvivalInstinctModEntities.MOLOTOV.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer((EntityType)SurvivalInstinctModEntities.NAIL_PROYECTILE.get(), NailProyectileRenderer::new);
    }
}

