package com.scarasol.sona.mixin;

import com.scarasol.sona.client.gui.ItemMarkHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 复刻上游 1.20.1 的 GuiMixin：在热区(hotbar)每个槽位渲染物品装饰后，叠加腐烂/锈蚀新鲜度三色图标。
 * 1.21.1 下 {@code Gui.renderSlot} 的 {@code float partialTick} 形参已变为 {@link DeltaTracker}，
 * 描述符随之改变（与包内 SearchCarefully 的 GuiHotbarRenderMixin 一致）。
 */
@OnlyIn(Dist.CLIENT)
@Mixin(Gui.class)
public class GuiMixin {

    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
            at = @At("TAIL"))
    private void sona$renderMark(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack itemStack, int slotIndex, CallbackInfo ci) {
        ItemMarkHandler.renderMark(guiGraphics, itemStack, x, y);
    }
}
