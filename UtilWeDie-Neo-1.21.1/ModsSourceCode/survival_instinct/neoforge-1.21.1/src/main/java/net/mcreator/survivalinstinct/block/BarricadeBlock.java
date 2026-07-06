package net.mcreator.survivalinstinct.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
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

public class BarricadeBlock
extends Block {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public BarricadeBlock() {
        super(BlockBehaviour.Properties.of().ignitedByLava().instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).strength(3.0f, 10.0f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
            default -> Shapes.or((VoxelShape)BarricadeBlock.box((double)0.0, (double)6.0, (double)0.0, (double)16.0, (double)10.0, (double)1.0), (VoxelShape)BarricadeBlock.box((double)0.0, (double)6.0, (double)1.0, (double)16.0, (double)10.0, (double)2.0));
            case Direction.NORTH -> Shapes.or((VoxelShape)BarricadeBlock.box((double)0.0, (double)6.0, (double)15.0, (double)16.0, (double)10.0, (double)16.0), (VoxelShape)BarricadeBlock.box((double)0.0, (double)6.0, (double)14.0, (double)16.0, (double)10.0, (double)15.0));
            case Direction.EAST -> Shapes.or((VoxelShape)BarricadeBlock.box((double)0.0, (double)6.0, (double)0.0, (double)1.0, (double)10.0, (double)16.0), (VoxelShape)BarricadeBlock.box((double)1.0, (double)6.0, (double)0.0, (double)2.0, (double)10.0, (double)16.0));
            case Direction.WEST -> Shapes.or((VoxelShape)BarricadeBlock.box((double)15.0, (double)6.0, (double)0.0, (double)16.0, (double)10.0, (double)16.0), (VoxelShape)BarricadeBlock.box((double)14.0, (double)6.0, (double)0.0, (double)15.0, (double)10.0, (double)16.0));
            case Direction.UP -> Shapes.or((VoxelShape)BarricadeBlock.box((double)0.0, (double)0.0, (double)6.0, (double)16.0, (double)1.0, (double)10.0), (VoxelShape)BarricadeBlock.box((double)0.0, (double)1.0, (double)6.0, (double)16.0, (double)2.0, (double)10.0));
            case Direction.DOWN -> Shapes.or((VoxelShape)BarricadeBlock.box((double)0.0, (double)15.0, (double)6.0, (double)16.0, (double)16.0, (double)10.0), (VoxelShape)BarricadeBlock.box((double)0.0, (double)14.0, (double)6.0, (double)16.0, (double)15.0, (double)10.0));
        };
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{FACING});
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return (BlockState)this.defaultBlockState().setValue((Property)FACING, (Comparable)context.getNearestLookingDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return (BlockState)state.setValue((Property)FACING, (Comparable)rot.rotate((Direction)state.getValue((Property)FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation((Direction)state.getValue((Property)FACING)));
    }
}

