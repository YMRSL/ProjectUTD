package net.mcreator.survivalinstinct.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StopSingBlock
extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public StopSingBlock() {
        super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(3.0f, 10.0f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)FACING, (Comparable)Direction.NORTH));
    }

    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch ((Direction)state.getValue((Property)FACING)) {
            default -> Shapes.or((VoxelShape)StopSingBlock.box((double)3.0, (double)0.0, (double)3.0, (double)13.0, (double)2.0, (double)13.0), (VoxelShape[])new VoxelShape[]{StopSingBlock.box((double)7.3, (double)1.3, (double)7.3, (double)8.7, (double)18.7, (double)8.7), StopSingBlock.box((double)1.0, (double)18.7, (double)7.5, (double)15.0, (double)31.7, (double)8.5)});
            case Direction.NORTH -> Shapes.or((VoxelShape)StopSingBlock.box((double)3.0, (double)0.0, (double)3.0, (double)13.0, (double)2.0, (double)13.0), (VoxelShape[])new VoxelShape[]{StopSingBlock.box((double)7.3, (double)1.3, (double)7.3, (double)8.7, (double)18.7, (double)8.7), StopSingBlock.box((double)1.0, (double)18.7, (double)7.5, (double)15.0, (double)31.7, (double)8.5)});
            case Direction.EAST -> Shapes.or((VoxelShape)StopSingBlock.box((double)3.0, (double)0.0, (double)3.0, (double)13.0, (double)2.0, (double)13.0), (VoxelShape[])new VoxelShape[]{StopSingBlock.box((double)7.3, (double)1.3, (double)7.3, (double)8.7, (double)18.7, (double)8.7), StopSingBlock.box((double)7.5, (double)18.7, (double)1.0, (double)8.5, (double)31.7, (double)15.0)});
            case Direction.WEST -> Shapes.or((VoxelShape)StopSingBlock.box((double)3.0, (double)0.0, (double)3.0, (double)13.0, (double)2.0, (double)13.0), (VoxelShape[])new VoxelShape[]{StopSingBlock.box((double)7.3, (double)1.3, (double)7.3, (double)8.7, (double)18.7, (double)8.7), StopSingBlock.box((double)7.5, (double)18.7, (double)1.0, (double)8.5, (double)31.7, (double)15.0)});
        };
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)context.getHorizontalDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rot.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation((Direction)state.getValue((Property)FACING)));
    }
}

