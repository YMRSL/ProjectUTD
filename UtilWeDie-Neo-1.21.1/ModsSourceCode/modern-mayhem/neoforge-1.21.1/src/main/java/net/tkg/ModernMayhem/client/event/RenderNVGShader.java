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
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.client.ShaderRenderer;
import net.tkg.ModernMayhem.server.item.curios.facewear.NVGGogglesItem;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import net.tkg.ModernMayhem.server.util.NVGConfigs;

@EventBusSubscriber(modid="mm", value={Dist.CLIENT})
public class RenderNVGShader {
    private static final ShaderRenderer NVG_SHADER_RENDERER = new ShaderRenderer(ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"shaders/post/night-vision.json"));
    public static boolean oculusShaderEnabled = false;
    private static final Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        if (oculusShaderEnabled) {
            return;
        }
        try {
            RenderNVGShader.renderNVGShader(event.getPartialTick());
        }
        catch (Exception e) {
            ModernMayhemMod.LOGGER.error("Error rendering NVG shader", (Throwable)e);
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        // AFTER_LEVEL 两个视角都会触发 (onRenderHand 只在第一人称), 所以这里负责【第三人称也渲染夜视】。
        // 第一人称下 onRenderHand 也会触发, renderNVGShader 幂等(已激活则提前返回), 双触发无害。
        try {
            RenderNVGShader.renderNVGShader(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        catch (Exception e) {
            ModernMayhemMod.LOGGER.error("Error rendering NVG shader", (Throwable)e);
        }
    }

    /**
     * 第三人称夜视开关同步兜底。通电状态(NvgCheck)本来只由第一人称模型动画的关键帧(enableNVGEffect/disableNVGEffect)
     * 切换 —— 第三人称那模型不渲染→关键帧不触发→关夜视不生效、切回一人称才补播动画。
     * 这里每 tick 检查: 若 翻盖状态 ≠ 通电状态 且【不在第一人称】, 立即把通电同步成翻盖状态并发包。
     * 第一人称仍交给动画关键帧处理(保留"翻盖到位才通电"的时序手感)。
     */
    @SubscribeEvent
    public static void onClientTickSyncNVGPower(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        LocalPlayer player = mc.player;
        if (player == null || !CuriosUtil.hasNVGEquipped((Player)player)) {
            return;
        }
        ItemStack fw = CuriosUtil.getFaceWearItem((Player)player);
        if (fw == null || !(fw.getItem() instanceof GenericSpecialGogglesItem)) {
            return;
        }
        boolean onFace = GenericSpecialGogglesItem.isNVGOnFace(fw);
        boolean powered = GenericSpecialGogglesItem.getNVGCheck(fw);
        if (powered == onFace) {
            return; // 已同步
        }
        if (mc.options.getCameraType().isFirstPerson()) {
            return; // 第一人称交给动画关键帧 (保留翻盖→通电时序)
        }
        if (onFace) {
            GenericSpecialGogglesItem.switchOnNVGMode(fw);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.tkg.ModernMayhem.server.network.NVGSyncSwitchOnPacket());
        } else {
            GenericSpecialGogglesItem.switchOffNVGMode(fw);
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.tkg.ModernMayhem.server.network.NVGSyncSwitchOffPacket());
        }
    }

    private static void renderNVGShader(float partialTick) {
        NVGGogglesItem nvgGogglesItem;
        Item item;
        LocalPlayer player = RenderNVGShader.mc.player;
        if (player == null) {
            return;
        }
        boolean shouldRender = false;
        GenericSpecialGogglesItem.NVGConfig nvgItemConfig = null;
        ItemStack facewearItem = null;
        if (CuriosUtil.hasNVGEquipped((Player)player) && (facewearItem = CuriosUtil.getFaceWearItem((Player)player)) != null && (item = facewearItem.getItem()) instanceof NVGGogglesItem && (nvgGogglesItem = (NVGGogglesItem)item).shouldRenderShader()) {
            shouldRender = GenericSpecialGogglesItem.getNVGMode(facewearItem) == 1;
            try {
                nvgItemConfig = GenericSpecialGogglesItem.getCurrentConfig(facewearItem);
            }
            catch (Exception e) {
                ModernMayhemMod.LOGGER.error("Error getting NVG config", (Throwable)e);
                shouldRender = false;
            }
        }
        if (shouldRender && !NVG_SHADER_RENDERER.isActive()) {
            NVG_SHADER_RENDERER.activate();
        } else if (!shouldRender && NVG_SHADER_RENDERER.isActive()) {
            NVG_SHADER_RENDERER.deactivate();
        }
        NVG_SHADER_RENDERER.render();
        if (NVG_SHADER_RENDERER.isActive() && nvgItemConfig != null && facewearItem != null) {
            try {
                NVGGogglesItem nvgGogglesItem2;
                Item item2 = facewearItem.getItem();
                boolean isUltraGamer = item2 instanceof NVGGogglesItem && (nvgGogglesItem2 = (NVGGogglesItem)item2).isGamerNVG();
                boolean autoGainActive = false;
                boolean autoGatingActive = false;
                Item item3 = facewearItem.getItem();
                if (item3 instanceof NVGGogglesItem) {
                    NVGGogglesItem nvgItem = (NVGGogglesItem)item3;
                    boolean bl = autoGainActive = nvgItem.hasAutoGain() && NVGGogglesItem.isAutoGainEnabled(facewearItem);
                    if (!autoGainActive && nvgItem.hasAutoGating()) {
                        autoGatingActive = true;
                    }
                }
                // 增益: 管线已砍掉 autogain/autogating 两套 GPU 反馈 pass。
                // 自动增益改成 CPU 端按玩家所在光照级别(0-15)做廉价自动曝光 —— 越暗增益越高,
                // 限制在该夜视仪的 [minGain, maxGain] 内。无 GPU 反馈缓冲 → 不会再黑屏(修问题4), 也省两趟 pass。
                float brightness = nvgItemConfig.getBrightness();
                if (autoGainActive || autoGatingActive) {
                    int lightLevel = 0;
                    try {
                        net.minecraft.core.BlockPos eyePos = net.minecraft.core.BlockPos.containing(player.getEyePosition(1.0f));
                        lightLevel = player.level().getMaxLocalRawBrightness(eyePos);
                    } catch (Exception ignored) {
                    }
                    float sceneLight = Math.max(0.04f, lightLevel / 15.0f);
                    float target = 0.5f;
                    float minGain = nvgItemConfig.getMinGain();
                    float maxGain = nvgItemConfig.getMaxGain();
                    brightness = Math.max(minGain, Math.min(maxGain, target / sceneLight));
                }
                // 整体亮度缩放 (默认 0.8): 压低夜视过曝/最高自适应亮度, 让 IR 锥光对比更明显。
                brightness *= net.tkg.ModernMayhem.client.config.ClientConfig.NVG_BRIGHTNESS_SCALE.get().floatValue();
                NVG_SHADER_RENDERER.setFloatUniform("mm:night-vision", "Brightness", brightness);
                NVG_SHADER_RENDERER.setFloatUniform("mm:night-vision", "RedValue", isUltraGamer ? NVGConfigs.getUltraGamerRedValue() : nvgItemConfig.getRedValue());
                NVG_SHADER_RENDERER.setFloatUniform("mm:night-vision", "GreenValue", isUltraGamer ? NVGConfigs.getUltraGamerGreenValue() : nvgItemConfig.getGreenValue());
                NVG_SHADER_RENDERER.setFloatUniform("mm:night-vision", "BlueValue", isUltraGamer ? NVGConfigs.getUltraGamerBlueValue() : nvgItemConfig.getBlueValue());
                NVG_SHADER_RENDERER.setFloatUniform("mm:night-vision", "NoiseMultiplier", nvgItemConfig.getNoiseMultiplier());
            }
            catch (Exception e) {
                ModernMayhemMod.LOGGER.error("Error setting shader uniforms", (Throwable)e);
            }
        }
    }

    public static boolean isNvActive() {
        return NVG_SHADER_RENDERER.isActive();
    }

    @SubscribeEvent(priority=EventPriority.NORMAL)
    public static void renderNVGOverlay(RenderGuiEvent.Pre event) {
        try {
            GenericSpecialGogglesItem.NVGConfig config;
            NVGGogglesItem nvgGogglesItem;
            Item item;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (!NVG_SHADER_RENDERER.isActive()) {
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
            if (facewearItem != null && (item = facewearItem.getItem()) instanceof NVGGogglesItem && (nvgGogglesItem = (NVGGogglesItem)item).shouldRenderShader() && GenericSpecialGogglesItem.getNVGMode(facewearItem) == 1 && (config = GenericSpecialGogglesItem.getCurrentConfig(facewearItem)) != null && config.getOverlay() != null) {
                // 第三人称也画镜片蒙版, 大小相对第一人称按配置缩放(默认 0.85)居中; 第一人称保持 1.0 原样。
                float maskScale = Minecraft.getInstance().options.getCameraType().isFirstPerson()
                        ? 1.0f
                        : net.tkg.ModernMayhem.client.config.ClientConfig.NVG_THIRD_PERSON_MASK_SCALE.get().floatValue();
                int w = Math.round((float)screenWidth * maskScale);
                int h = Math.round((float)screenHeight * maskScale);
                int x = (screenWidth - w) / 2;
                int y = (screenHeight - h) / 2;
                // 遮罩缩小(scale<1)后, 四周会露出未遮挡的夜视画面 —— 用纯黑把外围那一圈补上(视野外全黑)。
                if (x > 0 || y > 0) {
                    int black = 0xFF000000;
                    event.getGuiGraphics().fill(0, 0, screenWidth, y, black);                  // 上
                    event.getGuiGraphics().fill(0, y + h, screenWidth, screenHeight, black);    // 下
                    event.getGuiGraphics().fill(0, y, x, y + h, black);                         // 左
                    event.getGuiGraphics().fill(x + w, y, screenWidth, y + h, black);           // 右
                }
                event.getGuiGraphics().blit(config.getOverlay(), x, y, 0.0f, 0.0f, w, h, w, h);
            }
            RenderSystem.depthMask((boolean)true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        }
        catch (Exception e) {
            ModernMayhemMod.LOGGER.error("Error rendering NVG overlay", (Throwable)e);
        }
    }
}

