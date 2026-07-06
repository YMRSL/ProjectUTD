package io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ymrsl.firstpersonfoodeating.client.FirstPersonHandEvents;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAssetsManager;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodDisplayDefinition;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodTextureResolver;
import io.github.ymrsl.firstpersonfoodeating.item.FoodStackData;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class FoodGeoItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final FoodGeoItemRenderer INSTANCE = new FoodGeoItemRenderer();
    private static final boolean ENABLE_THIRD_PERSON_BONE_ANIMATION = false;
    private static final float THIRD_PERSON_RIGHT_FINE_X = 0.160f;
    private static final float THIRD_PERSON_LEFT_FINE_X = -0.160f;
    private static int rendererLogBudget = 10;
    private static int thirdPersonPoseMissLogBudget = 20;
    private static int thirdPersonGripLogBudget = 12;

    private FoodGeoItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static FoodGeoItemRenderer getInstance() {
        return INSTANCE;
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext context, @Nonnull PoseStack poseStack,
                             @Nonnull MultiBufferSource bufferSource, int light, int overlay) {
        ResourceLocation registryItemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (registryItemId == null) {
            return;
        }
        ResourceLocation foodId = FoodStackData.resolveFoodId(stack);
        FoodDisplayDefinition display = FoodAssetsManager.get().getDisplay(foodId).orElse(null);
        if (display == null) {
            return;
        }
        boolean firstPerson = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean gui = context == ItemDisplayContext.GUI;
        boolean useThirdGeo = !firstPerson && !gui;

        ResourceLocation primaryGeoId = useThirdGeo ? display.resolveThirdGeoId() : display.resolveGeoId();
        ResourceLocation fallbackGeoId = display.resolveGeoId();
        FoodGeoModel geoModel = FoodAssetsManager.get().getGeoModel(primaryGeoId).orElse(null);
        if (geoModel == null && useThirdGeo && !primaryGeoId.equals(fallbackGeoId)) {
            geoModel = FoodAssetsManager.get().getGeoModel(fallbackGeoId).orElse(null);
        }
        if (geoModel == null) {
            return;
        }
        ResourceLocation texture = useThirdGeo
                ? FoodTextureResolver.resolveThirdTexture(display)
                : FoodTextureResolver.resolveDisplayTexture(display);
        List<String> visibleRoots;
        if (useThirdGeo) {
            visibleRoots = display.resolveThirdGeoVisibleRoots();
            if (visibleRoots.isEmpty() && primaryGeoId.equals(fallbackGeoId)) {
                // Backward compatibility for displays that only define first-person geo.
                visibleRoots = display.resolveGeoVisibleRoots();
            }
        } else {
            visibleRoots = display.resolveGeoVisibleRoots();
        }
        String visibleRoot = visibleRoots.isEmpty() ? null : visibleRoots.get(0);
        Map<String, FoodAnimationController.BonePose> animatedPose = FirstPersonHandEvents.getScriptedBonePose(stack);
        Map<String, FoodAnimationController.BonePose> runtimePose = animatedPose.isEmpty() ? null : animatedPose;

        if (context == ItemDisplayContext.GUI) {
            ResourceLocation guiTexture = resolveGuiTexture(foodId, FoodTextureResolver.resolveDisplayTexture(display));
            float[] shaderColor = RenderSystem.getShaderColor();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            renderGuiSpriteUnlit(poseStack, bufferSource, guiTexture);
            RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);
            return;
        }

        if (firstPerson) {
            FoodFirstPersonGeoRenderer.render(
                    poseStack,
                    bufferSource,
                    light,
                    geoModel,
                    texture,
                    visibleRoot,
                    visibleRoots,
                    runtimePose,
                    stack
            );
            return;
        }

        Map<String, FoodAnimationController.BonePose> contextPose =
                (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                        ? (ENABLE_THIRD_PERSON_BONE_ANIMATION ? runtimePose : null)
                        : null;
        if ((context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                && (contextPose == null || contextPose.isEmpty())
                && thirdPersonPoseMissLogBudget > 0) {
            thirdPersonPoseMissLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Third-person item render without runtime pose: item={}, ctx={}, visibleRoot={}",
                    foodId, context, visibleRoot);
        }

        poseStack.pushPose();
        poseStack.translate(0.5f, 1.5f, 0.5f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        applyContextTransform(context, poseStack);
        applyThirdPersonGripTransform(context, poseStack, geoModel, contextPose);
        applyThirdPersonFineOffset(context, poseStack);
        var buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        boolean renderedByPart = false;
        if (!visibleRoots.isEmpty()) {
            for (String root : visibleRoots) {
                if (root == null || root.isBlank()) {
                    continue;
                }
                renderedByPart |= geoModel.renderPart(poseStack, buffer, light, overlay, root, contextPose);
            }
        } else if (visibleRoot != null && !visibleRoot.isBlank()) {
            renderedByPart = geoModel.renderPart(poseStack, buffer, light, overlay, visibleRoot, contextPose);
        }
        if (!renderedByPart) {
            geoModel.render(poseStack, buffer, light, overlay, contextPose);
        }
        poseStack.popPose();

        if (rendererLogBudget > 0) {
            rendererLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Geo item render: item={}, baseItem={}, ctx={}, texture={}, visibleRoots={}, renderedByPart={}, poseBones={}",
                    foodId, registryItemId, context, texture, visibleRoots, renderedByPart, animatedPose.size());
        }
    }

    private static void renderGuiSpriteUnlit(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation texture
    ) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.5f, 0.0f);
        var pose = poseStack.last().pose();
        var buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        int fullBright = 0x00F000F0;
        int overlay = OverlayTexture.NO_OVERLAY;
        buffer.addVertex(pose, -0.5f, -0.5f, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(0.0f, 1.0f)
                .setOverlay(overlay)
                .setLight(fullBright)
                .setNormal(0.0f, 0.0f, 1.0f);
        buffer.addVertex(pose, -0.5f, 0.5f, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(0.0f, 0.0f)
                .setOverlay(overlay)
                .setLight(fullBright)
                .setNormal(0.0f, 0.0f, 1.0f);
        buffer.addVertex(pose, 0.5f, 0.5f, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(1.0f, 0.0f)
                .setOverlay(overlay)
                .setLight(fullBright)
                .setNormal(0.0f, 0.0f, 1.0f);
        buffer.addVertex(pose, 0.5f, -0.5f, 0.0f)
                .setColor(255, 255, 255, 255)
                .setUv(1.0f, 1.0f)
                .setOverlay(overlay)
                .setLight(fullBright)
                .setNormal(0.0f, 0.0f, 1.0f);
        poseStack.popPose();
    }

    private static void applyContextTransform(ItemDisplayContext context, PoseStack poseStack) {
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(35.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-25.0f));
                poseStack.scale(0.82f, 0.82f, 0.82f);
            }
            case GROUND -> poseStack.scale(0.50f, 0.50f, 0.50f);
            case FIXED -> poseStack.scale(0.70f, 0.70f, 0.70f);
            case THIRD_PERSON_RIGHT_HAND -> {
                poseStack.translate(0.0f, -0.04f, -0.08f);
                poseStack.mulPose(Axis.XP.rotationDegrees(-8.0f));
                poseStack.scale(0.72f, 0.72f, 0.72f);
            }
            case THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0f, -0.04f, -0.08f);
                poseStack.mulPose(Axis.XP.rotationDegrees(-8.0f));
                poseStack.scale(0.72f, 0.72f, 0.72f);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.translate(-0.08f, -0.10f, -0.10f);
                poseStack.mulPose(Axis.YP.rotationDegrees(8.0f));
                poseStack.scale(0.72f, 0.72f, 0.72f);
            }
            default -> {
            }
        }
    }

    private static void applyThirdPersonGripTransform(
            ItemDisplayContext context,
            PoseStack poseStack,
            FoodGeoModel model,
            @Nullable Map<String, FoodAnimationController.BonePose> animatedPose
    ) {
        if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            return;
        }
        String gripNode = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? "righthand_pos" : "lefthand_pos";
        List<FoodGeoModel.Part> nodePath = model.getPath(gripNode);
        if (nodePath == null) {
            gripNode = context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? "righthand" : "lefthand";
            nodePath = model.getPath(gripNode);
        }
        if (nodePath == null || nodePath.isEmpty()) {
            return;
        }

        poseStack.translate(0.0f, 1.5f, 0.0f);
        applyInverseNodePathTransform(poseStack, nodePath, animatedPose);
        poseStack.translate(0.0f, -1.5f, 0.0f);
        if (thirdPersonGripLogBudget > 0) {
            thirdPersonGripLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Third-person grip inverse applied: ctx={}, node={}", context, gripNode);
        }
    }

    private static void applyThirdPersonFineOffset(ItemDisplayContext context, PoseStack poseStack) {
        if (context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.translate(THIRD_PERSON_RIGHT_FINE_X, 0.0f, 0.0f);
            return;
        }
        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) {
            poseStack.translate(THIRD_PERSON_LEFT_FINE_X, 0.0f, 0.0f);
        }
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
        if (animatedPose != null && part.getName() != null) {
            FoodAnimationController.BonePose pose = animatedPose.get(part.getName());
            if (pose != null) {
                x += pose.position().x();
                y -= pose.position().y();
                z += pose.position().z();
                xRot += (float) Math.toRadians(pose.rotationDeg().x());
                yRot += (float) Math.toRadians(pose.rotationDeg().y());
                zRot += (float) Math.toRadians(pose.rotationDeg().z());
            }
        }
        return new PartTransform(x, y, z, xRot, yRot, zRot);
    }

    private static ResourceLocation resolveGuiTexture(ResourceLocation itemId, ResourceLocation fallback) {
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "textures/item/" + itemId.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(icon).isPresent()) {
            return icon;
        }
        return fallback;
    }

    private record PartTransform(
            float x,
            float y,
            float z,
            float xRot,
            float yRot,
            float zRot
    ) {
    }

}
