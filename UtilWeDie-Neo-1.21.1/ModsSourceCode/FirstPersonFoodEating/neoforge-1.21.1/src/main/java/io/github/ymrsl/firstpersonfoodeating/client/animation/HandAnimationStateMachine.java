package io.github.ymrsl.firstpersonfoodeating.client.animation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public final class HandAnimationStateMachine {
    private static final float TICK_SECONDS = 1.0f / 20.0f;

    private final RandomSource random = RandomSource.create();

    private AnimationProfile activeProfile;
    private AnimationProfile pendingProfile;
    private ResourceLocation trackedItemKey;
    private AnimationState state = AnimationState.STATIC_IDLE;
    private float stateTime;
    private float globalTime;
    private int idleVariant;
    private int useVariant = 1;
    private boolean inspectRequested;
    private boolean wasUsing;

    public void requestInspect() {
        inspectRequested = true;
    }

    public void reset() {
        activeProfile = null;
        pendingProfile = null;
        trackedItemKey = null;
        state = AnimationState.STATIC_IDLE;
        stateTime = 0.0f;
        globalTime = 0.0f;
        idleVariant = 0;
        useVariant = 1;
        inspectRequested = false;
        wasUsing = false;
    }

    public void tick(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        AnimationProfile profile = AnimationProfileRegistry.resolve(stack);
        if (profile == null) {
            reset();
            return;
        }

        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemKey == null) {
            reset();
            return;
        }

        if (activeProfile == null) {
            activeProfile = profile;
            trackedItemKey = itemKey;
            idleVariant = random.nextInt(profile.idleVariants());
            useVariant = 1;
            transition(AnimationState.DRAW);
        } else if (!itemKey.equals(trackedItemKey)) {
            pendingProfile = profile;
            trackedItemKey = itemKey;
            transition(AnimationState.PUT_AWAY);
        } else {
            if (!(state == AnimationState.PUT_AWAY && pendingProfile != null)) {
                activeProfile = profile;
            }
        }

        boolean usingMainHand = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND;
        boolean sprinting = player.isSprinting() && player.onGround() && !usingMainHand;

        if (inspectRequested && (state == AnimationState.STATIC_IDLE || state == AnimationState.DRAW)) {
            transition(AnimationState.INSPECT);
            inspectRequested = false;
        }

        if (usingMainHand && state != AnimationState.USE && state != AnimationState.USE_END && state != AnimationState.PUT_AWAY) {
            useVariant = 1 + random.nextInt(activeProfile.useVariants());
            transition(AnimationState.USE);
        }

        if (!usingMainHand && wasUsing && state == AnimationState.USE) {
            transition(AnimationState.USE_END);
        }

        if (sprinting && state == AnimationState.STATIC_IDLE) {
            transition(AnimationState.RUN_START);
        }

        if (!sprinting && state == AnimationState.RUN) {
            transition(AnimationState.RUN_END);
        }

        stateTime += TICK_SECONDS;
        globalTime += TICK_SECONDS;
        advance(usingMainHand, sprinting);
        wasUsing = usingMainHand;
    }

    public HandPose samplePose(float partialTick, float equipProgress, float swingProgress) {
        if (activeProfile == null) {
            return HandPose.IDENTITY;
        }

        float animTime = stateTime + partialTick * TICK_SECONDS;
        float swing = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float x = -0.015f * swing;
        float y = 0.010f * swing;
        float z = -0.020f * equipProgress;
        float pitch = -4.0f * swing;
        float yaw = 4.0f * swing;
        float roll = -2.0f * swing;
        float scale = 1.0f;

        switch (state) {
            case DRAW -> {
                float p = easeOut(progress(animTime, activeProfile.drawDuration()));
                float q = 1.0f - p;
                x += 0.18f * q;
                y -= 0.30f * q;
                z += 0.18f * q;
                pitch += 26.0f * q;
                yaw -= 22.0f * q;
                roll += 8.0f * q;
            }
            case STATIC_IDLE -> {
                float amp = 1.0f + idleVariant * 0.07f;
                float breathe = Mth.sin(globalTime * 1.5f + idleVariant * 0.7f);
                float sway = Mth.sin(globalTime * 0.9f + idleVariant * 1.2f);
                x += 0.010f * sway * amp;
                y += 0.008f * breathe * amp;
                pitch += 1.4f * breathe * amp;
                yaw += 1.8f * sway * amp;
                roll += 0.8f * Mth.sin(globalTime * 1.1f + idleVariant);
            }
            case INSPECT -> {
                float p = easeInOut(progress(animTime, activeProfile.inspectDuration()));
                float hold = Mth.sin((float) Math.PI * p);
                float wiggle = Mth.sin(globalTime * 4.0f) * 2.4f;
                x += 0.07f * hold;
                y += 0.02f + 0.01f * hold;
                z += 0.06f * hold;
                pitch += 8.0f * hold;
                yaw += 24.0f * hold + wiggle;
                roll -= 8.0f * hold;
            }
            case USE -> {
                float cycle = animTime / activeProfile.useDuration() * (float) (Math.PI * 2.0);
                float amp = (0.12f + 0.02f * useVariant);
                float up = Math.abs(Mth.sin(cycle));
                x -= 0.03f;
                y -= amp * up;
                z += 0.08f * up;
                pitch += (26.0f + 2.0f * useVariant) * up;
                yaw += 4.0f * Mth.sin(cycle);
                roll += 2.0f * Mth.sin(cycle * 0.5f);
                scale += 0.01f * up;
            }
            case USE_END -> {
                float p = progress(animTime, activeProfile.useEndDuration());
                float q = 1.0f - p;
                y -= 0.08f * q;
                z += 0.06f * q;
                pitch += 14.0f * q;
                yaw += 2.0f * q;
            }
            case PUT_AWAY -> {
                float p = easeIn(progress(animTime, activeProfile.putAwayDuration()));
                x += 0.11f * p;
                y -= 0.30f * p;
                z += 0.22f * p;
                pitch += 20.0f * p;
                yaw -= 18.0f * p;
                roll += 10.0f * p;
            }
            case RUN_START -> {
                float blend = progress(animTime, activeProfile.runStartDuration());
                RunPose run = runPose(globalTime);
                x += run.x() * blend;
                y += run.y() * blend;
                z += run.z() * blend;
                pitch += run.pitch() * blend;
                yaw += run.yaw() * blend;
                roll += run.roll() * blend;
            }
            case RUN -> {
                RunPose run = runPose(globalTime);
                x += run.x();
                y += run.y();
                z += run.z();
                pitch += run.pitch();
                yaw += run.yaw();
                roll += run.roll();
            }
            case RUN_END -> {
                float blend = 1.0f - progress(animTime, activeProfile.runEndDuration());
                RunPose run = runPose(globalTime);
                x += run.x() * blend;
                y += run.y() * blend;
                z += run.z() * blend;
                pitch += run.pitch() * blend;
                yaw += run.yaw() * blend;
                roll += run.roll() * blend;
            }
        }

        return new HandPose(x, y, z, pitch, yaw, roll, scale, scale, scale);
    }

    private void advance(boolean usingMainHand, boolean sprinting) {
        switch (state) {
            case DRAW -> {
                if (stateTime >= activeProfile.drawDuration()) {
                    transition(AnimationState.STATIC_IDLE);
                }
            }
            case STATIC_IDLE -> {
                if (stateTime >= 2.8f) {
                    idleVariant = random.nextInt(activeProfile.idleVariants());
                    stateTime = 0.0f;
                }
            }
            case INSPECT -> {
                if (stateTime >= activeProfile.inspectDuration()) {
                    transition(AnimationState.STATIC_IDLE);
                }
            }
            case USE -> {
                if (!usingMainHand) {
                    transition(AnimationState.USE_END);
                } else if (stateTime >= activeProfile.useDuration()) {
                    stateTime = 0.0f;
                }
            }
            case USE_END -> {
                if (stateTime >= activeProfile.useEndDuration()) {
                    transition(AnimationState.STATIC_IDLE);
                }
            }
            case PUT_AWAY -> {
                if (stateTime >= activeProfile.putAwayDuration()) {
                    if (pendingProfile != null) {
                        activeProfile = pendingProfile;
                        pendingProfile = null;
                        idleVariant = random.nextInt(activeProfile.idleVariants());
                        useVariant = 1;
                        transition(AnimationState.DRAW);
                    } else {
                        transition(AnimationState.STATIC_IDLE);
                    }
                }
            }
            case RUN_START -> {
                if (stateTime >= activeProfile.runStartDuration()) {
                    transition(AnimationState.RUN);
                }
            }
            case RUN -> {
                if (!sprinting) {
                    transition(AnimationState.RUN_END);
                } else if (stateTime >= 0.45f) {
                    stateTime = 0.0f;
                }
            }
            case RUN_END -> {
                if (stateTime >= activeProfile.runEndDuration()) {
                    transition(AnimationState.STATIC_IDLE);
                }
            }
        }
    }

    private void transition(AnimationState next) {
        state = next;
        stateTime = 0.0f;
    }

    private static float progress(float time, float duration) {
        if (duration <= 0.0f) {
            return 1.0f;
        }
        return Mth.clamp(time / duration, 0.0f, 1.0f);
    }

    private static float easeOut(float t) {
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    private static float easeIn(float t) {
        return t * t * t;
    }

    private static float easeInOut(float t) {
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        }
        float f = -2.0f * t + 2.0f;
        return 1.0f - (f * f * f) * 0.5f;
    }

    private static RunPose runPose(float time) {
        float bob = Mth.sin(time * 14.0f);
        float bobFast = Mth.sin(time * 28.0f + 0.6f);
        float up = Math.abs(bob);
        return new RunPose(
                0.045f * bob,
                0.042f * up,
                0.028f + 0.010f * up,
                11.0f * up,
                6.0f * bob,
                9.0f * bobFast
        );
    }

    private record RunPose(float x, float y, float z, float pitch, float yaw, float roll) {
    }

    public record HandPose(
            float x,
            float y,
            float z,
            float pitch,
            float yaw,
            float roll,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
        public static final HandPose IDENTITY = new HandPose(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    }
}
