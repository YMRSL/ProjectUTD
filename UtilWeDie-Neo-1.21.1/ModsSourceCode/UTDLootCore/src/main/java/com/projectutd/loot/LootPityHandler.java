package com.projectutd.loot;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Persistent replacement for the former KubeJS chest-open pity listener. */
public final class LootPityHandler {
    private static final String DOOMSDAY_MENU_CLASS =
        "net.mcreator.doomsdaydecoration.functionality.DoomsdayContainerMenu";
    private static final String CONTAINER_PREFIX = "doomsday_decoration:chests/";
    private static final String LAST_ROLL_KEY = "utd_loot_core:last_processed_roll";
    private static final int CONTAINER_SLOTS = 27;

    private final LootCatalog catalog;
    private Field blockEntityField;
    private boolean reflectionFailureLogged;

    public LootPityHandler(LootCatalog catalog) {
        this.catalog = catalog;
    }

    @SubscribeEvent
    public void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        if (!DOOMSDAY_MENU_CLASS.equals(menu.getClass().getName()) || menu.slots.size() < CONTAINER_SLOTS) {
            return;
        }
        BlockEntity blockEntity = resolveBlockEntity(menu);
        if (blockEntity == null) {
            return;
        }

        CompoundTag updateTag = blockEntity.getUpdateTag(player.serverLevel().registryAccess());
        String lootTableId = resolveLootTableId(blockEntity, updateTag);
        if (!lootTableId.startsWith(CONTAINER_PREFIX)) {
            return;
        }
        LootCatalog.ContainerConfig config = catalog.container(lootTableId);
        if (config == null) {
            UtdLootCore.LOGGER.warn("No UTD container configuration for {}", lootTableId);
            return;
        }

        long lastLootDay = updateTag.contains("LastLootDay") ? updateTag.getLong("LastLootDay") : -1L;
        String rollId = lootTableId + "|" + lastLootDay;
        CompoundTag persistent = blockEntity.getPersistentData();
        if (rollId.equals(persistent.getString(LAST_ROLL_KEY))) {
            return;
        }

        processPity(player, menu, config);
        persistent.putString(LAST_ROLL_KEY, rollId);
        blockEntity.setChanged();
        menu.broadcastChanges();
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()
            && event.getOriginal() instanceof ServerPlayer original
            && event.getEntity() instanceof ServerPlayer replacement) {
            PityState.copy(original, replacement);
        }
    }

    private void processPity(
        ServerPlayer player,
        AbstractContainerMenu menu,
        LootCatalog.ContainerConfig config
    ) {
        String channel = config.pityChannel();
        String tier4Key = PityState.tier4Key(channel);
        String tier5Key = PityState.tier5Key(channel);
        if (!config.countPity() || tier4Key == null) {
            return;
        }

        int tier4Miss = PityState.get(player, tier4Key);
        int tier5Miss = PityState.get(player, tier5Key);
        RandomSource random = player.serverLevel().getRandom();
        boolean tier5Applied = false;
        boolean tier4Applied = false;

        if (tier5Key != null) {
            if (PityPolicy.tier5HardGuarantee(tier5Miss) || roll(random, PityPolicy.tier5SoftChance(tier5Miss))) {
                tier5Applied = tryInject(menu, player, config.template(), channel, 5, random);
            }
        }
        if (!tier5Applied && (PityPolicy.tier4HardGuarantee(tier4Miss) || roll(random, PityPolicy.tier4SoftChance(tier4Miss)))) {
            tier4Applied = tryInject(menu, player, config.template(), channel, 4, random);
        }

        InventoryStats stats = scan(menu);
        PityState.set(player, tier4Key, stats.hasTier4() ? 0 : tier4Miss + 1);
        if (tier5Key != null) {
            PityState.set(player, tier5Key, stats.hasTier5() ? 0 : tier5Miss + 1);
        }
        if (tier4Applied || tier5Applied) {
            UtdLootCore.LOGGER.info(
                "Applied {} pity loot to {} in {} ({})",
                tier5Applied ? "tier-5" : "tier-4",
                player.getGameProfile().getName(),
                config.lootTableId(),
                channel
            );
        }
    }

    private boolean tryInject(
        AbstractContainerMenu menu,
        ServerPlayer player,
        String template,
        String channel,
        int level,
        RandomSource random
    ) {
        LootCatalog.LootEntry candidate = catalog.pickDirectedCandidate(level, template, channel, random);
        ItemStack stack = catalog.createStack(candidate);
        if (stack.isEmpty()) {
            return false;
        }
        int slotIndex = findReplacementSlot(menu, level >= 5 ? 3 : 2, random);
        if (slotIndex < 0) {
            return false;
        }
        Slot slot = menu.slots.get(slotIndex);
        slot.set(stack);
        slot.setChanged();
        return true;
    }

    private int findReplacementSlot(AbstractContainerMenu menu, int maxReplaceLevel, RandomSource random) {
        int emptySlot = -1;
        int bestLevel = Integer.MAX_VALUE;
        List<Integer> bestSlots = new ArrayList<>();
        for (int index = 0; index < CONTAINER_SLOTS; index++) {
            ItemStack stack = menu.slots.get(index).getItem();
            if (stack.isEmpty()) {
                if (emptySlot < 0) {
                    emptySlot = index;
                }
                continue;
            }
            int level = catalog.levelOf(stack);
            if (level < 0 || level >= 4 || level > maxReplaceLevel) {
                continue;
            }
            if (level < bestLevel) {
                bestLevel = level;
                bestSlots.clear();
                bestSlots.add(index);
            } else if (level == bestLevel) {
                bestSlots.add(index);
            }
        }
        return bestSlots.isEmpty() ? emptySlot : bestSlots.get(random.nextInt(bestSlots.size()));
    }

    private InventoryStats scan(AbstractContainerMenu menu) {
        boolean hasTier4 = false;
        boolean hasTier5 = false;
        for (int index = 0; index < CONTAINER_SLOTS; index++) {
            int level = catalog.levelOf(menu.slots.get(index).getItem());
            hasTier4 |= level >= 4;
            hasTier5 |= level >= 5;
        }
        return new InventoryStats(hasTier4, hasTier5);
    }

    private BlockEntity resolveBlockEntity(AbstractContainerMenu menu) {
        try {
            if (blockEntityField == null) {
                blockEntityField = menu.getClass().getDeclaredField("blockEntity");
                blockEntityField.setAccessible(true);
            }
            Object value = blockEntityField.get(menu);
            return value instanceof BlockEntity blockEntity ? blockEntity : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            if (!reflectionFailureLogged) {
                reflectionFailureLogged = true;
                UtdLootCore.LOGGER.error("Cannot access the Doomsday container block entity; pity is disabled", exception);
            }
            return null;
        }
    }

    private static String resolveLootTableId(BlockEntity blockEntity, CompoundTag updateTag) {
        String stored = updateTag.getString("LootTable");
        if (!stored.isBlank()) {
            return stored;
        }
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "chests/" + blockId.getPath()).toString();
    }

    private static boolean roll(RandomSource random, double chance) {
        return chance > 0.0D && random.nextDouble() < chance;
    }

    private record InventoryStats(boolean hasTier4, boolean hasTier5) {}
}
