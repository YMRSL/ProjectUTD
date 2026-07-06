package net.mcreator.doomsdaydecoration.block;

import net.mcreator.doomsdaydecoration.init.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockItem for the decoration blocks that adds two placement behaviours for the
 * big multi-cell blocks (vehicles, hesco, wc…):
 *
 * <ul>
 *   <li><b>Lift</b>: blocks whose shape dips below their own cell (hummers,
 *   ambulances…) are placed a cell higher so they sit on the clicked surface
 *   instead of sinking into the ground. MC models can't be shifted up (max Y=32),
 *   so the placement is offset instead — only for the blocks that need it
 *   ({@link DecoShapeStore#lift}), the rest are unaffected.</li>
 *   <li><b>Obstruction</b>: placement is refused (item kept, nothing placed) if any
 *   footprint cell is already occupied by a real block — TaCZ-style.</li>
 * </ul>
 * Plain blocks (no lift, no fillers) behave exactly like a vanilla BlockItem.
 */
public class DecoBlockItem extends BlockItem {
    public DecoBlockItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        int lift = DecoShapeStore.lift(getBlock());
        return super.place(lift <= 0 ? context : new LiftedContext(context, lift));
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : null;
        int[][] fillers = DecoShapeStore.fillers(getBlock(), facing);
        if (fillers != null) {
            Level level = context.getLevel();
            BlockPos base = context.getClickedPos();
            Block fillerBlock = ModRegistry.COLLISION_FILLER.get();
            for (int[] c : fillers) {
                BlockState occ = level.getBlockState(base.offset(c[0], 0, c[1]));
                if (!occ.isAir() && !occ.canBeReplaced() && !occ.is(fillerBlock)) {
                    return null;   // a footprint cell is blocked — refuse placement
                }
            }
        }
        return state;
    }

    /** A placement context shifted up by {@code lift} cells (for below-cell shapes). */
    private static final class LiftedContext extends BlockPlaceContext {
        private final int lift;

        private LiftedContext(BlockPlaceContext base, int lift) {
            super(base);   // BlockPlaceContext(UseOnContext) — protected, reachable from a subclass
            this.lift = lift;
        }

        @Override
        public BlockPos getClickedPos() {
            return super.getClickedPos().above(lift);
        }
    }
}
