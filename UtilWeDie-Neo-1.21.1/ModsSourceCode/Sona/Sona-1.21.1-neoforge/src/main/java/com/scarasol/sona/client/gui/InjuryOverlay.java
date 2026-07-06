package com.scarasol.sona.client.gui;

import com.scarasol.sona.SonaMod;
import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import com.scarasol.sona.manager.InjuryManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * @author Scarasol
 */
@EventBusSubscriber(modid = SonaMod.MODID, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class InjuryOverlay {

    private static final ResourceLocation BLOOD0 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/blood0.png");
    private static final ResourceLocation BLOOD1 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/blood1.png");
    private static final ResourceLocation BLOOD2 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/blood2.png");
    private static final ResourceLocation BLOOD3 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/blood3.png");
    private static final ResourceLocation BLOOD4 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/blood4.png");
    private static final ResourceLocation BLOOD5 = ResourceLocation.fromNamespaceAndPath("sona", "textures/screens/blood5.png");

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void renderInjury(RenderGuiEvent.Pre event) {
        int w = event.getGuiGraphics().guiWidth();
        int h = event.getGuiGraphics().guiHeight();
        Player entity = Minecraft.getInstance().player;
        if (entity instanceof ILivingEntityAccessor player) {
            double blood = InjuryManager.getInjury(player);
            double goldBlood = blood + InjuryManager.getBandage(player);
            int preset = CommonConfig.INJURY_OVERLAY_PRESET.get();
            int posX;
            int posY;
            if (preset == 4) {
                // 左侧 HUD：与生命条同列，置于「黄血(吸收)+护甲」整列之上，随吸收行数与是否有护甲自适应上移，避开其他 mod(如 thirsty) 的条。
                int left = w / 2 - 91;
                int rows = Math.max(1, Mth.ceil((entity.getMaxHealth() + entity.getAbsorptionAmount()) / 20.0F)); // 生命+吸收占用整行数(20HP/行)
                // 原版护甲条就在生命/吸收块上方一行；有护甲时再让出一行，使血条高于护甲，避免被护甲遮挡。
                int stackRows = rows + (entity.getArmorValue() > 0 ? 1 : 0);
                posX = left - 10;                  // 抵消下方渲染循环的 +10，使首个血滴对齐生命条左缘
                posY = h - 39 - stackRows * 10;    // 整个生命/吸收(/护甲)块之上一行
            } else {
                posX = getXOffset(w);
                posY = getYOffset(h);
                if (preset == 1 || preset == 2 || (preset == 0 && CommonConfig.RISE_UNDERWATER.get())) {
                    if (entity.getAirSupply() < entity.getMaxAirSupply() || entity.getEyeInFluidType().canDrownIn(entity)) {
                        posY -= 9;
                    }
                }
            }
            if (CommonConfig.INJURY_OPEN.get() && !(entity.isCreative() || entity.isSpectator())) {
                for (int i = 0; i < 10; ++i) {
                    int j = (9 - i) * 10;
                    int k = 8;
                    event.getGuiGraphics().blit(BLOOD0, posX + 10 + k * i, posY, 0, 0, 9, 9, 9, 9);
                    if (blood > j + 5) {
                        event.getGuiGraphics().blit(BLOOD1, posX + 10 + k * i, posY, 0, 0, 9, 9, 9, 9);
                    } else if (blood > j) {
                        if (goldBlood > j + 5) {
                            event.getGuiGraphics().blit(BLOOD5, posX + 10 + k * i, posY, 0, 0, 9, 9, 9, 9);
                        } else {
                            event.getGuiGraphics().blit(BLOOD2, posX + 10 + k * i, posY, 0, 0, 9, 9, 9, 9);
                        }
                    } else if (goldBlood > j + 5) {
                        event.getGuiGraphics().blit(BLOOD3, posX + 10 + k * i, posY, 0, 0, 9, 9, 9, 9);
                    } else if (goldBlood > j) {
                        event.getGuiGraphics().blit(BLOOD4, posX + 10 + k * i, posY, 0, 0, 9, 9, 9, 9);
                    }
                }
            }
        }
    }

    public static int getXOffset(int scaledWidth) {
        return switch (CommonConfig.INJURY_OVERLAY_PRESET.get()) {
            case 1, 2 -> scaledWidth / 2;
            case 3 -> 20;
            default -> CommonConfig.INJURY_X_OFFSET.get();
        };
    }

    public static int getYOffset(int scaledHeight) {
        return switch (CommonConfig.INJURY_OVERLAY_PRESET.get()) {
            case 1 -> scaledHeight - 50;
            case 2 -> scaledHeight - 59;
            case 3 -> scaledHeight - 20;
            default -> scaledHeight - CommonConfig.INJURY_Y_OFFSET.get();
        };
    }
}
