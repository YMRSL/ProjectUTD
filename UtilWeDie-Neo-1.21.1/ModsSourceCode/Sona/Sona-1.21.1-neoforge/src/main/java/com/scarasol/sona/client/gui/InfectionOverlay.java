package com.scarasol.sona.client.gui;

import com.scarasol.sona.SonaMod;
import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import com.scarasol.sona.manager.InfectionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = SonaMod.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class InfectionOverlay {

    private static final ResourceLocation N1 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/n1.png");
    private static final ResourceLocation G2 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/g2.png");
    private static final ResourceLocation G3 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/g3.png");
    private static final ResourceLocation G4 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/g4.png");
    private static final ResourceLocation G5 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/g5.png");

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        int posX = getXOffset(width);
        int posY = getYOffset(height);
        Player entity = Minecraft.getInstance().player;
        if (entity != null) {
            if (CommonConfig.INFECTION_OPEN.get() && entity instanceof ILivingEntityAccessor player && !(entity.isCreative() || entity.isSpectator())) {
                ResourceLocation texture;
                if (InfectionManager.getInfection(player) <= 40) {
                    texture = N1;
                } else if (InfectionManager.getInfection(player) <= 70) {
                    texture = G2;
                } else if (InfectionManager.getInfection(player) <= 90) {
                    texture = G3;
                } else if (InfectionManager.getInfection(player) < 100) {
                    texture = G4;
                } else {
                    texture = G5;
                }
                event.getGuiGraphics().blit(texture, posX, posY, 0, 0, 32, 32, 32, 32);
                // 用户定制：在感染头像处显示感染数值。
                if (CommonConfig.INFECTION_SHOW_VALUE.get()) {
                    int infection = (int) InfectionManager.getInfection(player);
                    String text = String.valueOf(infection);
                    net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
                    int color = infection > 75 ? 0xFFFF5555 : (infection > 40 ? 0xFFFFD040 : 0xFFFFFFFF);
                    int tx = posX + 16 - font.width(text) / 2;   // 头像水平居中
                    int ty = posY + 32 - 7;                       // 头像底部
                    event.getGuiGraphics().drawString(font, text, tx, ty, color, true);
                }
            }
        }
    }

    public static int getXOffset(int scaledWidth) {
        return switch (CommonConfig.INFECTION_OVERLAY_PRESET.get()) {
            case 1 -> 0;
            case 2 -> scaledWidth - 32;
            case 3 -> scaledWidth / 2 - 16;
            default -> CommonConfig.INFECTION_X_OFFSET.get();
        };
    }

    public static int getYOffset(int scaledHeight) {
        return switch (CommonConfig.INFECTION_OVERLAY_PRESET.get()) {
            case 1, 2 -> scaledHeight - 32;
            case 3 -> scaledHeight - 65;
            default -> scaledHeight - CommonConfig.INFECTION_Y_OFFSET.get();
        };
    }
}
