package org.yanbwe.searchcarefully.manager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.yanbwe.searchcarefully.network.StartLoopSoundPacket;
import org.yanbwe.searchcarefully.util.ItemStackHelper;
import org.yanbwe.searchcarefully.util.SearchConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SearchSoundSessionManager {

    private static final Map<UUID, Boolean> playerSoundActive = new HashMap<>();

    public static boolean hasAnySearchableItems(Player player) {
        if (player == null) return false;

        // 检查容器菜单槽位
        if (player.containerMenu != null) {
            for (Slot slot : player.containerMenu.slots) {
                if (slot.hasItem()) {
                    ItemStack stack = slot.getItem();
                    if (ItemStackHelper.hasRemainingSearchTime(stack) &&
                        ItemStackHelper.getRemainingSearchTime(stack) > 0) {
                        return true;
                    }
                }
            }
        }

        // 检查热键栏（如果启用了热键栏搜索）
        if (SearchConstants.isHotbarSearchEnabled()) {
            var inventory = player.getInventory();
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inventory.getItem(i);
                if (ItemStackHelper.hasRemainingSearchTime(stack) &&
                    ItemStackHelper.getRemainingSearchTime(stack) > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void updateSoundSession(Player player) {
        if (player == null || player.level().isClientSide()) return;

        UUID playerId = player.getUUID();
        boolean hasSearchable = hasAnySearchableItems(player);
        boolean soundActive = playerSoundActive.getOrDefault(playerId, false);

        if (hasSearchable && !soundActive) {
            startLoopSound(player);
            playerSoundActive.put(playerId, true);
        } else if (!hasSearchable && soundActive) {
            stopLoopSound(player);
            playerSoundActive.put(playerId, false);
        }
    }

    public static void forceStopSound(Player player) {
        if (player == null || player.level().isClientSide()) return;

        UUID playerId = player.getUUID();
        Boolean wasActive = playerSoundActive.get(playerId);
        if (wasActive != null && wasActive) {
            stopLoopSound(player);
            playerSoundActive.put(playerId, false);
        }
    }

    private static void startLoopSound(Player player) {
        PacketDistributor.sendToPlayer(
            (net.minecraft.server.level.ServerPlayer) player,
            new StartLoopSoundPacket()
        );
    }

    private static void stopLoopSound(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(
                new net.minecraft.network.protocol.game.ClientboundStopSoundPacket(
                    org.yanbwe.searchcarefully.sounds.SearchCompletionSound.SEARCH_PROGRESS_SOUND_ID,
                    null
                )
            );
        }
    }

    public static void removePlayer(UUID playerId) {
        playerSoundActive.remove(playerId);
    }

    public static void resetAll() {
        playerSoundActive.clear();
    }
}
