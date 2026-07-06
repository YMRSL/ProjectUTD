package com.yitianys.BlockZ.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.capability.PlayerBackpack;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.BlockZAttachments;
import com.yitianys.BlockZ.network.DayzTogglePermissionS2C;
import com.yitianys.BlockZ.network.DayzToggleStateS2C;
import com.yitianys.BlockZ.network.SyncBackpackS2C;
import com.yitianys.BlockZ.network.SyncConfigS2C;
import com.yitianys.BlockZ.network.SyncGridRulesS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
        modid = BlockZ.MODID,
        bus = EventBusSubscriber.Bus.GAME
)
public class ModEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CuriosIntegration.importToCapability(player);
            syncPlayerState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) {
            BlockZConfigs.clearSyncedValues();
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CuriosIntegration.importToCapability(player);
            syncPlayerState(player);
        }
    }

    private static void syncPlayerState(ServerPlayer player) {
        PlayerBackpack backpack = player.getData(BlockZAttachments.PLAYER_BACKPACK);
        for (int i = 0; i < 4; i++) {
            ItemStack stack = backpack.getInventory().getStackInSlot(i);
            PacketDistributor.sendToPlayer(player, new SyncBackpackS2C(i, stack));
        }
        PacketDistributor.sendToPlayer(player, new DayzToggleStateS2C(backpack.isDayzEnabled()));

        boolean allowed = BlockZConfigs.getAllowPlayerToggleDayz() || player.hasPermissions(2);
        PacketDistributor.sendToPlayer(player, new DayzTogglePermissionS2C(allowed));
        syncServerConfigs(player);
    }

    public static void syncServerConfigs(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SyncGridRulesS2C.createServerSnapshot());
        PacketDistributor.sendToPlayer(player, SyncConfigS2C.createServerSnapshot());
    }

    public static void broadcastServerConfigs(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncServerConfigs(player);
        }
    }
}
