package net.mcreator.survivalinstinct.block;

import net.mcreator.survivalinstinct.procedures.WireTrapEntityCollidesInTheBlockProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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

public class WireTrapBlock
extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public WireTrapBlock() {
        super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.WOOD).strength(2.0f, 10.0f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
            default -> Shapes.or((VoxelShape)WireTrapBlock.box((double)14.0, (double)0.0, (double)7.0, (double)16.0, (double)9.0, (double)9.0), (VoxelShape)WireTrapBlock.box((double)0.0, (double)0.0, (double)7.0, (double)2.0, (double)9.0, (double)9.0));
            case Direction.NORTH -> Shapes.or((VoxelShape)WireTrapBlock.box((double)0.0, (double)0.0, (double)7.0, (double)2.0, (double)9.0, (double)9.0), (VoxelShape)WireTrapBlock.box((double)14.0, (double)0.0, (double)7.0, (double)16.0, (double)9.0, (double)9.0));
            case Direction.EAST -> Shapes.or((VoxelShape)WireTrapBlock.box((double)7.0, (double)0.0, (double)0.0, (double)9.0, (double)9.0, (double)2.0), (VoxelShape)WireTrapBlock.box((double)7.0, (double)0.0, (double)14.0, (double)9.0, (double)9.0, (double)16.0));
            case Direction.WEST -> Shapes.or((VoxelShape)WireTrapBlock.box((double)7.0, (double)0.0, (double)14.0, (double)9.0, (double)9.0, (double)16.0), (VoxelShape)WireTrapBlock.box((double)7.0, (double)0.0, (double)0.0, (double)9.0, (double)9.0, (double)2.0));
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

    public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
        super.entityInside(blockstate, world, pos, entity);
        WireTrapEntityCollidesInTheBlockProcedure.execute((LevelAccessor)world, pos.getX(), pos.getY(), pos.getZ(), entity);
    }
}

