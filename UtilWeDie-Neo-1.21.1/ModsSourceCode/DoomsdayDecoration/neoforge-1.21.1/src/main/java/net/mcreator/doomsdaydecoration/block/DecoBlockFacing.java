package net.mcreator.doomsdaydecoration.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decoration block with a horizontal FACING property (no waterlogging).
 *
 * The FACING property is added unconditionally in {@link #createBlockStateDefinition},
 * so it is registered correctly even though that method runs during {@code super(props)}
 * before subclass fields would be initialized. See {@link DecoBlockPlain} for the full
 * rationale behind splitting the original data-driven {@code DecoBlock}.
 */
public class DecoBlockFacing extends Block implements DecoShaped {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public DecoBlockFacing(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(DecoBlockFacing::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = DecoShapeStore.shape(this, state.getValue(FACING));
        return s != null ? s : super.getShape(state, level, pos, ctx);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = DecoShapeStore.collisionShape(this, state.getValue(FACING));
        return s != null ? s : super.getCollisionShape(state, level, pos, ctx);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return DecoShapeStore.has(this) ? Shapes.empty() : super.getVisualShape(state, level, pos, ctx);
    }

    @Override
    public boolean ddOversized() {
        return DecoShapeStore.oversized(this);
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (DecoShapeStore.oversized(this)) DecoFillerManager.onPlaced(level, pos, this, state.getValue(FACING));
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                            BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && DecoShapeStore.oversized(this)) {
            DecoFillerManager.onRemoved(level, pos, this, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
