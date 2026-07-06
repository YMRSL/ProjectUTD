package io.github.ymrsl.firstpersonfoodeating.client;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.config.ModCommonConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class ConsumeFlashOverlayEvents {
    private static final float FLASH_MAX_ALPHA = 0.72f;
    private static final int FLASH_FADE_TICKS = 30; // 1.5s at 20 TPS
    private static long flashStartGameTick = Long.MIN_VALUE;

    private ConsumeFlashOverlayEvents() {
    }

    public static void triggerOnConsume(Player sourcePlayer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (!shouldAcceptTrigger(minecraft, sourcePlayer)) {
            return;
        }
        flashStartGameTick = minecraft.level.getGameTime();
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float alpha = computeCurrentAlpha(minecraft, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        if (alpha <= 0.001f) {
            return;
        }

        int alphaByte = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        int color = (alphaByte << 24) | 0x00FFFFFF;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        event.getGuiGraphics().fill(0, 0, width, height, color);
    }

    private static float computeCurrentAlpha(Minecraft minecraft, float partialTick) {
        if (flashStartGameTick == Long.MIN_VALUE || minecraft.level == null) {
            return 0.0f;
        }
        float elapsedTicks = (minecraft.level.getGameTime() - flashStartGameTick) + partialTick;
        if (elapsedTicks < 0.0f) {
            elapsedTicks = 0.0f;
        }
        float progress = elapsedTicks / (float) FLASH_FADE_TICKS;
        if (progress >= 1.0f) {
            flashStartGameTick = Long.MIN_VALUE;
            return 0.0f;
        }
        return FLASH_MAX_ALPHA * (1.0f - smoothStep(progress));
    }

    private static float smoothStep(float t) {
        float x = Mth.clamp(t, 0.0f, 1.0f);
        return x * x * (3.0f - 2.0f * x);
    }

    private static boolean shouldAcceptTrigger(Minecraft minecraft, Player sourcePlayer) {
        if (sourcePlayer == null) {
            return true;
        }
        if (ModCommonConfig.CONSUME_FLASH_AFFECTS_NEARBY_PLAYERS.get()) {
            return true;
        }
        return minecraft.player != null && sourcePlayer.getUUID().equals(minecraft.player.getUUID());
    }
}
