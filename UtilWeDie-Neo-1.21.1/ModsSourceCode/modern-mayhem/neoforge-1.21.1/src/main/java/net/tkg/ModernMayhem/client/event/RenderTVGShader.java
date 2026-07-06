package net.tkg.ModernMayhem.client.event;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tkg.ModernMayhem.client.ShaderRenderer;
import net.tkg.ModernMayhem.server.item.curios.facewear.TVGGogglesItem;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

@EventBusSubscriber(modid="mm", value={Dist.CLIENT})
public class RenderTVGShader {
    private static final ShaderRenderer THERMAL_SHADER_RENDERER = new ShaderRenderer(ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"shaders/post/thermal-vision.json"));
    public static boolean oculusShaderEnabled = false;
    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (oculusShaderEnabled) {
            return;
        }
        try {
            RenderTVGShader.renderThermalShader(event.getPartialTick());
        }
        catch (Exception e) {
            System.err.println("[ModernMayhem] Error rendering thermal shader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        // AFTER_LEVEL 两视角都触发, 让【第三人称也渲染热成像】(onRenderHand 只在第一人称)。幂等, 双触发无害。
        try {
            RenderTVGShader.renderThermalShader(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        catch (Exception e) {
            System.err.println("[ModernMayhem] Error rendering thermal shader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void renderThermalShader(float partialTick) {
        TVGGogglesItem thermalItem;
        Item item;
        LocalPlayer player = RenderTVGShader.mc.player;
        if (player == null) {
            return;
        }
        boolean shouldRender = false;
        GenericSpecialGogglesItem.NVGConfig thermalConfig = null;
        ItemStack facewearItem = null;
        if (CuriosUtil.hasNVGEquipped((Player)player) && (item = (facewearItem = CuriosUtil.getFaceWearItem((Player)player)).getItem()) instanceof TVGGogglesItem && (thermalItem = (TVGGogglesItem)item).shouldRenderShader()) {
            shouldRender = GenericSpecialGogglesItem.getNVGMode(facewearItem) == 1;
            thermalConfig = GenericSpecialGogglesItem.getCurrentConfig(facewearItem);
        }
        if (shouldRender && !THERMAL_SHADER_RENDERER.isActive()) {
            THERMAL_SHADER_RENDERER.activate();
        } else if (!shouldRender && THERMAL_SHADER_RENDERER.isActive()) {
            THERMAL_SHADER_RENDERER.deactivate();
        }
        THERMAL_SHADER_RENDERER.render();
    }

    public static boolean isThermalActive() {
        return THERMAL_SHADER_RENDERER.isActive();
    }

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public static void renderThermalOverlay(RenderGuiEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (!THERMAL_SHADER_RENDERER.isActive()) {
            return;
        }
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        ItemStack facewearItem = CuriosUtil.getFaceWearItem((Player)player);
        GenericSpecialGogglesItem.NVGConfig tvgConfig;
        if (facewearItem.getItem() instanceof TVGGogglesItem && GenericSpecialGogglesItem.getNVGMode(facewearItem) == 1 && (tvgConfig = GenericSpecialGogglesItem.getCurrentConfig(facewearItem)) != null && tvgConfig.getOverlay() != null) {
            // 第三人称也画镜片蒙版, 按配置缩放(默认 0.85)居中 + 外圈纯黑补上; 第一人称保持 1.0 原样。
            float maskScale = Minecraft.getInstance().options.getCameraType().isFirstPerson()
                    ? 1.0f
                    : net.tkg.ModernMayhem.client.config.ClientConfig.NVG_THIRD_PERSON_MASK_SCALE.get().floatValue();
            int w = Math.round((float)screenWidth * maskScale);
            int h = Math.round((float)screenHeight * maskScale);
            int x = (screenWidth - w) / 2;
            int y = (screenHeight - h) / 2;
            if (x > 0 || y > 0) {
                int black = 0xFF000000;
                event.getGuiGraphics().fill(0, 0, screenWidth, y, black);
                event.getGuiGraphics().fill(0, y + h, screenWidth, screenHeight, black);
                event.getGuiGraphics().fill(0, y, x, y + h, black);
                event.getGuiGraphics().fill(x + w, y, screenWidth, y + h, black);
            }
            event.getGuiGraphics().blit(tvgConfig.getOverlay(), x, y, 0.0f, 0.0f, w, h, w, h);
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
    }
}

