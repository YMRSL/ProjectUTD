package com.ymrsl.utdassetmanager.common.blocktransform;

import com.ymrsl.utdassetmanager.UTDAssetManagerMod;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformMatcher.InteractionView;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.ActivationHand;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.CatalystSource;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformDeduplicator.Key;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformDeduplicator.Outcome;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRuntimeRules.CompiledRule;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRuntimeRules.CompiledSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = UTDAssetManagerMod.MOD_ID)
public final class BlockTransformEvents {
    private static final BlockTransformDeduplicator CLIENT_DEDUPLICATOR = new BlockTransformDeduplicator(512);
    private static final BlockTransformDeduplicator SERVER_DEDUPLICATOR = new BlockTransformDeduplicator(512);

    private BlockTransformEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos)) return;

        CompiledSet rules = BlockTransformRuntimeRules.current();
        if (!rules.usable() || rules.rules().isEmpty()) return;
        BlockTransformDeduplicator deduplicator = level.isClientSide ? CLIENT_DEDUPLICATOR : SERVER_DEDUPLICATOR;
        for (CompiledRule rule : rules.rules()) {
            Outcome previous = deduplicator.find(deduplicationKey(event, rule), level.getGameTime());
            if (previous != null) {
                cancel(event, cancellationResult(level, previous));
                return;
            }
        }
        BlockState targetState = level.getBlockState(pos);
        if (targetState.hasBlockEntity() || level.getBlockEntity(pos) != null) return;
        if (event.getEntity().isSpectator()) return;

        Candidate candidate = findCandidate(rules.rules(), event, targetState);
        if (candidate == null) return;
        Key deduplicationKey = deduplicationKey(event, candidate.rule());
        if (level.isClientSide) {
            deduplicator.remember(deduplicationKey, level.getGameTime(), Outcome.SUCCESS);
            cancel(event, InteractionResult.sidedSuccess(true));
            return;
        }

        boolean transformed = event.getEntity() instanceof ServerPlayer player
                && level instanceof ServerLevel serverLevel
                && execute(serverLevel, player, event, candidate, targetState);
        Outcome outcome = transformed ? Outcome.SUCCESS : Outcome.FAIL;
        deduplicator.remember(deduplicationKey, level.getGameTime(), outcome);
        cancel(event, cancellationResult(level, outcome));
    }

    private static Candidate findCandidate(
            List<CompiledRule> rules, PlayerInteractEvent.RightClickBlock event, BlockState targetState) {
        Player player = event.getEntity();
        ActivationHand hand = event.getHand() == InteractionHand.MAIN_HAND ? ActivationHand.MAIN : ActivationHand.OFF;
        String blockId = BuiltInRegistries.BLOCK.getKey(targetState.getBlock()).toString();
        boolean fakePlayer = player instanceof FakePlayer;
        for (CompiledRule rule : rules) {
            if (!rule.targetMatches(targetState)) continue;
            int available = availableCatalyst(rule, player, event.getHand());
            InteractionView interaction = new InteractionView(
                    blockId,
                    rule.targetStateValues(targetState),
                    hand,
                    player.isShiftKeyDown(),
                    fakePlayer,
                    player.getAbilities().instabuild);
            if (!BlockTransformMatcher.matches(rule.source(), interaction, available)) continue;
            BlockState resultState = rule.resultState(targetState);
            if (resultState.hasBlockEntity() || !resultState.canSurvive(event.getLevel(), event.getPos())) continue;
            return new Candidate(rule, resultState);
        }
        return null;
    }

    private static int availableCatalyst(CompiledRule rule, Player player, InteractionHand clickedHand) {
        if (player.getAbilities().instabuild && !rule.source().creative().requireInput()) return 0;
        if (rule.source().catalyst().source() == CatalystSource.CLICKED_HAND) {
            ItemStack stack = player.getItemInHand(clickedHand);
            return rule.itemMatches(stack, player.level().registryAccess()) ? stack.getCount() : 0;
        }
        int count = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!rule.itemMatches(stack, player.level().registryAccess())) continue;
            count = Math.min(Integer.MAX_VALUE, count + stack.getCount());
            if (count >= rule.source().catalyst().count()) break;
        }
        return count;
    }

    private static boolean execute(
            ServerLevel level,
            ServerPlayer player,
            PlayerInteractEvent.RightClickBlock event,
            Candidate candidate,
            BlockState expectedTarget) {
        BlockPos pos = event.getPos();
        CompiledRule rule = candidate.rule();
        Direction face = event.getFace() == null ? Direction.UP : event.getFace();
        if (!level.hasChunkAt(pos)
                || level.isOutsideBuildHeight(pos)
                || player.isSpectator()
                || !player.mayInteract(level, pos)
                || !player.mayUseItemAt(pos, face, player.getItemInHand(event.getHand()))
                || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                || level.captureBlockSnapshots
                || !level.capturedBlockSnapshots.isEmpty()) {
            return false;
        }
        BlockState current = level.getBlockState(pos);
        if (current != expectedTarget || !rule.targetMatches(current)
                || current.hasBlockEntity() || level.getBlockEntity(pos) != null) {
            return false;
        }
        if (!candidate.resultState().canSurvive(level, pos)) return false;
        if (!hasRequiredInput(rule, player, event.getHand())) return false;

        int snapshotStart = level.capturedBlockSnapshots.size();
        boolean placed;
        level.captureBlockSnapshots = true;
        try {
            placed = level.setBlock(pos, candidate.resultState(), Block.UPDATE_ALL);
        } finally {
            level.captureBlockSnapshots = false;
        }
        List<BlockSnapshot> attemptSnapshots = new ArrayList<>(
                level.capturedBlockSnapshots.subList(snapshotStart, level.capturedBlockSnapshots.size()));
        discardCapturedAfter(level, snapshotStart);
        if (!placed || attemptSnapshots.size() != 1) {
            restoreSnapshots(level, attemptSnapshots);
            if (level.getBlockState(pos) != expectedTarget) forceRollback(level, pos, expectedTarget);
            return false;
        }

        BlockSnapshot snapshot = attemptSnapshots.getFirst();
        if (EventHooks.onBlockPlace(player, snapshot, face)
                || level.getBlockState(pos) != candidate.resultState()
                || !consume(rule, player, event.getHand())) {
            restoreSnapshots(level, List.of(snapshot));
            if (level.getBlockState(pos) != expectedTarget) forceRollback(level, pos, expectedTarget);
            return false;
        }

        candidate.resultState().onPlace(level, pos, expectedTarget, false);
        level.markAndNotifyBlock(
                pos,
                level.getChunkAt(pos),
                expectedTarget,
                candidate.resultState(),
                Block.UPDATE_ALL,
                Block.UPDATE_LIMIT);
        player.getInventory().setChanged();
        return true;
    }

    private static boolean hasRequiredInput(CompiledRule rule, ServerPlayer player, InteractionHand hand) {
        if (player.getAbilities().instabuild && !rule.source().creative().requireInput()) return true;
        return availableCatalyst(rule, player, hand) >= rule.source().catalyst().count();
    }

    private static boolean consume(CompiledRule rule, ServerPlayer player, InteractionHand hand) {
        boolean creative = player.getAbilities().instabuild;
        boolean shouldConsume = rule.source().catalyst().consume()
                && (!creative || rule.source().creative().consume());
        if (!shouldConsume) return true;
        int required = rule.source().catalyst().count();
        if (rule.source().catalyst().source() == CatalystSource.CLICKED_HAND) {
            ItemStack stack = player.getItemInHand(hand);
            if (!rule.itemMatches(stack, player.level().registryAccess()) || stack.getCount() < required) return false;
            stack.shrink(required);
            return true;
        }

        int available = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (rule.itemMatches(stack, player.level().registryAccess())) available += stack.getCount();
            if (available >= required) break;
        }
        if (available < required) return false;
        int remaining = required;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!rule.itemMatches(stack, player.level().registryAccess())) continue;
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            remaining -= taken;
            if (remaining == 0) return true;
        }
        return false;
    }

    private static void discardCapturedAfter(ServerLevel level, int start) {
        while (level.capturedBlockSnapshots.size() > start) {
            level.capturedBlockSnapshots.remove(level.capturedBlockSnapshots.size() - 1);
        }
    }

    private static void restoreSnapshots(ServerLevel level, List<BlockSnapshot> snapshots) {
        List<BlockSnapshot> reversed = new ArrayList<>(snapshots);
        Collections.reverse(reversed);
        for (BlockSnapshot snapshot : reversed) {
            level.restoringBlockSnapshots = true;
            try {
                snapshot.restore(snapshot.getFlags() | Block.UPDATE_CLIENTS);
            } finally {
                level.restoringBlockSnapshots = false;
            }
        }
    }

    /** Last-resort rollback when a snapshot listener interfered with the normal restoration path. */
    private static void forceRollback(ServerLevel level, BlockPos pos, BlockState target) {
        BlockState replaced = level.getBlockState(pos);
        if (replaced == target) return;
        int start = level.capturedBlockSnapshots.size();
        boolean restored;
        level.captureBlockSnapshots = true;
        try {
            restored = level.setBlock(pos, target, Block.UPDATE_ALL);
        } finally {
            level.captureBlockSnapshots = false;
            discardCapturedAfter(level, start);
        }
        if (restored && level.getBlockState(pos) == target) {
            target.onPlace(level, pos, replaced, false);
            level.markAndNotifyBlock(
                    pos, level.getChunkAt(pos), replaced, target, Block.UPDATE_ALL, Block.UPDATE_LIMIT);
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCancellationResult(result);
        event.setCanceled(true);
    }

    private static InteractionResult cancellationResult(Level level, Outcome outcome) {
        return outcome == Outcome.SUCCESS
                ? InteractionResult.sidedSuccess(level.isClientSide)
                : InteractionResult.FAIL;
    }

    private static Key deduplicationKey(PlayerInteractEvent.RightClickBlock event, CompiledRule rule) {
        return new Key(
                event.getEntity().getUUID().toString(),
                event.getLevel().dimension().location().toString(),
                event.getPos().asLong(),
                rule.source().id());
    }

    private record Candidate(CompiledRule rule, BlockState resultState) {
    }
}
