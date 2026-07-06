package net.tkg.ModernMayhem.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.client.compat.ar.ARCompat;
import net.tkg.ModernMayhem.client.compat.oculus.OculusCompat;
import net.tkg.ModernMayhem.client.item.NVGFirstPersonFakeItem;
import net.tkg.ModernMayhem.client.registry.ClientItemRegistryMM;
import net.tkg.ModernMayhem.client.renderer.custom.NVGFirstPersonRenderer;
import net.tkg.ModernMayhem.server.item.curios.facewear.VisorItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import org.lwjgl.opengl.GL11;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

@EventBusSubscriber(modid="mm", value={Dist.CLIENT})
public class RenderNVGFirstPerson {
    private static final Minecraft MC = Minecraft.getInstance();
    private static NVGFirstPersonFakeItem DUMMY_ITEM = null;
    private static final NVGFirstPersonRenderer RENDERER = new NVGFirstPersonRenderer();
    private static boolean initialized = false;
    private static boolean isRendering = false;
    public static boolean shouldRenderLeftArm = true;
    private static final boolean TACZ_LOADED = ModList.get().isLoaded("tacz");
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("iris");

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    @OnlyIn(value=Dist.CLIENT)
    public static void onRenderHand(RenderHandEvent event) {
        if (!initialized) {
            return;
        }
        if (!RenderNVGFirstPerson.shouldRender()) {
            return;
        }
        if (OculusCompat.isRenderShadow()) {
            return;
        }
        RenderNVGFirstPerson.renderNVGFirstPersonModel(event);
    }

    private static void renderNVGFirstPersonModel(RenderHandEvent event) {
        boolean isVisor;
        if (isRendering) {
            return;
        }
        LocalPlayer player = RenderNVGFirstPerson.MC.player;
        if (player == null) {
            return;
        }
        boolean shadersActive = OCULUS_LOADED && OculusCompat.isShaderPackInUse();
        boolean isShaderTranslucentPass = shadersActive && OculusCompat.isTranslucentHandPass();
        MultiBufferSource.BufferSource buffer = MC.renderBuffers().bufferSource();
        float partialTicks = event.getPartialTick();
        if (event.getHand() == InteractionHand.MAIN_HAND && !player.getOffhandItem().isEmpty()) {
            return;
        }
        if (event.getHand() == InteractionHand.OFF_HAND && player.getOffhandItem().isEmpty()) {
            return;
        }
        event.setCanceled(true);
        isRendering = true;
        ItemStack mainHandStack = player.getMainHandItem();
        boolean isHoldingGun = TACZ_LOADED && RenderNVGFirstPerson.isTACZGun(mainHandStack);
        PoseStack handStack = isHoldingGun ? new PoseStack() : event.getPoseStack();
        ItemInHandRenderer itemInHandRenderer = RenderNVGFirstPerson.MC.gameRenderer.itemInHandRenderer;
        ARCompat.disableAcceleration();
        if (!isShaderTranslucentPass && (shouldRenderLeftArm || event.getHand() == InteractionHand.MAIN_HAND)) {
            itemInHandRenderer.renderHandsWithItems(partialTicks, handStack, buffer, player, event.getPackedLight());
        }
        if (!OculusCompat.endBatch(buffer)) {
            buffer.endBatch();
        }
        ARCompat.resetAcceleration();
        PoseStack nvgStack = event.getPoseStack();
        nvgStack.pushPose();
        GeoModel model = RENDERER.getGeoModel();
        BakedGeoModel bakedModel = model.getBakedModel(model.getModelResource((GeoAnimatable)DUMMY_ITEM));
        ResourceLocation texture = RENDERER.getTextureLocation(DUMMY_ITEM);
        ItemStack facewearItem = CuriosUtil.getFaceWearItem((Player)player);
        boolean bl = isVisor = facewearItem != null && facewearItem.getItem() instanceof VisorItem;
        if (shadersActive) {
            if (isVisor) {
                if (isShaderTranslucentPass) {
                    RenderNVGFirstPerson.renderVisorWithShaderSupport(nvgStack, buffer, event, bakedModel, texture);
                }
            } else if (!isShaderTranslucentPass) {
                RenderNVGFirstPerson.renderNVGWithShaderSupport(nvgStack, buffer, event, bakedModel, texture);
            }
        } else {
            RenderNVGFirstPerson.renderWithoutShaders(nvgStack, buffer, event, bakedModel, texture, isVisor);
        }
        nvgStack.popPose();
        isRendering = false;
    }

