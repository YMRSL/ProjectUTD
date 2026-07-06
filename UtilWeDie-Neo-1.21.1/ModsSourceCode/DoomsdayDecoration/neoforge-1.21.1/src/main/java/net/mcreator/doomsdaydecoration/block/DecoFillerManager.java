package net.mcreator.doomsdaydecoration.block;

import net.mcreator.doomsdaydecoration.init.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places / removes the invisible {@link CollisionFillerBlock}s that make an
 * oversized decoration block's whole footprint gap-free solid.
 *
 * <p>Multi-piece vehicles share footprint cells, so a filler is only removed when
 * no <em>other</em> oversized owner still covers that cell (and its height is
 * recomputed from whoever remains). Placement into a non-air, non-filler block is
 * skipped — that is the "can't place if obstructed" behaviour for large blocks.
 */
public final class DecoFillerManager {
    private DecoFillerManager() {}

    /** Owner block placed: fill its footprint cells with collision fillers. Server only. */
    public static void onPlaced(Level level, BlockPos ownerPos, Block owner, Direction facing) {
        if (level.isClientSide) return;
        int[][] cells = DecoShapeStore.fillers(owner, facing);
        if (cells == null) return;
        boolean sunken = DecoShapeStore.sunken(owner);
        Block filler = ModRegistry.COLLISION_FILLER.get();
        for (int[] c : cells) {
            BlockPos p = ownerPos.offset(c[0], 0, c[1]);
            int top = c[2];
            BlockState cur = level.getBlockState(p);
            if (cur.isAir() || cur.canBeReplaced()) {
                level.setBlock(p, filler.defaultBlockState()
                        .setValue(CollisionFillerBlock.TOP, top)
                        .setValue(CollisionFillerBlock.SUNKEN, sunken), Block.UPDATE_CLIENTS);
            } else if (cur.is(filler)) {
                int existing = cur.getValue(CollisionFillerBlock.TOP);
                boolean s = sunken || cur.getValue(CollisionFillerBlock.SUNKEN);
                if (top > existing || s != cur.getValue(CollisionFillerBlock.SUNKEN)) {
                    level.setBlock(p, cur.setValue(CollisionFillerBlock.TOP, Math.max(top, existing))
                            .setValue(CollisionFillerBlock.SUNKEN, s), Block.UPDATE_CLIENTS);
                }
            }
            // otherwise: a real block already occupies the cell — leave it (obstruction).
        }
    }

    /** Owner block removed: clear its fillers, keeping cells still covered by another owner. Server only. */
    public static void onRemoved(Level level, BlockPos ownerPos, Block owner, Direction facing) {
        if (level.isClientSide) return;
        int[][] cells = DecoShapeStore.fillers(owner, facing);
        if (cells == null) return;
        Block filler = ModRegistry.COLLISION_FILLER.get();
        for (int[] c : cells) {
            BlockPos p = ownerPos.offset(c[0], 0, c[1]);
            if (!level.getBlockState(p).is(filler)) continue;
            int top = maxTopFromOtherOwners(level, p, ownerPos);
            if (top > 0) {
                boolean s = anySunkenOwner(level, p, ownerPos);
                level.setBlock(p, filler.defaultBlockState()
                        .setValue(CollisionFillerBlock.TOP, top)
                        .setValue(CollisionFillerBlock.SUNKEN, s), Block.UPDATE_CLIENTS);
            } else {
                level.removeBlock(p, false);
            }
        }
    }

    /**
     * Scan oversized owners near cell {@code p} (footprints reach ±1, so a covering
     * owner is within ±2) other than the one at {@code excludePos}; return the max
     * filler height they contribute to {@code p}, or 0 if none.
     */
    private static int maxTopFromOtherOwners(Level level, BlockPos p, BlockPos excludePos) {
        int best = 0;
        BlockPos.MutableBlockPos op = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                op.set(p.getX() + dx, p.getY(), p.getZ() + dz);
                if (op.getX() == excludePos.getX() && op.getY() == excludePos.getY() && op.getZ() == excludePos.getZ()) {
                    continue;
                }
                BlockState os = level.getBlockState(op);
                Block ob = os.getBlock();
                if (!(ob instanceof DecoShaped) || !DecoShapeStore.oversized(ob)) continue;
                Direction f = os.hasProperty(HorizontalDirectionalBlock.FACING)
                        ? os.getValue(HorizontalDirectionalBlock.FACING) : null;
                int[][] cells = DecoShapeStore.fillers(ob, f);
                if (cells == null) continue;
                for (int[] c : cells) {
                    if (op.getX() + c[0] == p.getX() && op.getZ() + c[1] == p.getZ()) {
                        best = Math.max(best, c[2]);
                    }
                }
            }
        }
        return best;
    }

    /** True if any oversized owner (≠ excludePos) covering cell {@code p} is sunken. */
    private static boolean anySunkenOwner(Level level, BlockPos p, BlockPos excludePos) {
        BlockPos.MutableBlockPos op = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                op.set(p.getX() + dx, p.getY(), p.getZ() + dz);
                if (op.getX() == excludePos.getX() && op.getY() == excludePos.getY() && op.getZ() == excludePos.getZ()) {
                    continue;
                }
                BlockState os = level.getBlockState(op);
                Block ob = os.getBlock();
                if (!(ob instanceof DecoShaped) || !DecoShapeStore.oversized(ob) || !DecoShapeStore.sunken(ob)) continue;
                Direction f = os.hasProperty(HorizontalDirectionalBlock.FACING)
                        ? os.getValue(HorizontalDirectionalBlock.FACING) : null;
                int[][] cells = DecoShapeStore.fillers(ob, f);
                if (cells == null) continue;
                for (int[] c : cells) {
                    if (op.getX() + c[0] == p.getX() && op.getZ() + c[1] == p.getZ()) return true;
                }
            }
        }
        return false;
    }
}
