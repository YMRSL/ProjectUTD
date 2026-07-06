package com.scarasol.sona.mixin;

import com.scarasol.sona.client.gui.ItemMarkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 复刻上游 1.20.1 的 AbstractContainerScreenMixin：在容器/背包界面每个槽位渲染物品装饰后，
 * 叠加腐烂/锈蚀新鲜度三色图标。1.21.1 下 {@code renderSlot(GuiGraphics, Slot)} 签名稳定
 * （与包内 SearchCarefully 的 SlotRenderMixin、BlockZ 的 MixinAbstractContainerScreen 一致）。
 *
 * <p><b>z 层级关键点</b>：1.21.1 的 {@code renderSlot} 把整段物品渲染包在
 * {@code pushPose()/translate(0,0,100)/popPose()} 内，物品模型经 {@code GuiGraphics.renderItem}
 * 再 {@code translate(...,150)} 落在绝对 z≈250、物品装饰文字在 z≈300。本注入点 {@code TAIL}
 * 已在 {@code popPose()} 之后（pose 回到 z=0），若直接 blit 会被 z250 的物品精灵遮住——这正是
 * “快捷栏显示、背包不显示”的原因。故此处显式把 pose 抬到 z=300 再绘制，确保压在物品之上。
 * 快捷栏 {@code Gui.renderSlot} 无此 z 抬升，{@link GuiMixin} 保持原样即可。
 */
@OnlyIn(Dist.CLIENT)
@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V",
            at = @At("TAIL"))
    private void sona$renderMark(GuiGraphics guiGraphics, Slot slot, CallbackInfo ci) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 300.0F);
        ItemMarkHandler.renderMark(guiGraphics, slot);
        guiGraphics.pose().popPose();
    }
}
