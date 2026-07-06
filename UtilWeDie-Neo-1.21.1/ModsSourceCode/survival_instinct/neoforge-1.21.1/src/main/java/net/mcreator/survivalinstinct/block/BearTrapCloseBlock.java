package net.mcreator.survivalinstinct.block;

import net.mcreator.survivalinstinct.procedures.BearTrapCloseOnBlockRightClickedProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BearTrapCloseBlock
extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BearTrapCloseBlock() {
        super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.CHAIN).strength(1.0f, 10.0f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
            default -> Shapes.or((VoxelShape)BearTrapCloseBlock.box((double)5.0, (double)2.0, (double)6.0, (double)11.0, (double)3.0, (double)10.0), (VoxelShape[])new VoxelShape[]{BearTrapCloseBlock.box((double)7.0, (double)1.0, (double)3.0, (double)9.0, (double)2.0, (double)13.0), BearTrapCloseBlock.box((double)6.0, (double)1.0, (double)0.0, (double)10.0, (double)2.0, (double)3.0), BearTrapCloseBlock.box((double)6.0, (double)0.0, (double)2.0, (double)10.0, (double)1.0, (double)14.0), BearTrapCloseBlock.box((double)4.0, (double)0.0, (double)14.0, (double)12.0, (double)1.0, (double)16.0), BearTrapCloseBlock.box((double)4.0, (double)0.0, (double)0.0, (double)12.0, (double)1.0, (double)2.0)});
            case Direction.NORTH -> Shapes.or((VoxelShape)BearTrapCloseBlock.box((double)5.0, (double)2.0, (double)6.0, (double)11.0, (double)3.0, (double)10.0), (VoxelShape[])new VoxelShape[]{BearTrapCloseBlock.box((double)7.0, (double)1.0, (double)3.0, (double)9.0, (double)2.0, (double)13.0), BearTrapCloseBlock.box((double)6.0, (double)1.0, (double)13.0, (double)10.0, (double)2.0, (double)16.0), BearTrapCloseBlock.box((double)6.0, (double)0.0, (double)2.0, (double)10.0, (double)1.0, (double)14.0), BearTrapCloseBlock.box((double)4.0, (double)0.0, (double)0.0, (double)12.0, (double)1.0, (double)2.0), BearTrapCloseBlock.box((double)4.0, (double)0.0, (double)14.0, (double)12.0, (double)1.0, (double)16.0)});
            case Direction.EAST -> Shapes.or((VoxelShape)BearTrapCloseBlock.box((double)6.0, (double)2.0, (double)5.0, (double)10.0, (double)3.0, (double)11.0), (VoxelShape[])new VoxelShape[]{BearTrapCloseBlock.box((double)3.0, (double)1.0, (double)7.0, (double)13.0, (double)2.0, (double)9.0), BearTrapCloseBlock.box((double)0.0, (double)1.0, (double)6.0, (double)3.0, (double)2.0, (double)10.0), BearTrapCloseBlock.box((double)2.0, (double)0.0, (double)6.0, (double)14.0, (double)1.0, (double)10.0), BearTrapCloseBlock.box((double)14.0, (double)0.0, (double)4.0, (double)16.0, (double)1.0, (double)12.0), BearTrapCloseBlock.box((double)0.0, (double)0.0, (double)4.0, (double)2.0, (double)1.0, (double)12.0)});
            case Direction.WEST -> Shapes.or((VoxelShape)BearTrapCloseBlock.box((double)6.0, (double)2.0, (double)5.0, (double)10.0, (double)3.0, (double)11.0), (VoxelShape[])new VoxelShape[]{BearTrapCloseBlock.box((double)3.0, (double)1.0, (double)7.0, (double)13.0, (double)2.0, (double)9.0), BearTrapCloseBlock.box((double)13.0, (double)1.0, (double)6.0, (double)16.0, (double)2.0, (double)10.0), BearTrapCloseBlock.box((double)2.0, (double)0.0, (double)6.0, (double)14.0, (double)1.0, (double)10.0), BearTrapCloseBlock.box((double)0.0, (double)0.0, (double)4.0, (double)2.0, (double)1.0, (double)12.0), BearTrapCloseBlock.box((double)14.0, (double)0.0, (double)4.0, (double)16.0, (double)1.0, (double)12.0)});
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

    public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
        super.useWithoutItem(blockstate, world, pos, entity, hit);
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        double hitX = hit.getLocation().x;
        double hitY = hit.getLocation().y;
        double hitZ = hit.getLocation().z;
        Direction direction = hit.getDirection();
        BearTrapCloseOnBlockRightClickedProcedure.execute((LevelAccessor)world, x, y, z);
        return InteractionResult.SUCCESS;
    }
}

