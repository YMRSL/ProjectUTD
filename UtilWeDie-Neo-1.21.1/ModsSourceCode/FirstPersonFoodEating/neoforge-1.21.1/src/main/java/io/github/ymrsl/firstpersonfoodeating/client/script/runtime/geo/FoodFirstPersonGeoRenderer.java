package io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.ymrsl.firstpersonfoodeating.client.FirstPersonHandEvents;
import io.github.ymrsl.firstpersonfoodeating.client.PlayerArmRenderUtil;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class FoodFirstPersonGeoRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final float DEFAULT_OFFSET_X_PIXELS = 0.0f;
    private static final float DEFAULT_OFFSET_Y_PIXELS = 15.0f;
    private static final float PIXEL_TO_TRANSLATION_SCALE = 2.0f;
    private static final int OFFSET_HISTORY_LIMIT = 32;
    private static final float RUN_SWAY_BLEND_IN_RATE = 8.0f;
    private static final float RUN_SWAY_BLEND_OUT_RATE = 10.0f;
    private static float firstPersonOffsetXPixels = DEFAULT_OFFSET_X_PIXELS;
    private static float firstPersonOffsetYPixels = DEFAULT_OFFSET_Y_PIXELS;
    private static final Deque<OffsetSnapshot> offsetUndoHistory = new ArrayDeque<>();
    private static final Deque<OffsetSnapshot> offsetRedoHistory = new ArrayDeque<>();
    private static int firstPersonRenderLogBudget = 6;
    // Private immediate buffer: render the food + arm into this and flush it on the spot, so the
    // draw happens while the modelview is still the first-person camera-view matrix. That is what
    // the modelview-inverse cancellation in render() is computed against, so it must not be
    // deferred to a later flush (where the modelview differs).
    private static final MultiBufferSource.BufferSource FP_IMMEDIATE =
            MultiBufferSource.immediate(new com.mojang.blaze3d.vertex.ByteBufferBuilder(1536));
    private static float laggedPitchDeg = 0.0f;
    private static float laggedYawDeg = 0.0f;
    private static float laggedPitchVel = 0.0f;
    private static float laggedYawVel = 0.0f;
    private static long lookSpringLastNs = -1L;
    private static boolean lookSpringInitialized = false;
    private static float runSwayBlend = 0.0f;
    private static long runSwayLastNs = -1L;

    private FoodFirstPersonGeoRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                              FoodGeoModel model, ResourceLocation textureLocation, @Nullable String visibleRoot) {
        render(poseStack, bufferSource, light, model, textureLocation, visibleRoot, null, null, null);
    }

    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, int light,
                              FoodGeoModel model, ResourceLocation textureLocation,
                              @Nullable List<String> visibleRoots) {
        render(poseStack, bufferSource, light, model, textureLocation, null, visibleRoots, null, null);
    }

    public static void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            FoodGeoModel model,
            ResourceLocation textureLocation,
            @Nullable String visibleRoot,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        render(poseStack, bufferSource, light, model, textureLocation, visibleRoot, null, animatedPose, null);
    }

    public static void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            FoodGeoModel model,
            ResourceLocation textureLocation,
            @Nullable List<String> visibleRoots,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        render(poseStack, bufferSource, light, model, textureLocation, null, visibleRoots, animatedPose, null);
    }

    public static void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            FoodGeoModel model,
            ResourceLocation textureLocation,
            @Nullable String visibleRoot,
            @Nullable List<String> visibleRoots,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose,
            @Nullable ItemStack stackForStaticIdleBob
    ) {
        VertexConsumer consumer = FP_IMMEDIATE.getBuffer(RenderType.entityCutoutNoCull(textureLocation));

        poseStack.pushPose();
        poseStack.last().pose().identity();
        poseStack.last().normal().identity();
        // ROOT FIX (world-lock / "facing north"): 1.21 draws first-person items with
        // modelView = the live camera view matrix, and the vanilla hand poseStack carries ITS
        // INVERSE so the item is screen-locked. Clearing to identity above dropped that inverse,
        // so the model rendered world-locked. Re-apply the inverse of the ACTUAL modelview to
        // cancel the camera rotation. Using the live matrix (not Camera.rotation()) is exact and
        // self-adapting: a no-op where the frame is already view space, and a full cancel where
        // the modelview is the camera view -- which is why earlier Camera.rotation() attempts
        // over-rotated ("double-follow") or froze ("R^-1").
        Matrix4f mvInv = new Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).invert();
        poseStack.last().pose().mul(mvInv);
        poseStack.last().normal().mul(new Matrix3f(mvInv));
        applyLookDirectionOffset(poseStack);
        poseStack.translate(computeFirstPersonHorizontalOffset(), computeFirstPersonVerticalOffset(), 0.0f);
        poseStack.translate(0.0f, 1.5f, 0.0f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        if (stackForStaticIdleBob != null && !stackForStaticIdleBob.isEmpty()) {
            FirstPersonHandEvents.applyStaticIdleBobIfNeeded(stackForStaticIdleBob, poseStack);
        }
        applyFirstPersonPositioningTransform(poseStack, model, animatedPose);
        boolean renderedByPart = false;
        if (visibleRoots != null && !visibleRoots.isEmpty()) {
            for (String root : visibleRoots) {
                if (root == null || root.isBlank()) {
                    continue;
                }
                renderedByPart |= model.renderPart(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, root, animatedPose);
            }
        }
        if (visibleRoot != null && !visibleRoot.isBlank()) {
            renderedByPart |= model.renderPart(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, visibleRoot, animatedPose);
        }
        if (!renderedByPart) {
            model.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, animatedPose);
        }
        renderPlayerArmAtNode(poseStack, FP_IMMEDIATE, light, model, animatedPose, "righthand_pos", HumanoidArm.RIGHT);
        renderPlayerArmAtNode(poseStack, FP_IMMEDIATE, light, model, animatedPose, "lefthand_pos", HumanoidArm.LEFT);
        poseStack.popPose();
        // Flush on the spot, while the modelview is still the first-person camera-view matrix the
        // modelview-inverse above was computed against. Deferring to a later flush would draw it
        // under a different modelview and the cancellation would no longer hold.
        FP_IMMEDIATE.endBatch();

        if (firstPersonRenderLogBudget > 0) {
            firstPersonRenderLogBudget--;
            LOGGER.info("[firstpersonfoodeating] First-person geo render: texture={}, visibleRoot={}, renderedByPart={}, offsetPx=({}, {})",
                    textureLocation, visibleRoot, renderedByPart, firstPersonOffsetXPixels, firstPersonOffsetYPixels);
        }
    }

    public static void adjustFirstPersonOffsetPixels(float deltaX, float deltaY) {
        applyOffsetPixels(firstPersonOffsetXPixels + deltaX, firstPersonOffsetYPixels + deltaY, true, "adjust");
    }

    public static void setFirstPersonOffsetPixels(float xPixels, float yPixels) {
        applyOffsetPixels(xPixels, yPixels, true, "set");
    }

    public static void resetFirstPersonOffsetPixels() {
        applyOffsetPixels(DEFAULT_OFFSET_X_PIXELS, DEFAULT_OFFSET_Y_PIXELS, true, "reset");
    }

    public static boolean undoFirstPersonOffsetPixels() {
        if (offsetUndoHistory.isEmpty()) {
            return false;
        }
        OffsetSnapshot current = new OffsetSnapshot(firstPersonOffsetXPixels, firstPersonOffsetYPixels);
        OffsetSnapshot previous = offsetUndoHistory.pop();
        pushHistory(offsetRedoHistory, current);
        applyOffsetPixels(previous.xPixels(), previous.yPixels(), false, "undo");
        return true;
    }

    public static boolean redoFirstPersonOffsetPixels() {
        if (offsetRedoHistory.isEmpty()) {
            return false;
        }
        OffsetSnapshot current = new OffsetSnapshot(firstPersonOffsetXPixels, firstPersonOffsetYPixels);
        OffsetSnapshot next = offsetRedoHistory.pop();
        pushHistory(offsetUndoHistory, current);
        applyOffsetPixels(next.xPixels(), next.yPixels(), false, "redo");
        return true;
    }

    public static float getFirstPersonOffsetXPixels() {
        return firstPersonOffsetXPixels;
    }

    public static float getFirstPersonOffsetYPixels() {
        return firstPersonOffsetYPixels;
    }

    public static void applyFirstPersonPositioningTransform(
            PoseStack poseStack,
            FoodGeoModel model,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        List<FoodGeoModel.Part> nodePath = model.getPath("camera");
        if (nodePath == null) {
            nodePath = model.getPath("aim_root");
        }
        if (nodePath == null) {
            nodePath = model.getPath("ep_camera");
        }
        if (nodePath == null) {
            nodePath = model.getPath("root");
        }
        if (nodePath == null || nodePath.isEmpty()) {
            return;
        }

        poseStack.translate(0.0f, 1.5f, 0.0f);
        applyInverseNodePathTransform(poseStack, nodePath, animatedPose);
        poseStack.translate(0.0f, -1.5f, 0.0f);
    }

    private static void applyInverseNodePathTransform(
            PoseStack poseStack,
            List<FoodGeoModel.Part> nodePath,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        for (int i = nodePath.size() - 1; i >= 0; i--) {
            FoodGeoModel.Part part = nodePath.get(i);
            PartTransform transform = resolveTransform(part, animatedPose);
            if (transform.xRot() != 0.0f) {
                poseStack.mulPose(Axis.XN.rotation(transform.xRot()));
            }
            if (transform.yRot() != 0.0f) {
                poseStack.mulPose(Axis.YN.rotation(transform.yRot()));
            }
            if (transform.zRot() != 0.0f) {
                poseStack.mulPose(Axis.ZN.rotation(transform.zRot()));
            }
            if (part.getParent() != null) {
                poseStack.translate(-transform.x() / 16.0f, -transform.y() / 16.0f, -transform.z() / 16.0f);
            } else {
                poseStack.translate(-transform.x() / 16.0f, 1.5f - transform.y() / 16.0f, -transform.z() / 16.0f);
            }
        }
    }

    private static void applyLookDirectionOffset(PoseStack poseStack) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            resetLookSpring();
            return;
        }
        // 用 false 取标准渲染插值 partialTick（与 Sona 等渲染代码一致；true 会按暂停语义取值）。
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float xRotOffset = Mth.lerp(partialTick, player.xBobO, player.xBob);
        float yRotOffset = Mth.lerp(partialTick, player.yBobO, player.yBob);
        float xRot = player.getViewXRot(partialTick) - xRotOffset;
        float yRot = player.getViewYRot(partialTick) - yRotOffset;

        // Invert sway direction so turn motion produces delayed trailing rather than lead.
        float targetPitch = (float) Math.tanh(xRot / 32.0f) * 3.0f;
        float targetYaw = (float) Math.tanh(yRot / 32.0f) * 3.4f;
        updateLookSpring(targetPitch, targetYaw);

        poseStack.mulPose(Axis.XP.rotationDegrees(laggedPitchDeg));
        poseStack.mulPose(Axis.YP.rotationDegrees(laggedYawDeg));
        poseStack.translate(laggedYawDeg * 0.00075f, -laggedPitchDeg * 0.0005f, 0.0f);

        boolean runSwayTarget = !player.isMovingSlowly() && player.isSprinting() && player.onGround();
        float runBlend = updateRunSwayBlend(runSwayTarget);
        if (runBlend > 0.0001f) {
            float t = (player.tickCount + partialTick) * 0.9f;
            float sway = Mth.sin(t) * 0.022f;
            float lift = Math.abs(Mth.cos(t * 0.5f)) * 0.013f;
            poseStack.translate(sway * runBlend, lift * runBlend, 0.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(-sway * 45.0f * runBlend));
            poseStack.mulPose(Axis.XP.rotationDegrees(-lift * 80.0f * runBlend));
        }
    }

    private static float computeFirstPersonHorizontalOffset() {
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = Math.max(minecraft.getWindow().getWidth(), 1);
        return firstPersonOffsetXPixels / (float) screenWidth * PIXEL_TO_TRANSLATION_SCALE;
    }

    private static float computeFirstPersonVerticalOffset() {
        Minecraft minecraft = Minecraft.getInstance();
        int screenHeight = Math.max(minecraft.getWindow().getHeight(), 1);
        return firstPersonOffsetYPixels / (float) screenHeight * PIXEL_TO_TRANSLATION_SCALE;
    }

    private static void applyOffsetPixels(float xPixels, float yPixels, boolean recordHistory, String action) {
        if (firstPersonOffsetXPixels == xPixels && firstPersonOffsetYPixels == yPixels) {
            return;
        }
        if (recordHistory) {
            pushHistory(offsetUndoHistory, new OffsetSnapshot(firstPersonOffsetXPixels, firstPersonOffsetYPixels));
            offsetRedoHistory.clear();
        }
        firstPersonOffsetXPixels = xPixels;
        firstPersonOffsetYPixels = yPixels;
        LOGGER.info("[firstpersonfoodeating] First-person render offset updated ({}) : x={}px, y={}px",
                action, firstPersonOffsetXPixels, firstPersonOffsetYPixels);
    }

    private static void pushHistory(Deque<OffsetSnapshot> history, OffsetSnapshot snapshot) {
        history.push(snapshot);
        while (history.size() > OFFSET_HISTORY_LIMIT) {
            history.removeLast();
        }
    }

    private static void updateLookSpring(float targetPitch, float targetYaw) {
        long now = System.nanoTime();
        if (!lookSpringInitialized || lookSpringLastNs <= 0L) {
            lookSpringInitialized = true;
            lookSpringLastNs = now;
            laggedPitchDeg = targetPitch;
            laggedYawDeg = targetYaw;
            laggedPitchVel = 0.0f;
            laggedYawVel = 0.0f;
            return;
        }
        float dt = (now - lookSpringLastNs) / 1_000_000_000.0f;
        lookSpringLastNs = now;
        dt = Mth.clamp(dt, 0.001f, 0.05f);

        // Critically damped-ish spring: adds follow delay while turning,
        // then gives a quick rebound when camera movement stops.
        float yawAccel = (targetYaw - laggedYawDeg) * 48.0f - laggedYawVel * 11.5f;
        laggedYawVel += yawAccel * dt;
        laggedYawDeg += laggedYawVel * dt;

        float pitchAccel = (targetPitch - laggedPitchDeg) * 42.0f - laggedPitchVel * 10.0f;
        laggedPitchVel += pitchAccel * dt;
        laggedPitchDeg += laggedPitchVel * dt;
    }

    private static void resetLookSpring() {
        lookSpringInitialized = false;
        lookSpringLastNs = -1L;
        laggedPitchDeg = 0.0f;
        laggedYawDeg = 0.0f;
        laggedPitchVel = 0.0f;
        laggedYawVel = 0.0f;
        runSwayBlend = 0.0f;
        runSwayLastNs = -1L;
    }

    private static float updateRunSwayBlend(boolean active) {
        long now = System.nanoTime();
        if (runSwayLastNs <= 0L) {
            runSwayLastNs = now;
            runSwayBlend = active ? 1.0f : 0.0f;
            return runSwayBlend;
        }
        float dt = (now - runSwayLastNs) / 1_000_000_000.0f;
        runSwayLastNs = now;
        dt = Mth.clamp(dt, 0.001f, 0.05f);

        float rate = active ? RUN_SWAY_BLEND_IN_RATE : RUN_SWAY_BLEND_OUT_RATE;
        float step = dt * rate;
        runSwayBlend = Mth.approach(runSwayBlend, active ? 1.0f : 0.0f, step);
        return runSwayBlend;
    }

    private static PartTransform resolveTransform(
            FoodGeoModel.Part part,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        float x = part.getX();
        float y = part.getY();
        float z = part.getZ();
        float xRot = part.getXRot();
        float yRot = part.getYRot();
        float zRot = part.getZRot();
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        float scaleZ = 1.0f;
        if (animatedPose != null && part.getName() != null) {
            FoodAnimationController.BonePose pose = animatedPose.get(part.getName());
            if (pose != null) {
                x += pose.position().x();
                y -= pose.position().y();
                z += pose.position().z();
                xRot += (float) Math.toRadians(pose.rotationDeg().x());
                yRot += (float) Math.toRadians(pose.rotationDeg().y());
                zRot += (float) Math.toRadians(pose.rotationDeg().z());
                scaleX *= pose.scale().x();
                scaleY *= pose.scale().y();
                scaleZ *= pose.scale().z();
            }
        }
        return new PartTransform(x, y, z, xRot, yRot, zRot, scaleX, scaleY, scaleZ);
    }

    private static void renderPlayerArmAtNode(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            FoodGeoModel model,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose,
            String nodeName,
            HumanoidArm arm
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        List<FoodGeoModel.Part> nodePath = model.getPath(nodeName);
        if (nodePath == null || nodePath.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        for (FoodGeoModel.Part part : nodePath) {
            applyPathPartTransform(poseStack, part, animatedPose);
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        Matrix4f pose = new Matrix4f(poseStack.last().pose());
        Matrix3f normal = new Matrix3f(poseStack.last().normal());
        PoseStack armStack = new PoseStack();
        armStack.last().pose().mul(pose);
        armStack.last().normal().mul(normal);
        PlayerArmRenderUtil.renderFirstPersonArm(player, arm, armStack, bufferSource, light);
        poseStack.popPose();
    }

    private static void applyPathPartTransform(
            PoseStack poseStack,
            FoodGeoModel.Part part,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        PartTransform transform = resolveTransform(part, animatedPose);
        poseStack.translate(transform.x() / 16.0f, transform.y() / 16.0f, transform.z() / 16.0f);
        if (transform.zRot() != 0.0f) {
            poseStack.mulPose(Axis.ZP.rotation(transform.zRot()));
        }
        if (transform.yRot() != 0.0f) {
            poseStack.mulPose(Axis.YP.rotation(transform.yRot()));
        }
        if (transform.xRot() != 0.0f) {
            poseStack.mulPose(Axis.XP.rotation(transform.xRot()));
        }
    }

    private record PartTransform(
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
    }

    private record OffsetSnapshot(float xPixels, float yPixels) {
    }
}
