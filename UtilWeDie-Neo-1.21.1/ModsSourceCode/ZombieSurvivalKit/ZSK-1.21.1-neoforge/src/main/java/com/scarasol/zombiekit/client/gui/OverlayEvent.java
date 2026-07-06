package com.scarasol.zombiekit.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.mechanics.HeavyMachineGunEntity;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitTags;
import com.scarasol.zombiekit.item.api.ModifiableWeapon;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.item.projectile.MortarShell;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import com.scarasol.zombiekit.item.weapon.parts.ChargingParts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.function.Predicate;

@EventBusSubscriber(modid = ZombieKitMod.MODID, value = Dist.CLIENT)
public class OverlayEvent {

    public static void renderExo(RenderGuiEvent.Pre event) {
        Player entity = Minecraft.getInstance().player;
        if (entity != null && ExoArmor.numberOfSuit(entity) >= 4) {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int posX = width - com.scarasol.zombiekit.config.CommonConfig.EXO_HUD_WIDTH.get();
            int posY = height - com.scarasol.zombiekit.config.CommonConfig.EXO_HUD_HEIGHT.get();
            if (!entity.isSpectator()) {
                event.getGuiGraphics().pose().pushPose();
                ItemStack itemStack = entity.getItemBySlot(EquipmentSlot.CHEST);
                String color;
                switch (ExoArmor.getMode(itemStack)) {
                    case 1 -> color = "blue";
                    case 2 -> color = "red";
                    default -> color = "green";
                }

                event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/basic_component.png"), posX, posY, 0, 0, 112, 35, 112, 35);

                int power = ExoArmor.getPower(itemStack);

                event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/" + power / 100 + ".png"), posX + 80, posY + 12, 0, 0, 5, 8, 5, 8);
                event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/" + power % 100 / 10 + ".png"), posX + 87, posY + 12, 0, 0, 5, 8, 5, 8);
                event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/" + power % 10 + ".png"), posX + 94, posY + 12, 0, 0, 5, 8, 5, 8);


                event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/percent.png"), posX + 101, posY + 12, 0, 0, 5, 8, 5, 8);


                for (int i = 0; power / ((i + 1) * 5) > 0; i++){
                    event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/power.png"), posX + 103 - 5 * i, posY + 21, 0, 0, 3, 8, 3, 8);
                }

                if (ExoArmor.getRadar(itemStack) == 2){
                    event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/radar.png"), posX + 96, posY + 2, 0, 0, 8, 8, 8, 8);
                }else if (ExoArmor.getRadar(itemStack) == 1) {
                    event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/night_vision.png"), posX + 96, posY + 2, 0, 0, 8, 8, 8, 8);
                }

                if (ExoArmor.getFlyMode(itemStack)){
                    event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/fall_fly.png"), posX + 86, posY + 2, 0, 0, 8, 8, 8, 8);
                }

                if (ExoArmor.getReactiveArmor(itemStack) >= 0){
                    event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/reactive_armor_charge.png"), posX + 76, posY + 2, 0, 0, 8, 8, 8, 8);
                    event.getGuiGraphics().blit(ResourceLocation.parse("zombiekit:textures/screens/exo/" + color + "/reactive_armor.png"), posX + 76, posY + 2, 0, 0, 8, 8 * ExoArmor.getReactiveArmor(itemStack) / 160, 8, 8);
                }
                event.getGuiGraphics().pose().popPose();
            }
        }
    }

    public static void renderSum(GuiGraphics guiGraphics, Player player, int posX, int posY, Predicate<ItemStack> predicate) {
        int sum = player.getInventory().items.stream().filter(predicate).mapToInt(ItemStack::getCount).sum();
        boolean flag = false;
        int[] number = new int[4];
        for (int i = 1; i <= 4; i++) {
            number[4 - i] = sum % 10;
            sum -= number[4 - i];
            sum /= 10;
            if (sum == 0)
                break;
        }
        for (int i = 0; i < number.length; i++) {
            int num = number[i];
            if ((num != 0 || i == number.length - 1) && !flag) {
                flag = true;
            }
            if (flag) {
                guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/" + num + ".png"), posX, posY, 0, 0, 6, 10, 6, 10);
                posX += 8;
            }
        }
    }