    private static void renderVisorWithShaderSupport(PoseStack nvgStack, MultiBufferSource.BufferSource buffer, RenderHandEvent event, BakedGeoModel bakedModel, ResourceLocation texture) {
        RenderType renderType = RenderType.itemEntityTranslucentCull((ResourceLocation)texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask((boolean)false);
        RENDERER.actuallyRender(nvgStack, DUMMY_ITEM, bakedModel, renderType, (MultiBufferSource)buffer, buffer.getBuffer(renderType), false, MC.getTimer().getGameTimeDeltaPartialTick(false), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        if (!OculusCompat.endBatch(buffer)) {
            buffer.endBatch();
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
    }

    private static void renderNVGWithShaderSupport(PoseStack nvgStack, MultiBufferSource.BufferSource buffer, RenderHandEvent event, BakedGeoModel bakedModel, ResourceLocation texture) {
        RenderType renderType = RenderType.entityCutoutNoCull((ResourceLocation)texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask((boolean)true);
        RENDERER.actuallyRender(nvgStack, DUMMY_ITEM, bakedModel, renderType, (MultiBufferSource)buffer, buffer.getBuffer(renderType), false, MC.getTimer().getGameTimeDeltaPartialTick(false), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        if (!OculusCompat.endBatch(buffer)) {
            buffer.endBatch();
        }
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
    }

    private static void renderWithoutShaders(PoseStack nvgStack, MultiBufferSource.BufferSource buffer, RenderHandEvent event, BakedGeoModel bakedModel, ResourceLocation texture, boolean isVisor) {
        RenderType renderType = isVisor ? RenderType.entityTranslucent((ResourceLocation)texture) : RenderType.entityTranslucentCull((ResourceLocation)texture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GL11.glDepthFunc((int)519);
        RenderSystem.depthMask((boolean)false);
        RENDERER.actuallyRender(nvgStack, DUMMY_ITEM, bakedModel, renderType, (MultiBufferSource)buffer, buffer.getBuffer(renderType), false, MC.getTimer().getGameTimeDeltaPartialTick(false), event.getPackedLight(), OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        if (!OculusCompat.endBatch(buffer)) {
            buffer.endBatch();
        }
        GL11.glDepthFunc((int)515);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.disableBlend();
    }

    private static boolean isTACZGun(ItemStack stack) {
        try {
            Class<?> iGunClass = Class.forName("com.tacz.guns.api.item.IGun");
            Method method = iGunClass.getMethod("getIGunOrNull", ItemStack.class);
            Object result = method.invoke(null, stack);
            return result != null;
        }
        catch (Exception e) {
            return false;
        }
    }

    public static boolean shouldRender() {
        LocalPlayer player = RenderNVGFirstPerson.MC.player;
        if (player == null) {
            return false;
        }
        return CuriosUtil.hasNVGEquipped((Player)player);
    }

    public static void initialiseFirstPersonRenderer() {
        ModernMayhemMod.LOGGER.info("[mm] Initializing NVG First Person Renderer");
        if (initialized) {
            return;
        }
        DUMMY_ITEM = (NVGFirstPersonFakeItem)((Object)ClientItemRegistryMM.FIRST_PERSON_NVG.get());
        RENDERER.initCurrentItemStack(DUMMY_ITEM);
        initialized = true;
    }
}

