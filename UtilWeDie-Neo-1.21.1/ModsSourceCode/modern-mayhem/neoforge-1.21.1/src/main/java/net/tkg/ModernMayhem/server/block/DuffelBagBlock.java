package net.tkg.ModernMayhem.server.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.tkg.ModernMayhem.server.block.entity.DuffelBagBlockEntity;

public class DuffelBagBlock
extends Block
implements EntityBlock,
SimpleWaterloggedBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public DuffelBagBlock(BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)Direction.NORTH)).setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(false)));
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch ((Direction)state.getValue((Property)FACING)) {
            case Direction.WEST, Direction.EAST -> DuffelBagBlock.box((double)4.5, (double)0.0, (double)0.0, (double)11.5, (double)6.0, (double)16.0);
            case Direction.SOUTH, Direction.NORTH -> DuffelBagBlock.box((double)0.0, (double)0.0, (double)4.5, (double)16.0, (double)6.0, (double)11.5);
            default -> DuffelBagBlock.box((double)0.0, (double)0.0, (double)4.5, (double)16.0, (double)6.0, (double)11.5);
        };
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DuffelBagBlockEntity(pos, state);
    }

    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            DuffelBagBlockEntity entity;
            ItemStack duffelBag;
            BlockEntity blockEntity;
            if (!level.isClientSide && (blockEntity = level.getBlockEntity(pos)) instanceof DuffelBagBlockEntity && !(duffelBag = (entity = (DuffelBagBlockEntity)blockEntity).getDuffelBag()).isEmpty()) {
                if (!player.addItem(duffelBag.copy())) {
                    player.drop(duffelBag.copy(), false);
                }
                ((ServerLevel)level).sendParticles((ParticleOptions)ParticleTypes.SWEEP_ATTACK, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.0f);
                level.removeBlock(pos, false);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.sidedSuccess((boolean)level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy((BlockGetter)level, below, Direction.UP);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)context.getHorizontalDirection().getOpposite());
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return (BlockState)state.setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(fluid.getType() == Fluids.WATER));
    }

    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (((Boolean)state.getValue((Property)WATERLOGGED)).booleanValue()) {
            level.scheduleTick(pos, (Fluid)Fluids.WATER, Fluids.WATER.getTickDelay((LevelReader)level));
        }
        if (direction == Direction.DOWN && !state.canSurvive((LevelReader)level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!state.canSurvive((LevelReader)level, pos)) {
            level.destroyBlock(pos, true);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DuffelBagBlockEntity entity;
        ItemStack duffelBag;
        BlockEntity blockEntity;
        if (!level.isClientSide && (blockEntity = level.getBlockEntity(pos)) instanceof DuffelBagBlockEntity && !(duffelBag = (entity = (DuffelBagBlockEntity)blockEntity).getDuffelBag()).isEmpty()) {
            ItemEntity itemEntity = new ItemEntity(level, (double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5, duffelBag.copy());
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity((Entity)itemEntity);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    public FluidState getFluidState(BlockState state) {
        return (Boolean)state.getValue((Property)WATERLOGGED) != false ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING, WATERLOGGED});
    }
}

