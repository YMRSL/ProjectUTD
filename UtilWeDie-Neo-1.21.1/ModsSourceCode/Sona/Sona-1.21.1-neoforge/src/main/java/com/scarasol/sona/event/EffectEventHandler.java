package com.scarasol.sona.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.scarasol.sona.SonaMod;
import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import com.scarasol.sona.client.renderer.PositionIndicatorRenderer;
import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.entity.SoundDecoy;
import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.sona.manager.InfectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = SonaMod.MODID)
public class EffectEventHandler {

    public static float tick = 0;

    @SubscribeEvent
    public static void entityTargeting(LivingEvent.LivingVisibilityEvent event) {
        LivingEntity livingEntity = event.getEntity();
        Entity lookingEntity = event.getLookingEntity();
        if (livingEntity == null || lookingEntity == null) {
            return;
        }
        if (lookingEntity instanceof Mob entity_buffer) {
            LivingEntity originalTarget = entity_buffer.getTarget();
            BlockPos targetPos = BlockPos.containing(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
            BlockPos mobPos = BlockPos.containing(entity_buffer.getX(), entity_buffer.getY(), entity_buffer.getZ());
            if (SonaEventHooks.shouldCheckNeutrality(entity_buffer, livingEntity)) {
                return;
            }
            // 缓存 followRange 属性值，避免在同一次事件中重复查 AttributeMap（每次 LivingVisibilityEvent 触发均复用此值）
            double followRange = entity_buffer.getAttributeValue(Attributes.FOLLOW_RANGE);
            if (entity_buffer instanceof Enemy && !(entity_buffer instanceof NeutralMob) && !(livingEntity instanceof SoundDecoy) && livingEntity.hasEffect(SonaMobEffects.EXPOSURE) && (originalTarget == null || !originalTarget.isAlive())) {
                double distance = targetPos.distSqr(mobPos);
                double r = followRange * (livingEntity.getEffect(SonaMobEffects.EXPOSURE).getAmplifier() + 1) * 0.3;
                if (distance < r * r && !livingEntity.isAlliedTo(entity_buffer)) {
                    if (!(livingEntity instanceof Player player) || !player.getAbilities().instabuild) {
                        entity_buffer.setTarget(livingEntity);
                        return;
                    }
                }
            }
            if (livingEntity.hasEffect(SonaMobEffects.CAMOUFLAGE) && !livingEntity.equals(originalTarget)) {
                double distance = targetPos.distSqr(mobPos);
                // 用位移代替 Math.pow(2, n)，amplifier 为小整数时精确且更快
                double r = followRange / (double)(1 << (livingEntity.getEffect(SonaMobEffects.CAMOUFLAGE).getAmplifier() + 1));
                if (distance > r * r) {
                    event.modifyVisibility(0);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void constraintPlayer(InputEvent.MouseButton.Pre event) {
        Player entity = Minecraft.getInstance().player;
        if (entity != null && Minecraft.getInstance().screen == null) {
            if (event.getAction() == 1 && (entity.hasEffect(SonaMobEffects.STUN) || (entity.hasEffect(SonaMobEffects.SLIMINESS) && entity.hasEffect(SonaMobEffects.FROST)))) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onAttacked(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.hasEffect(SonaMobEffects.STUN)) {
            event.setAmount((float) (event.getAmount() * CommonConfig.STUN_DAMAGE_MULTIPLIER.get()));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderInfectionOverlayPost(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player != null) {
            PositionIndicatorRenderer.renderGuiIndicators(event.getGuiGraphics(), minecraft.getTimer().getGameTimeDeltaPartialTick(false));
            if (player.hasEffect(SonaMobEffects.INFECTION) || tick > 0) {
                PoseStack poseStack = event.getGuiGraphics().pose();
                Minecraft mc = Minecraft.getInstance();
                int width = mc.getWindow().getGuiScaledWidth();
                int height = mc.getWindow().getGuiScaledHeight();

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1, 1, 1, tick);
                poseStack.pushPose();
                event.getGuiGraphics().blit(
                        ResourceLocation.fromNamespaceAndPath(SonaMod.MODID, "textures/screens/effects/infection.png"),
                        0, 0, 0, 0,
                        width, height,
                        width, height
                );
                poseStack.popPose();
                RenderSystem.disableBlend();
                RenderSystem.setShaderColor(1, 1, 1, 1);
            }
            if (!player.hasEffect(SonaMobEffects.INFECTION)) {
                tick = Math.max(0, tick - 0.01f);
            } else {
                tick = Math.min(tick + 0.01f, 1);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onRenderExposureHints(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        PositionIndicatorRenderer.renderWorldHalos(event.getPoseStack(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.hasEffect(SonaMobEffects.MAIM)) {
            float proc = Math.max(1 - 0.15f * (livingEntity.getEffect(SonaMobEffects.MAIM).getAmplifier() + 1), 0);
            event.setAmount(proc * event.getAmount());
        }
    }

}
