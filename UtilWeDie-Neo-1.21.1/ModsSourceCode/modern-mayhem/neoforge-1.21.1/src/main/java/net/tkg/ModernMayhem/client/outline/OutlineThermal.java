package net.tkg.ModernMayhem.client.outline;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tkg.ModernMayhem.client.event.RenderNVGShader;
import net.tkg.ModernMayhem.client.event.RenderTVGShader;
import net.tkg.ModernMayhem.client.outline.render.OutlineRenderer;
import net.tkg.ModernMayhem.server.item.curios.facewear.NVGGogglesItem;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

public class OutlineThermal {
    public static void setupOutlines() {
        OutlineThermal.registerFacewearModeListener();
        OutlineRenderer.setUseBlackOutline(false);
        OutlineRenderer.setOutlinePredicate(entity -> {
            if (!(entity instanceof LivingEntity)) {
                return false;
            }
            if (entity instanceof ItemEntity) {
                return false;
            }
            if (entity instanceof ThrowableProjectile) {
                return false;
            }
            if (entity instanceof HangingEntity) {
                return false;
            }
            return !(entity instanceof ArmorStand);
        });
    }

    public static void registerHelmetModeListener() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            OutlineThermal.updateModeFromHelmet();
        });
    }

    public static void updateModeFromHelmet() {
        if (Minecraft.getInstance().player == null) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OFF);
            return;
        }
        ItemStack head = Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD);
        OutlineRenderer.setOutlineColorProvider(null);
        if (head.isEmpty()) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OFF);
        } else if (head.getItem() == Items.IRON_HELMET) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OUTLINE);
            OutlineRenderer.setOutlineColor(1.0f, 1.0f, 1.0f, 2.0f);
        } else if (head.getItem() == Items.DIAMOND_HELMET) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OVERLAY);
            OutlineRenderer.setOutlineColor(1.0f, 1.0f, 1.0f, 2.0f);
        } else if (head.getItem() == Items.GOLDEN_HELMET) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OUTLINE);
            float hue = (float)(System.currentTimeMillis() % 2000L) / 2000.0f;
            int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            float r = (float)(rgb >> 16 & 0xFF) / 255.0f;
            float g = (float)(rgb >> 8 & 0xFF) / 255.0f;
            float b = (float)(rgb & 0xFF) / 255.0f;
            OutlineRenderer.setOutlineColor(r, g, b, 2.0f);
        } else if (head.getItem() == Items.NETHERITE_HELMET) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OUTLINE);
            OutlineRenderer.setOutlineColor(1.0f, 1.0f, 1.0f, 2.0f);
            OutlineRenderer.setOutlineColorProvider(entity -> {
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity)entity;
                    float health = living.getHealth();
                    float max = living.getMaxHealth();
                    float pct = Math.max(0.0f, Math.min(1.0f, health / max));
                    return Color.HSBtoRGB(pct * 0.33f, 1.0f, 1.0f);
                }
                return -1;
            });
        } else {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OFF);
        }
    }

    public static void registerFacewearModeListener() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            OutlineThermal.updateModeFromFacewear();
        });
    }

    public static void updateModeFromFacewear() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OFF);
            return;
        }
        OutlineRenderer.setOutlineColorProvider(null);
        if (RenderTVGShader.isThermalActive()) {
            // 去掉第一人称门控 —— 热成像的生物纯白渲染在第三人称也生效。
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OVERLAY);
            OutlineRenderer.setOutlineColor(1.0f, 1.0f, 1.0f, 2.0f);
        } else if (RenderNVGShader.isNvActive() && Minecraft.getInstance().options.getCameraType().isFirstPerson() && OutlineThermal.isCotiEnabledOnPlayer(player)) {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OUTLINE);
            OutlineRenderer.setOutlineColor(1.0f, 1.0f, 1.0f, 2.0f);
        } else {
            OutlineRenderer.setRenderMode(OutlineRenderer.RenderMode.OFF);
        }
    }

    private static boolean isCotiEnabledOnPlayer(LocalPlayer player) {
        ItemStack stack = CuriosUtil.getFaceWearItem((Player)player);
        return stack.getItem() instanceof NVGGogglesItem && GenericSpecialGogglesItem.isCotiEnabled(stack);
    }
}

