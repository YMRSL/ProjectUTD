package com.yitianys.BlockZ.client.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.gui.DayZInventoryScreen;
import com.yitianys.BlockZ.client.key.ModKeyMappings;
import com.yitianys.BlockZ.client.renderer.layer.ClothingLayer;
import com.yitianys.BlockZ.init.ModMenus;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端 modBus 事件注册（KEEP only）。
 *
 * <p>SPLIT：删除了 DROP 注册——HUD overlay（RegisterGuiOverlaysEvent / DayZHudOverlay）、
 * 僵尸/尸体实体渲染器（EntityRenderersEvent.RegisterRenderers）、主菜单屏幕音乐相关。
 * 菜单屏幕注册从 Forge {@code MenuScreens.register}(FMLClientSetupEvent) 迁移为
 * NeoForge {@link RegisterMenuScreensEvent}。
 *
 * <p>保留：菜单屏幕、键位、ClothingLayer 玩家层、以及 ClothingLayer 3D 渲染所需的额外模型。
 */
@EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModBusClientEvents {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.DAYZ_INVENTORY.get(), DayZInventoryScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ModKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        // Add clothing layer to default player renderer
        PlayerRenderer defaultRenderer = event.getSkin(net.minecraft.client.resources.PlayerSkin.Model.WIDE);
        if (defaultRenderer != null) {
            defaultRenderer.addLayer(new ClothingLayer(defaultRenderer));
        }

        // Add clothing layer to slim player renderer (Alex)
        PlayerRenderer slimRenderer = event.getSkin(net.minecraft.client.resources.PlayerSkin.Model.SLIM);
        if (slimRenderer != null) {
            slimRenderer.addLayer(new ClothingLayer(slimRenderer));
        }
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(modelId("item/backpack_coyote_3d"));
        event.register(modelId("item/backpack_alice_3d"));
        event.register(modelId("item/backpack_czech_3d"));
        event.register(modelId("item/backpack_czechpouch_3d"));
        event.register(modelId("item/backpack_patrolpack_3d"));
        event.register(modelId("item/vest_0_3d"));
    }

    private static net.minecraft.client.resources.model.ModelResourceLocation modelId(String path) {
        return net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, path));
    }
}
