package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.block.entity.MortarRackBlockEntity;
import com.scarasol.zombiekit.client.model.ZombieKitGeoBlockModel;
import com.scarasol.zombiekit.client.renderer.*;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = "zombiekit", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ZombieKitEntityRenderers {
    @SuppressWarnings("unchecked")
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ZombieKitEntities.MOLOTOV_COCKTAIL.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.POTION_JAR.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.BILE_JAR.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.LANDMINE.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.FIRECRACKER.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.WRENCH.get(), WrenchRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.FLARES.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.FLARE_GUN.get(), FlareGunRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.DRONE.get(), DroneRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.HEAVY_MACHINE_GUN_AMMO.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.HEAVY_MACHINE_GUN.get(), HeavyMachineGunRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.UV_LAMP.get(), UvLampRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.MORTAR_SHELL.get(), MortarShellRenderer::new);
        event.registerEntityRenderer(ZombieKitEntities.MORTAR.get(), MortarRenderer::new);
        event.registerBlockEntityRenderer((BlockEntityType<MortarRackBlockEntity>) ZombieKitBlockEntities.MORTAR_RACK.get(), context -> new ZombieKitGeoBlockRenderer<>(new ZombieKitGeoBlockModel<>()));
    }

}
