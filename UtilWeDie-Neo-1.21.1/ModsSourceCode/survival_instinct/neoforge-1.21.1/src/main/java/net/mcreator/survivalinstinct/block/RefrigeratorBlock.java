package net.mcreator.survivalinstinct.block;

import io.netty.buffer.Unpooled;
import net.mcreator.survivalinstinct.block.entity.RefrigeratorBlockEntity;
import net.mcreator.survivalinstinct.world.inventory.RefrigeratorGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
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

public class RefrigeratorBlock
extends Block
implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public RefrigeratorBlock() {
        super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(5.0f, 10.0f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
            default -> Shapes.or((VoxelShape)RefrigeratorBlock.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0), (VoxelShape)RefrigeratorBlock.box((double)0.0, (double)16.0, (double)0.0, (double)16.0, (double)32.0, (double)16.0));
            case Direction.NORTH -> Shapes.or((VoxelShape)RefrigeratorBlock.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0), (VoxelShape)RefrigeratorBlock.box((double)0.0, (double)16.0, (double)0.0, (double)16.0, (double)32.0, (double)16.0));
            case Direction.EAST -> Shapes.or((VoxelShape)RefrigeratorBlock.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0), (VoxelShape)RefrigeratorBlock.box((double)0.0, (double)16.0, (double)0.0, (double)16.0, (double)32.0, (double)16.0));
            case Direction.WEST -> Shapes.or((VoxelShape)RefrigeratorBlock.box((double)0.0, (double)0.0, (double)0.0, (double)16.0, (double)16.0, (double)16.0), (VoxelShape)RefrigeratorBlock.box((double)0.0, (double)16.0, (double)0.0, (double)16.0, (double)32.0, (double)16.0));
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

    public InteractionResult useWithoutItem(BlockState blockstate, Level world, final BlockPos pos, Player entity, BlockHitResult hit) {
        super.useWithoutItem(blockstate, world, pos, entity, hit);
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            player.openMenu((MenuProvider)new MenuProvider(){

                public Component getDisplayName() {
                    return Component.literal((String)"Refrigerator");
                }

                public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
                    return new RefrigeratorGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(pos));
                }
            }, (BlockPos)pos);
        }
        return InteractionResult.SUCCESS;
    }

    public MenuProvider getMenuProvider(BlockState state, Level worldIn, BlockPos pos) {
        MenuProvider menuProvider;
        BlockEntity tileEntity = worldIn.getBlockEntity(pos);
        return tileEntity instanceof MenuProvider ? (menuProvider = (MenuProvider)tileEntity) : null;
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RefrigeratorBlockEntity(pos, state);
    }

    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int eventID, int eventParam) {
        super.triggerEvent(state, world, pos, eventID, eventParam);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity == null ? false : blockEntity.triggerEvent(eventID, eventParam);
    }

    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof RefrigeratorBlockEntity) {
                RefrigeratorBlockEntity be = (RefrigeratorBlockEntity)blockEntity;
                Containers.dropContents((Level)world, (BlockPos)pos, (Container)be);
                world.updateNeighbourForOutputSignal(pos, (Block)this);
            }
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    public int getAnalogOutputSignal(BlockState blockState, Level world, BlockPos pos) {
        BlockEntity tileentity = world.getBlockEntity(pos);
        if (tileentity instanceof RefrigeratorBlockEntity) {
            RefrigeratorBlockEntity be = (RefrigeratorBlockEntity)tileentity;
            return AbstractContainerMenu.getRedstoneSignalFromContainer((Container)be);
        }
        return 0;
    }
}

