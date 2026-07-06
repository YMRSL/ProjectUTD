package com.scarasol.zombiekit.client.shaders;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.mixin.LevelRendererAccessor;
import com.scarasol.zombiekit.mixin.RenderChunkInfoAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Scarasol
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ThermalShader implements ResourceManagerReloadListener {
    private static final ResourceLocation THERMAL_EFFECT = new ResourceLocation("zombiekit", "shaders/post/thermal.json");
    private static final String BLOCK_TARGET = "thermal_buffer";
    private static final String ENTITY_TARGET = "thermal_entity_buffer";
    private static boolean isActive = false;
    private static PostChain thermalChain;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean seeThroughWalls = true;

    private static final Map<ChunkPos, LuminousCache> LUMINOUS_CACHE = new ConcurrentHashMap<>();
    private static final List<LuminousBlock> VISIBLE_LUMINOUS_BLOCKS = new ArrayList<>();


    private record LuminousBlock(BlockState state, ChunkPos chunkPos, int emission, int color, int worldX, int worldY,
                                 int worldZ) {
    }

    private record LuminousCache(List<LuminousBlock> blocks) {
    }

    public static void setSeeThroughWalls(boolean seeThrough) {
        seeThroughWalls = seeThrough;
    }

    public static void setActive(boolean active) {
        if (isActive != active) {
            isActive = active;
            if (!active) {
                cleanup();
            }
        }
    }

    private static void cleanup() {
        if (thermalChain != null) {
            thermalChain.close();
            thermalChain = null;
        }
    }

    public static boolean isActive() {
        return isActive;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        cleanup();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        setSeeThroughWalls(false);
        setActive(true);
        if (!isActive) {
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            collectVisibleLuminousBlocks();
            prepareAndRenderEntities(event.getPoseStack(), event.getPartialTick());
        }
    }


    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (!isActive || thermalChain == null) {
            return;
        }
        applyPostProcess(event.getPartialTick());
        RenderSystem.depthMask(true);
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

    }

    private static boolean ensureChain(Minecraft mc) {
        if (thermalChain == null) {
            try {
                thermalChain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), THERMAL_EFFECT);
                thermalChain.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
                lastWidth = mc.getWindow().getWidth();
                lastHeight = mc.getWindow().getHeight();
            } catch (Exception e) {
                e.printStackTrace();
                isActive = false;
                return false;
            }
        }

        if (lastWidth != mc.getWindow().getWidth() || lastHeight != mc.getWindow().getHeight()) {
            lastWidth = mc.getWindow().getWidth();
            lastHeight = mc.getWindow().getHeight();
            thermalChain.resize(lastWidth, lastHeight);
        }
        return true;
    }

    private static void prepareAndRenderEntities(PoseStack poseStack, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        if (!ensureChain(mc)) {
            return;
        }

        // 获取 Buffer
        RenderTarget blockBuffer = thermalChain.getTempTarget(BLOCK_TARGET);
        RenderTarget entityBuffer = thermalChain.getTempTarget(ENTITY_TARGET);
        if (blockBuffer == null || entityBuffer == null) {
            return;
        }

        prepareRenderTarget(blockBuffer, mc);
        prepareRenderTarget(entityBuffer, mc);

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        poseStack.pushPose();

//        RenderSystem.enablePolygonOffset();
//        RenderSystem.polygonOffset(-1.0F, -1.0F);
        mc.getEntityRenderDispatcher().setRenderShadow(false);
        blockBuffer.bindWrite(true);
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        renderLuminousBlocks(mc, poseStack, bufferSource, cameraPos);
        bufferSource.endBatch();

        entityBuffer.bindWrite(true);
        MultiBufferSource.BufferSource entitySource = mc.renderBuffers().bufferSource();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (isHotEntity(entity)) {
                double lerpX = Mth.lerp(partialTick, entity.xo, entity.getX());
                double lerpY = Mth.lerp(partialTick, entity.yo, entity.getY());
                double lerpZ = Mth.lerp(partialTick, entity.zo, entity.getZ());

                mc.getEntityRenderDispatcher().render(
                        entity,
                        lerpX - cameraPos.x,
                        lerpY - cameraPos.y,
                        lerpZ - cameraPos.z,
                        entity.getViewYRot(partialTick),
                        partialTick,
                        poseStack,
                        entitySource,
                        15728880
                );
            }
        }

        entitySource.endBatch();
