package net.mcreator.doomsdaydecoration.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible collision-only filler placed by 2-D multi-cell decoration blocks
 * (vehicles, hesco, wc…) into every footprint cell, so the whole block is gap-free
 * solid with clean per-cell physics — you stand on a real block in each cell
 * (proper onGround / jump), it tiles cell-aligned (no catch/suck), and it needs no
 * entity mixin (nothing else, e.g. TaCZ / ParCool crawl, is affected).
 *
 * <p>Collision is a cell-aligned full box grounded at the block floor up to the
 * cell's height ({@code top}, in 1/16 blocks 1..32). The shape is <b>cached</b> per
 * blockstate (no dynamic BlockEntity shape) — that cache is what keeps standing /
 * walking smooth; an uncached dynamic shape jitters (repulsion / suction). Not
 * rendered, not selectable, no drops — managed by {@link DecoFillerManager}.
 */
public class CollisionFillerBlock extends Block {
    public static final IntegerProperty TOP = IntegerProperty.create("top", 1, 32);
    /** True when the block dips a cell below its own cell (wheels/chassis) — collide down to -16. */
    public static final BooleanProperty SUNKEN = BooleanProperty.create("sunken");

    private static final VoxelShape[] SHAPES = new VoxelShape[33];        // floor 0
    private static final VoxelShape[] SHAPES_SUNKEN = new VoxelShape[33]; // floor -16
    static {
        for (int t = 1; t <= 32; t++) {
            SHAPES[t] = Block.box(0.0, 0.0, 0.0, 16.0, t, 16.0);
            SHAPES_SUNKEN[t] = Block.box(0.0, -16.0, 0.0, 16.0, t, 16.0);
        }
    }

    public CollisionFillerBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(TOP, 16).setValue(SUNKEN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(CollisionFillerBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, SUNKEN);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return (state.getValue(SUNKEN) ? SHAPES_SUNKEN : SHAPES)[state.getValue(TOP)];
    }

    /** Empty outline: invisible, must not be selectable / targetable. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
