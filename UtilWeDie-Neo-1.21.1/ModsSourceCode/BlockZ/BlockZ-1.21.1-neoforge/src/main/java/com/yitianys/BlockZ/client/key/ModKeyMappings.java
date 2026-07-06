package com.yitianys.BlockZ.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.network.DayzToggleRequestC2S;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * KEEP 键位：DayZ 界面开关 + 占格物品旋转。
 *
 * <p>SPLIT：已删除 DROP 键位 FOCUS（瞄准缩放）/PRONE（卧倒）/LEAN_LEFT/LEAN_RIGHT（探头），
 * 以及它们关联的 InputEvent.Key 处理、ClientTick 体力/卧倒同步、ViewportEvent FOV/相机 roll。
 * ROTATE_ITEM 的实际处理在 DayZInventoryScreen#keyPressed 内（界面打开时）。
 */
@EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT)
public class ModKeyMappings {
    public static KeyMapping OPEN_DAYZ;
    public static KeyMapping ROTATE_ITEM;

    public static void register(RegisterKeyMappingsEvent event) {
        OPEN_DAYZ = new KeyMapping("key.blockz.open_dayz", InputConstants.KEY_I, "key.categories.inventory");
        ROTATE_ITEM = new KeyMapping("key.blockz.rotate_item", InputConstants.KEY_SPACE, "key.categories.inventory");
        event.register(OPEN_DAYZ);
        event.register(ROTATE_ITEM);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (OPEN_DAYZ != null && OPEN_DAYZ.consumeClick()) {
            if (!ClientSettings.dayzToggleAllowed) {
                mc.player.sendSystemMessage(Component.translatable("msg.blockz.dayz_toggle_denied"));
                return;
            }
            BlockZ.LOGGER.info("Toggling DayZ UI. Current state: {}. Sending: {}",
                    ClientSettings.dayzEnabled, !ClientSettings.dayzEnabled);
            PacketDistributor.sendToServer(new DayzToggleRequestC2S(!ClientSettings.dayzEnabled));
        }
    }
}
