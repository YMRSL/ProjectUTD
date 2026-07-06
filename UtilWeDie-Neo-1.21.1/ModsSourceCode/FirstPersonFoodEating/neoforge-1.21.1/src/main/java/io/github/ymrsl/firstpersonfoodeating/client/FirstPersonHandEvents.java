package io.github.ymrsl.firstpersonfoodeating.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.animation.AnimationProfileRegistry;
import io.github.ymrsl.firstpersonfoodeating.client.animation.HandAnimationStateMachine;
import io.github.ymrsl.firstpersonfoodeating.client.animation.HandAnimationStateMachine.HandPose;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAssetsManager;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodTextureResolver;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodScriptedHandStateMachine;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo.FoodFirstPersonGeoRenderer;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo.FoodGeoModel;
import java.util.Map;
import io.github.ymrsl.firstpersonfoodeating.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class FirstPersonHandEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final HandAnimationStateMachine LEGACY_STATE_MACHINE = new HandAnimationStateMachine();
    private static final FoodScriptedHandStateMachine SCRIPTED_STATE_MACHINE = new FoodScriptedHandStateMachine();
    private static final float STATIC_BOB_BLEND_IN_RATE = 7.0f;
    private static final float STATIC_BOB_BLEND_OUT_RATE = 9.0f;
    private static boolean scriptedActive = false;
    private static boolean extensionLogged = false;
    private static int idleBobLogBudget = 10;
    private static float staticIdleBobBlend = 0.0f;
    private static long staticIdleBobLastNs = -1L;

    private FirstPersonHandEvents() {
    }

    @SubscribeEvent
    public static void onOffhandRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handleOffhandInteraction(event.getEntity(), event.getHand());
    }

    @SubscribeEvent
    public static void onOffhandRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleOffhandInteraction(event.getEntity(), event.getHand());
    }

    // Off hand placing/using an item while a scripted food sits in the main hand would otherwise
    // freeze the food in a "drooping arms" pose (there is no off-hand food animation). Mirror TaCZ:
    // retract the food (put_away) and quickly re-draw it, covering the off-hand action.
    private static void handleOffhandInteraction(Player player, InteractionHand hand) {
        if (hand != InteractionHand.OFF_HAND || !scriptedActive) {
            return;
        }
        if (player == null || player != Minecraft.getInstance().player) {
            return;
        }
        if (player.getOffhandItem().isEmpty()) {
            return;
        }
        if (!SCRIPTED_STATE_MACHINE.handles(player.getMainHandItem())) {
            return;
        }
        SCRIPTED_STATE_MACHINE.triggerOffhandRetractRedraw();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            LEGACY_STATE_MACHINE.reset();
            SCRIPTED_STATE_MACHINE.reset();
            scriptedActive = false;
            resetStaticIdleBobBlend();
            return;
        }

        FoodCreativeTabEvents.flushPendingCreativeTabsRebuild();

        if (minecraft.isPaused()) {
            // Keep current animation pose while paused. Do not advance or reset.
            return;
        }

        if (!extensionLogged) {
            extensionLogged = true;
            try {
                var ext = IClientItemExtensions.of(ModItems.PACK_FOOD.get());
                var renderer = ext.getCustomRenderer();
                LOGGER.info("[firstpersonfoodeating] Extension check: item=pack_food renderer={}",
                        renderer == null ? "null" : renderer.getClass().getName());
            } catch (Exception ex) {
                LOGGER.warn("[firstpersonfoodeating] Extension check failed for pack_food", ex);
            }
        }

        while (ClientKeyMappings.INSPECT.consumeClick()) {
            LEGACY_STATE_MACHINE.requestInspect();
            SCRIPTED_STATE_MACHINE.requestInspect();
        }

        scriptedActive = SCRIPTED_STATE_MACHINE.tick(minecraft.player);
        if (scriptedActive) {
            LEGACY_STATE_MACHINE.reset();
        } else {
            LEGACY_STATE_MACHINE.tick(minecraft.player);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (scriptedActive && SCRIPTED_STATE_MACHINE.isSwitching()) {
            var display = SCRIPTED_STATE_MACHINE.getActiveDisplay();
            FoodGeoModel geoModel = display == null ? null
                    : FoodAssetsManager.get().getGeoModel(display.resolveGeoId()).orElse(null);
            if (display != null && geoModel != null) {
                FoodFirstPersonGeoRenderer.render(
                        event.getPoseStack(),
                        event.getMultiBufferSource(),
                        event.getPackedLight(),
                        geoModel,
                        FoodTextureResolver.resolveDisplayTexture(display),
                        display.resolveGeoVisibleRoot(),
                        display.resolveGeoVisibleRoots(),
                        SCRIPTED_STATE_MACHINE.sampleBonePose(),
                        event.getItemStack()
                );
                event.setCanceled(true);
                return;
            }
        }

        if (scriptedActive && SCRIPTED_STATE_MACHINE.handles(event.getItemStack())) {
            // Scripted mode drives model bones directly in BEWLR. Do not add extra
            // event-level offsets here, otherwise sprint/use motion can push the item
            // (and player arm) out of frame.
            return;
        }

        if (!AnimationProfileRegistry.hasProfile(event.getItemStack())) {
            return;
        }

        HandPose pose = LEGACY_STATE_MACHINE.samplePose(
                event.getPartialTick(),
                event.getEquipProgress(),
                event.getSwingProgress()
        );
        applyPose(event.getPoseStack(), pose);
    }

    private static void applyPose(PoseStack poseStack, HandPose pose) {
        poseStack.translate(pose.x(), pose.y(), pose.z());
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
        poseStack.scale(pose.scaleX(), pose.scaleY(), pose.scaleZ());
    }

    private static void applyStaticIdleVanillaBob(PoseStack poseStack, LocalPlayer player, float partialTick) {
        applyStaticIdleVanillaBob(poseStack, player, partialTick, 1.0f);
    }

    private static void applyStaticIdleVanillaBob(
            PoseStack poseStack,
            LocalPlayer player,
            float partialTick,
            float weight
    ) {
        if (weight <= 0.0001f) {
            return;
        }
        float walk = player.walkDistO + (player.walkDist - player.walkDistO) * partialTick;
        float xSwing = Mth.sin(walk * (float) Math.PI);
        float ySwing = Mth.cos(walk * (float) Math.PI - 0.2f);

        // Keep one low-frequency vanilla-like sway layer to avoid stacked fast jitter.
        poseStack.translate(-xSwing * 0.010f * weight, Math.abs(ySwing) * 0.006f * weight, 0.0f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-xSwing * 1.15f * weight));
        poseStack.mulPose(Axis.XP.rotationDegrees(Math.abs(ySwing) * 0.45f * weight));

        if (!player.onGround() || player.getAbilities().flying || player.isFallFlying()) {
            float t = (player.tickCount + partialTick) * 0.22f;
            float airBob = Mth.sin(t) * 0.0035f;
            poseStack.translate(0.0f, airBob * weight, 0.0f);
            poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.cos(t * 0.7f) * 0.45f * weight));
        }

        if (idleBobLogBudget > 0) {
            idleBobLogBudget--;
            LOGGER.info("[firstpersonfoodeating] First-person static_idle bob applied: walk={}", walk);
        }
    }

    public static Map<String, FoodAnimationController.BonePose> getScriptedBonePose(ItemStack stack) {
        if (stack.isEmpty()) {
            return Map.of();
        }
        if (!SCRIPTED_STATE_MACHINE.handles(stack)) {
            return Map.of();
        }
        return SCRIPTED_STATE_MACHINE.sampleBonePose();
    }

    public static String getScriptedDebugSummary() {
        return SCRIPTED_STATE_MACHINE.debugTrackSummary();
    }

    public static void applyStaticIdleBobIfNeeded(ItemStack stack, PoseStack poseStack) {
        if (stack.isEmpty()) {
            resetStaticIdleBobBlend();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            resetStaticIdleBobBlend();
            return;
        }
        if (!SCRIPTED_STATE_MACHINE.handles(stack)) {
            resetStaticIdleBobBlend();
            return;
        }
        boolean targetActive = SCRIPTED_STATE_MACHINE.shouldApplyVanillaStaticIdleBob(player);
        float blend = updateStaticIdleBobBlend(targetActive);
        if (blend <= 0.0001f) {
            return;
        }
        applyStaticIdleVanillaBob(poseStack, player, minecraft.getTimer().getGameTimeDeltaPartialTick(true), blend);
    }

    private static float updateStaticIdleBobBlend(boolean active) {
        long now = System.nanoTime();
        if (staticIdleBobLastNs <= 0L) {
            staticIdleBobLastNs = now;
            staticIdleBobBlend = active ? 1.0f : 0.0f;
            return staticIdleBobBlend;
        }
        float dt = (now - staticIdleBobLastNs) / 1_000_000_000.0f;
        staticIdleBobLastNs = now;
        dt = Mth.clamp(dt, 0.001f, 0.05f);

        float rate = active ? STATIC_BOB_BLEND_IN_RATE : STATIC_BOB_BLEND_OUT_RATE;
        float step = dt * rate;
        staticIdleBobBlend = Mth.approach(staticIdleBobBlend, active ? 1.0f : 0.0f, step);
        return staticIdleBobBlend;
    }

    private static void resetStaticIdleBobBlend() {
        staticIdleBobBlend = 0.0f;
        staticIdleBobLastNs = -1L;
    }
}