//        RenderSystem.disablePolygonOffset();
        poseStack.popPose();

        mc.getMainRenderTarget().bindWrite(true);
    }



    private static void applyPostProcess(float partialTick) {
        if (thermalChain == null) {
            return;
        }

        try {
            thermalChain.process(partialTick);
        } catch (Exception e) {
            e.printStackTrace();
            cleanup();
        }

        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }

    private static boolean isHotEntity(Entity entity) {
        if (entity == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return false;
        }
//        return entity instanceof LivingEntity || entity.isOnFire();
        return true;
    }

    private static void prepareRenderTarget(RenderTarget target, Minecraft mc) {
        target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        target.clear(Minecraft.ON_OSX);
        if (!seeThroughWalls) {
            if (mc.getMainRenderTarget().isStencilEnabled() && !target.isStencilEnabled()) {
                target.enableStencil();
            }

            try {
                target.copyDepthFrom(mc.getMainRenderTarget());
            } catch (Throwable ignored) {
                seeThroughWalls = true;
            }
        }
    }

    public static void recordLuminousBlock(BlockAndTintGetter level, BlockState state, BlockPos pos, int emission) {
        if (emission <= 0) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        ChunkPos chunkPos = new ChunkPos(pos);
        MapColor mapColor = state.getMapColor(level, pos);
        int color = mapColor != null ? mapColor.col : MapColor.NONE.col;
        LUMINOUS_CACHE.compute(chunkPos, (cp, cache) -> {
            List<LuminousBlock> list = cache == null ? new ArrayList<>() : new ArrayList<>(cache.blocks());

            list.add(new LuminousBlock(state, chunkPos, emission, color, pos.getX(), pos.getY(), pos.getZ()));
            return new LuminousCache(list);
        });
    }

    private static void collectVisibleLuminousBlocks() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.levelRenderer instanceof LevelRendererAccessor accessor)) {
            return;
        }

        ObjectArrayList<?> infos = accessor.zombiekit$getRenderChunksInFrustum();
        if (infos == null) {
            return;
        }


        VISIBLE_LUMINOUS_BLOCKS.clear();
        for (Object info : infos) {
            if (!(info instanceof RenderChunkInfoAccessor infoAccessor)) {
                continue;
            }
            ChunkRenderDispatcher.RenderChunk renderChunk = infoAccessor.zombiekit$getChunk();
            ChunkPos chunkPos = new ChunkPos(renderChunk.getOrigin());
            LuminousCache cache = LUMINOUS_CACHE.get(chunkPos);
            if (cache == null) {
                continue;
            }
            List<LuminousBlock> validBlocks = new ArrayList<>();
            for (LuminousBlock block : cache.blocks()) {
                BlockPos pos = new BlockPos(block.worldX(), block.worldY(), block.worldZ());
                BlockState currentState = mc.level.getBlockState(pos);
                int emission = currentState.getLightEmission(mc.level, pos);
                if (!currentState.isAir() && emission > 0) {
                    MapColor mapColor = currentState.getMapColor(mc.level, pos);
                    int color = mapColor != null ? mapColor.col : MapColor.NONE.col;
                    validBlocks.add(new LuminousBlock(currentState, chunkPos, emission, color, block.worldX(), block.worldY(), block.worldZ()));
                }
            }
            if (validBlocks.isEmpty()) {
                LUMINOUS_CACHE.remove(chunkPos);
                continue;
            }
            if (validBlocks.size() != cache.blocks().size()) {
                LUMINOUS_CACHE.put(chunkPos, new LuminousCache(validBlocks));
            }
            VISIBLE_LUMINOUS_BLOCKS.addAll(validBlocks);
        }
    }

    private static void renderLuminousBlocks(Minecraft mc, PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Vec3 cameraPos) {
        if (VISIBLE_LUMINOUS_BLOCKS.isEmpty()) {
            return;
        }
        for (LuminousBlock block : VISIBLE_LUMINOUS_BLOCKS) {
            int worldX = block.worldX();
            int worldZ = block.worldZ();
            int worldY = block.worldY();
            poseStack.pushPose();
            poseStack.translate(worldX - cameraPos.x, worldY - cameraPos.y, worldZ - cameraPos.z);
            BlockRenderDispatcher dispatcher = mc.getBlockRenderer();
//            float heatFactor = Mth.clamp(block.emission() / 15.0f, 0.4f, 1.0f);
//            boolean isLava = block.state().getFluidState().is(FluidTags.LAVA);
//            float boost = isLava ? 0.8f : 0.25f;
//            float r = Mth.clamp(1.0f + boost * heatFactor, 1.0f, 1.8f);
//            float g = Mth.clamp(1.0f + boost * 0.7f * heatFactor, 1.0f, 1.5f);
//            float b = Mth.clamp(1.0f + boost * 0.4f * heatFactor, 1.0f, 1.3f);
//            RenderSystem.setShaderColor(r, g, b, 1.0f);
            dispatcher.renderSingleBlock(block.state(), poseStack, bufferSource, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
//            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            poseStack.popPose();
        }
    }
}
