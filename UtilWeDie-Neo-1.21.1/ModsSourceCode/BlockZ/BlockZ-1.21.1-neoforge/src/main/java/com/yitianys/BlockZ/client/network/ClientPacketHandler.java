package com.yitianys.BlockZ.client.network;

import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.BlockZAttachments;
import com.yitianys.BlockZ.network.DayzTogglePermissionS2C;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 客户端侧 S2C 包处理。仅在客户端类加载，所有对 ClientSettings / 客户端玩家的写入集中于此。
 *
 * <p>从 Forge {@code NetworkEvent.Context} + {@code DistExecutor} 迁移而来：
 * 现在由各 S2C 包的 handler 在 {@code FMLEnvironment.dist == Dist.CLIENT} 守卫下直接调用。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void handleSyncBackpack(SyncBackpackS2C msg) {
        if (Minecraft.getInstance().player != null) {
            PlayerBackpack cap = Minecraft.getInstance().player.getData(BlockZAttachments.PLAYER_BACKPACK);
            cap.getInventory().setStackInSlot(msg.slotId(), msg.stack());
        }
    }

    public static void handleDayzToggleState(DayzToggleStateS2C msg) {
        ClientSettings.dayzEnabled = msg.isEnabled();
        ClientSettings.dayzHudEnabled = msg.isEnabled();
        if (Minecraft.getInstance().player != null) {
            if (BlockZConfigs.getShowDayzToggleChatHint()) {
                Minecraft.getInstance().player.sendSystemMessage(Component.translatable(
                        msg.isEnabled() ? "msg.blockz.dayz_enabled" : "msg.blockz.dayz_disabled"));
            }
        }
    }

    public static void handleDayzTogglePermission(DayzTogglePermissionS2C msg) {
        ClientSettings.dayzToggleAllowed = msg.allowed();
        if (!msg.allowed()) {
            ClientSettings.dayzEnabled = true;
        }
    }
}
