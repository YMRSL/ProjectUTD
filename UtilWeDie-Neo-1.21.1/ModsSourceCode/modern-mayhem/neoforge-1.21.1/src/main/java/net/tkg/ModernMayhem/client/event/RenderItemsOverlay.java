package net.tkg.ModernMayhem.client.event;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tkg.ModernMayhem.server.item.armor.CustomArmorItem;
import net.tkg.ModernMayhem.server.util.ArmorProperties;

@EventBusSubscriber(modid="mm", value={Dist.CLIENT}, bus=EventBusSubscriber.Bus.GAME)
public class RenderItemsOverlay {
    @SubscribeEvent(priority=EventPriority.NORMAL)
    public static void renderRoninOverlay(RenderGuiEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        Item item = helmet.getItem();
        if (!(item instanceof CustomArmorItem)) {
            return;
        }
        CustomArmorItem item2 = (CustomArmorItem)item;
        if (item2.getConfig() != ArmorProperties.RONIN || item2.getType() != ArmorItem.Type.HELMET) {
            return;
        }
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        int screenWidth = event.getGuiGraphics().guiWidth();
        int screenHeight = event.getGuiGraphics().guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        ResourceLocation roninOverlay = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"textures/screens/ronin_overlay.png");
        event.getGuiGraphics().blit(roninOverlay, 0, 0, 0.0f, 0.0f, screenWidth, screenHeight, screenWidth, screenHeight);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }
}

