package net.tkg.ModernMayhem.client.outline.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.tkg.ModernMayhem.client.compat.ar.ARCompat;
import net.tkg.ModernMayhem.client.compat.oculus.OculusCompat;
import net.tkg.ModernMayhem.client.outline.render.EntityMaskRenderer;
import net.tkg.ModernMayhem.client.outline.render.OutlineFramebuffer;
import net.tkg.ModernMayhem.client.outline.render.OutlineShader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class OutlineRenderer {
    private static final boolean OCULUS_LOADED = ModList.get().isLoaded("iris");
    private static final boolean AR_LOADED = ModList.get().isLoaded("acceleratedrendering");
    private static OutlineFramebuffer maskFramebuffer;
    private static OutlineFramebuffer edgeFramebuffer;
    private static OutlineFramebuffer blurFramebuffer1;
    private static OutlineFramebuffer blurFramebuffer2;
    private static OutlineFramebuffer blackOutlineFramebuffer;
    private static OutlineShader maskShader;
    private static OutlineShader blurShader;
    private static OutlineShader applyShader;
    private static OutlineShader blackOutlineShader;
    private static int processedMaskTexture;
    private static int quadVAO;
    private static int quadVBO;
    private static ByteBufferBuilder maskBuffer;
    private static MultiBufferSource.BufferSource maskBufferSource;
    private static int lastEntityCount;
    private static int framesSinceLastCheck;
    private static final int CHECK_INTERVAL = 5;
    private static RenderMode renderMode;
    private static Predicate<Entity> outlinePredicate;
    private static Function<Entity, Integer> colorProvider;
    private static float outlineR;
    private static float outlineG;
    private static float outlineB;
    private static float outlineA;
    private static boolean useColoredOutline;
    private static boolean useBlackOutline;

    public static void setRenderMode(RenderMode mode) {
        renderMode = mode;
    }

    public static RenderMode getRenderMode() {
        return renderMode;
    }

    public static void setOutlineColorProvider(Function<Entity, Integer> provider) {
        colorProvider = provider;
    }

    public static void setOutlinePredicate(Predicate<Entity> predicate) {
        outlinePredicate = predicate;
    }

    public static void setOutlineColor(float r, float g, float b, float a) {
        outlineR = r;
        outlineG = g;
        outlineB = b;
        outlineA = a;
    }

    public static void setUseColoredOutline(boolean use) {
        useColoredOutline = use;
    }

    public static boolean isUsingColoredOutline() {
        return useColoredOutline;
    }

    public static void setUseBlackOutline(boolean use) {
        useBlackOutline = use;
    }

    public static boolean isUsingBlackOutline() {
        return useBlackOutline;
    }

    public static void init() {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        maskFramebuffer = new OutlineFramebuffer(width, height, true);
        edgeFramebuffer = new OutlineFramebuffer(width, height);
        blurFramebuffer1 = new OutlineFramebuffer(width, height);
        blurFramebuffer2 = new OutlineFramebuffer(width, height);
        blackOutlineFramebuffer = new OutlineFramebuffer(width, height);
        try {
            maskShader = new OutlineShader("mask");
            blurShader = new OutlineShader("blur");
            applyShader = new OutlineShader("apply");
            blackOutlineShader = new OutlineShader("black_outline");
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load outline shaders", e);
        }
        OutlineRenderer.createQuadVAO();
        maskBuffer = new ByteBufferBuilder(262144);
        maskBufferSource = MultiBufferSource.immediate(maskBuffer);
    }

    private static void minimalStateReset() {
        GL20.glUseProgram((int)0);
        GlStateManager._glBindFramebuffer((int)36160, (int)0);
        for (int i = 0; i < 4; ++i) {
            RenderSystem.activeTexture((int)(33984 + i));
            GlStateManager._bindTexture((int)0);
        }
        RenderSystem.activeTexture((int)33984);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        GL30.glBindVertexArray((int)0);
    }

    private static void createQuadVAO() {
        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL30.glGenBuffers();
        float[] quadVertices = new float[]{-1.0f, 1.0f, 0.0f, 1.0f, -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, -1.0f, 1.0f, 0.0f, -1.0f, 1.0f, 0.0f, 1.0f, 1.0f, -1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        GL30.glBindVertexArray((int)quadVAO);
        GL30.glBindBuffer((int)34962, (int)quadVBO);
        GL30.glBufferData((int)34962, (float[])quadVertices, (int)35044);
        GL30.glEnableVertexAttribArray((int)0);
        GL30.glVertexAttribPointer((int)0, (int)2, (int)5126, (boolean)false, (int)16, (long)0L);
        GL30.glEnableVertexAttribArray((int)1);
        GL30.glVertexAttribPointer((int)1, (int)2, (int)5126, (boolean)false, (int)16, (long)8L);
        GL30.glBindVertexArray((int)0);
    }

    public static void resize(int width, int height) {
        if (maskFramebuffer != null) {
            maskFramebuffer.resize(width, height);
            edgeFramebuffer.resize(width, height);
            blurFramebuffer1.resize(width, height);
            blurFramebuffer2.resize(width, height);
            blackOutlineFramebuffer.resize(width, height);
        }
    }

    public static void cleanup() {
        if (maskFramebuffer != null) {
            maskFramebuffer.destroy();
            edgeFramebuffer.destroy();
            blurFramebuffer1.destroy();
            blurFramebuffer2.destroy();
            blackOutlineFramebuffer.destroy();
            maskFramebuffer = null;
        }
        if (maskShader != null) {
            maskShader.close();
            blurShader.close();
            applyShader.close();
            blackOutlineShader.close();
            maskShader = null;
        }
        if (quadVAO != -1) {
            GL30.glDeleteVertexArrays((int)quadVAO);
            GL30.glDeleteBuffers((int)quadVBO);
            quadVAO = -1;
            quadVBO = -1;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (renderMode == RenderMode.OFF) {
            return;
        }
        if (OCULUS_LOADED && OculusCompat.isRenderShadow()) {
            return;
        }
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            Minecraft mc = Minecraft.getInstance();
            boolean arDisabled = false;
            try {
                MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
                if (!OculusCompat.endBatch(bufferSource)) {
                    bufferSource.endBatch();
                }
                if (AR_LOADED) {
                    try {
                        ARCompat.disableAcceleration();
                        arDisabled = true;
                    }
                    catch (Throwable t) {
                        System.err.println("[OutlineRenderer] Failed to disable AR acceleration: " + t.getMessage());
                    }
                }
                if (OutlineRenderer.captureMobMasks(event.getPoseStack(), event.getProjectionMatrix())) {
                    OutlineRenderer.drawOverlayToScreen();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (arDisabled && AR_LOADED) {
                    try {
                        ARCompat.resetAcceleration();
                    }
                    catch (Throwable t) {
                        System.err.println("[OutlineRenderer] Failed to reset AR acceleration: " + t.getMessage());
                    }
                }
            }
        }
    }

    private static boolean captureMobMasks(PoseStack poseStack, Matrix4f projectionMatrix) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || maskFramebuffer == null) {
            return false;
        }
        int count = 0;
        if (++framesSinceLastCheck >= 5) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!outlinePredicate.test(entity)) continue;
                ++count;
            }
            lastEntityCount = count;
            framesSinceLastCheck = 0;
        } else {
            count = lastEntityCount;
        }
        if (count == 0) {
            return false;
        }
        if (!useColoredOutline && !useBlackOutline) {
            return false;
        }
        try {
            OutlineRenderer.renderEntityMasks(poseStack, projectionMatrix);
            return true;
        }
        catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    private static void drawOverlayToScreen() {
        try {
            RenderSystem.clearStencil((int)0);
            GL11.glClear((int)1024);
            Minecraft mc = Minecraft.getInstance();
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            if (!OculusCompat.endBatch(bufferSource)) {
                bufferSource.endBatch();
            }
            mc.getMainRenderTarget().bindWrite(false);
            OutlineRenderer.extractEdges();
            OutlineRenderer.applyBlur(true);
            OutlineRenderer.applyBlur(false);
            if (useBlackOutline) {
                OutlineRenderer.createBlackOutline();
            }
            OutlineRenderer.compositeOutline();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            OutlineRenderer.minimalStateReset();
            Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        }
    }

    private static void renderEntityMasks(PoseStack poseStack, Matrix4f projectionMatrix) {
        boolean canShareDepth;
        Minecraft mc = Minecraft.getInstance();
        int targetWidth = mc.getMainRenderTarget().width;
        int targetHeight = mc.getMainRenderTarget().height;
        if (OutlineRenderer.maskFramebuffer.width != targetWidth || OutlineRenderer.maskFramebuffer.height != targetHeight) {
            OutlineRenderer.resize(targetWidth, targetHeight);
        }
        int mainFbo = mc.getMainRenderTarget().frameBufferId;
        GL30.glBindFramebuffer((int)36008, (int)mainFbo);
        int depthType = GL30.glGetFramebufferAttachmentParameteri((int)36008, (int)36096, (int)36048);
        int depthId = GL30.glGetFramebufferAttachmentParameteri((int)36008, (int)36096, (int)36049);
        boolean bl = canShareDepth = depthId != 0;
        if (canShareDepth) {
            maskFramebuffer.attachExternalDepth(depthType, depthId);
        } else {
            maskFramebuffer.restoreInternalDepth();
        }
        maskFramebuffer.bind();
        RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        if (canShareDepth) {
            RenderSystem.clear((int)16384, (boolean)false);
        } else {
            RenderSystem.clear((int)16640, (boolean)false);
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc((int)515);
        RenderSystem.depthMask((boolean)false);
        RenderSystem.colorMask((boolean)true, (boolean)true, (boolean)true, (boolean)true);
        GL11.glDisable((int)2960);
        if (maskBufferSource == null) {
            maskBuffer = new ByteBufferBuilder(262144);
            maskBufferSource = MultiBufferSource.immediate(maskBuffer);
        }
        boolean oldRenderShadows = (Boolean)mc.options.entityShadows().get();
        boolean oldRenderHitboxes = mc.getEntityRenderDispatcher().shouldRenderHitBoxes();
        mc.getEntityRenderDispatcher().setRenderShadow(false);
        mc.getEntityRenderDispatcher().setRenderHitBoxes(false);
        float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(false);
        int renderDistanceChunks = (Integer)mc.options.renderDistance().get();
        double renderDistanceBlocks = (double)renderDistanceChunks * 16.0;
        FogType fogType = mc.gameRenderer.getMainCamera().getFluidInCamera();
        if (fogType == FogType.WATER || fogType == FogType.LAVA) {
            renderDistanceBlocks = Math.min(renderDistanceBlocks, 32.0);
        }
        double renderDistanceSquared = renderDistanceBlocks * renderDistanceBlocks;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!outlinePredicate.test(entity) || entity == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON) continue;
            double lerpX = entity.xOld + (entity.getX() - entity.xOld) * (double)partialTick;
            double lerpY = entity.yOld + (entity.getY() - entity.yOld) * (double)partialTick;
            double lerpZ = entity.zOld + (entity.getZ() - entity.zOld) * (double)partialTick;
            Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            double dx = lerpX - cameraPos.x;
            double dy = lerpY - cameraPos.y;
            double dz = lerpZ - cameraPos.z;
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > renderDistanceSquared) continue;
            if (colorProvider != null) {
                int color = colorProvider.apply(entity);
                float r = (float)(color >> 16 & 0xFF) / 255.0f;
                float g = (float)(color >> 8 & 0xFF) / 255.0f;
                float b = (float)(color & 0xFF) / 255.0f;
                float a = (float)(color >> 24 & 0xFF) / 255.0f;
                if (a == 0.0f) {
                    a = 1.0f;
                }
                RenderSystem.setShaderColor((float)r, (float)g, (float)b, (float)a);
            } else {
                RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
            }
            EntityMaskRenderer.renderEntityMask(entity, lerpX, lerpY, lerpZ, partialTick, poseStack, projectionMatrix, maskBufferSource);
            if (colorProvider == null || OculusCompat.endBatch(maskBufferSource)) continue;
            maskBufferSource.endBatch();
        }
        if (colorProvider == null && !OculusCompat.endBatch(maskBufferSource)) {
            maskBufferSource.endBatch();
        }
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        mc.getEntityRenderDispatcher().setRenderShadow(oldRenderShadows);
        mc.getEntityRenderDispatcher().setRenderHitBoxes(oldRenderHitboxes);
        RenderSystem.depthMask((boolean)true);
        RenderSystem.depthFunc((int)515);
        maskFramebuffer.restoreInternalDepth();
        mc.getMainRenderTarget().bindWrite(false);
    }

    private static void extractEdges() {
        edgeFramebuffer.bind();
        RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        RenderSystem.clear((int)16384, (boolean)false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        GL30.glBindVertexArray((int)quadVAO);
        maskShader.use();
        int sourceMaskTex = processedMaskTexture != 0 ? processedMaskTexture : maskFramebuffer.getColorTexture();
        maskShader.setTexture("DiffuseSampler", sourceMaskTex);
        maskShader.setUniform("InSize", OutlineRenderer.maskFramebuffer.width, OutlineRenderer.maskFramebuffer.height);
        maskShader.setUniform("OutSize", OutlineRenderer.edgeFramebuffer.width, OutlineRenderer.edgeFramebuffer.height);
        OutlineRenderer.drawFullscreenQuad();
        blurFramebuffer1.bind();
        RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        RenderSystem.clear((int)16384, (boolean)false);
        applyShader.use();
        applyShader.setTexture("DiffuseSampler", edgeFramebuffer.getColorTexture());
        applyShader.setUniform("OutlineColor", 1.0f, 1.0f, 1.0f, 1.0f);
        applyShader.setUniform("UseSourceColor", 1);
        OutlineRenderer.drawFullscreenQuad();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    private static void applyBlur(boolean horizontal) {
        OutlineFramebuffer source = horizontal ? blurFramebuffer1 : blurFramebuffer2;
        OutlineFramebuffer target = horizontal ? blurFramebuffer2 : blurFramebuffer1;
        target.bind();
        RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        RenderSystem.clear((int)16384, (boolean)false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableBlend();
        GL30.glBindVertexArray((int)quadVAO);
        blurShader.use();
        blurShader.setTexture("DiffuseSampler", source.getColorTexture());
        blurShader.setUniform("InSize", source.width, source.height);
        blurShader.setUniform("OutSize", target.width, target.height);
        blurShader.setUniform("BlurDir", horizontal ? 1.0f : 0.0f, horizontal ? 0.0f : 1.0f);
        blurShader.setUniform("Radius", 1.0f);
        OutlineRenderer.drawFullscreenQuad();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    private static void createBlackOutline() {
        blackOutlineFramebuffer.bind();
        RenderSystem.clearColor((float)0.0f, (float)0.0f, (float)0.0f, (float)0.0f);
        RenderSystem.clear((int)16384, (boolean)false);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.disableBlend();
        GL30.glBindVertexArray((int)quadVAO);
        blackOutlineShader.use();
        blackOutlineShader.setTexture("DiffuseSampler", edgeFramebuffer.getColorTexture());
        blackOutlineShader.setTexture("EntityMask", maskFramebuffer.getColorTexture());
        blackOutlineShader.setUniform("InSize", OutlineRenderer.edgeFramebuffer.width, OutlineRenderer.edgeFramebuffer.height);
        blackOutlineShader.setUniform("OutSize", OutlineRenderer.blackOutlineFramebuffer.width, OutlineRenderer.blackOutlineFramebuffer.height);
        blackOutlineShader.setUniform("Radius", 1.5f);
        OutlineRenderer.drawFullscreenQuad();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    private static void compositeOutline() {
        int sourceMaskTex;
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask((boolean)false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate((GlStateManager.SourceFactor)GlStateManager.SourceFactor.SRC_ALPHA, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, (GlStateManager.SourceFactor)GlStateManager.SourceFactor.ONE, (GlStateManager.DestFactor)GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GL30.glBindVertexArray((int)quadVAO);
        int n = sourceMaskTex = processedMaskTexture != 0 ? processedMaskTexture : maskFramebuffer.getColorTexture();
        if (renderMode == RenderMode.OUTLINE) {
            if (useBlackOutline) {
                applyShader.use();
                applyShader.setTexture("DiffuseSampler", blackOutlineFramebuffer.getColorTexture());
                applyShader.setUniform("OutlineColor", 0.0f, 0.0f, 0.0f, 1.0f);
                applyShader.setUniform("UseSourceColor", 0);
                OutlineRenderer.drawFullscreenQuad();
            }
            if (useColoredOutline) {
                applyShader.use();
                applyShader.setTexture("DiffuseSampler", blurFramebuffer1.getColorTexture());
                applyShader.setUniform("OutlineColor", outlineR, outlineG, outlineB, outlineA);
                if (colorProvider != null) {
                    applyShader.setUniform("UseSourceColor", 1);
                } else {
                    applyShader.setUniform("UseSourceColor", 0);
                }
                OutlineRenderer.drawFullscreenQuad();
            }
        } else if (renderMode == RenderMode.OVERLAY) {
            applyShader.use();
            applyShader.setTexture("DiffuseSampler", sourceMaskTex);
            applyShader.setUniform("OutlineColor", outlineR, outlineG, outlineB, outlineA);
            if (colorProvider != null) {
                applyShader.setUniform("UseSourceColor", 1);
            } else {
                applyShader.setUniform("UseSourceColor", 0);
            }
            OutlineRenderer.drawFullscreenQuad();
        }
        processedMaskTexture = 0;
        RenderSystem.disableBlend();
    }

    private static void drawFullscreenQuad() {
        GL30.glBindVertexArray((int)quadVAO);
        GL30.glDrawArrays((int)4, (int)0, (int)6);
    }

    static {
        processedMaskTexture = 0;
        quadVAO = -1;
        quadVBO = -1;
        maskBuffer = null;
        maskBufferSource = null;
        lastEntityCount = 0;
        framesSinceLastCheck = 0;
        renderMode = RenderMode.OUTLINE;
        outlinePredicate = entity -> entity instanceof LivingEntity;
        colorProvider = null;
        outlineR = 1.0f;
        outlineG = 1.0f;
        outlineB = 1.0f;
        outlineA = 1.0f;
        useColoredOutline = true;
        useBlackOutline = true;
    }

    public static enum RenderMode {
        OFF,
        OUTLINE,
        OVERLAY;

    }
}