    public static void renderMortarInfo(GuiGraphics guiGraphics, int posX, int posY, MortarEntity mortarEntity, Player player) {
        guiGraphics.pose().pushPose();
        guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/mortar_bottom.png"), posX, posY, 0, 0, 28, 2, 28, 2);
        guiGraphics.pose().translate(posX + 26 , posY - 2, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotation((float) Math.toRadians(-mortarEntity.getAngle() - 45)));
        guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/mortar.png"), -20, -20, 0, 0, 30, 30, 30, 30);
        guiGraphics.pose().popPose();
        guiGraphics.pose().pushPose();
        posX = posX + 30;
        posY = posY - 28;
        guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/mortar_shell.png"), posX, posY, 0, 0, 22, 20, 22, 20);
        renderSum(guiGraphics, player, posX + 24, posY, itemStack -> itemStack.getItem() instanceof MortarShell);
        guiGraphics.pose().popPose();
    }

    public static void renderHeavyMachineGunInfo(GuiGraphics guiGraphics, int posX, int posY, Player player) {
        guiGraphics.pose().pushPose();
        guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/heavy_machine_gun.png"), posX, posY, 0, 0, 56, 16, 56, 16);
        renderSum(guiGraphics, player, posX + 60, posY + 4, itemStack -> itemStack.is(ZombieKitTags.MACHINE_GUN_AMMO));
        guiGraphics.pose().popPose();
    }

    public static void renderFlamethrowerInfo(GuiGraphics guiGraphics, int posX, int posY, ItemStack itemStack) {
        Flamethrower flamethrower = (Flamethrower) itemStack.getItem();
        double pressure = flamethrower.getPressure(itemStack) / 10000;
        guiGraphics.pose().pushPose();
        guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/flamethrower.png"), posX, posY, 0, 0, 80, 28, 80, 28);
        guiGraphics.pose().translate(posX + 61, posY + 19, 0);
        guiGraphics.pose().mulPose(Axis.ZP.rotation((float) (-Math.PI / 4 + pressure * Math.PI)));
        guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/pointer.png"), -9, -9, 0, 0, 10, 10, 10, 10);
        guiGraphics.pose().popPose();
    }

    public static void renderChargingPartsInfo(GuiGraphics guiGraphics, int posX, int posY, ItemStack itemStack) {
        if (itemStack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
            Item parts = modifiableWeapon.getChargingParts(itemStack);
            Level level = Minecraft.getInstance().level;
            if (parts instanceof ChargingParts chargingParts && chargingParts.isOnCoolDown(itemStack, level.getGameTime())) {
                guiGraphics.pose().pushPose();
                guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/charging_parts_empty.png"), posX + 10, posY - 5, 0, 0, 10, 10, 10, 10);
                double percent = (level.getGameTime() - chargingParts.getCooldownTime(itemStack)) / (double)chargingParts.getChargingTime();
                guiGraphics.blit(ResourceLocation.parse("zombiekit:textures/screens/weapon/charging_parts.png"), posX + 10, posY - 5, 0, 0, 10, (int) Math.round(10 * percent), 10, 10);
                guiGraphics.pose().popPose();
            }

        }

    }

    public static void renderWeaponInfo(RenderGuiEvent.Pre event) {
        Player entity = Minecraft.getInstance().player;
        if (entity != null) {
            int width = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int height = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            int posX = width - 120;
            int posY = height - 15;
            GuiGraphics guiGraphics = event.getGuiGraphics();
            Entity vehicle = entity.getVehicle();
            ItemStack itemStack = entity.getMainHandItem();
            if (vehicle instanceof MortarEntity mortarEntity)
                renderMortarInfo(guiGraphics, posX, posY, mortarEntity, entity);
            else if (vehicle instanceof HeavyMachineGunEntity)
                renderHeavyMachineGunInfo(guiGraphics, posX + 10, posY - 10, entity);
            else if (itemStack.getItem() instanceof Flamethrower)
                renderFlamethrowerInfo(guiGraphics, posX + 10, posY - 20, itemStack);
            else if (itemStack.getItem() instanceof ModifiableWeapon modifiableWeapon && modifiableWeapon.getChargingParts(itemStack) != null)
                renderChargingPartsInfo(guiGraphics, width / 2, height / 2, itemStack);
        }
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        renderExo(event);
        renderWeaponInfo(event);
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
