package com.scarasol.zombiekit.block;

import com.mojang.serialization.MapCodec;
import com.scarasol.zombiekit.block.entity.ShortwaveRadioBlockEntity;
import com.scarasol.zombiekit.init.ZombieKitBlockEntities;
import com.scarasol.zombiekit.init.ZombieKitBlocks;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.network.MapVariables;
import com.scarasol.zombiekit.network.SyncBlockPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShortwaveRadioBlock extends BaseEntityBlock{
    public static final MapCodec<ShortwaveRadioBlock> CODEC = simpleCodec(ShortwaveRadioBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty TURN_ON = BooleanProperty.create("turn_on");
    public static final IntegerProperty TIME = IntegerProperty.create("time", 0, 1);


    private static final Map<Level, Set<BlockPos>> workRadios = new HashMap<>();


    public ShortwaveRadioBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIME, 0).setValue(TURN_ON, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return box(0, 0, 0, 16, 12, 16);
    }

    @Override
    protected RenderShape getRenderShape(BlockState p_49232_) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShortwaveRadioBlockEntity(pos, state);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("block.zombiekit.shortwave_radio.description_1"));
        list.add(Component.translatable("block.zombiekit.shortwave_radio.description_2"));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof ShortwaveRadioBlockEntity blockEntity) {
                PacketDistributor.sendToPlayer(serverPlayer, new SyncBlockPacket(0, pos, blockEntity.getContent()));
                serverPlayer.openMenu(getMenuProvider(blockState, level, pos), pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static BlockPos findNearestRadio(BlockPos pos, Level world){
        Set<BlockPos> levelRadios = getLevelRadios(world);
        if (levelRadios.size() == 0)
            return null;
        BlockPos nearest = null;
        int nearestDistance = Integer.MAX_VALUE;
        int x = pos.getX();
        int z = pos.getZ();
        for (BlockPos radio : levelRadios){
            if (!(world.getBlockState(radio).getBlock() == ZombieKitBlocks.SHORTWAVE_RADIO.get())){
                removeRadio(radio, world);
                continue;
            }
            int distance = Math.abs(x - radio.getX()) + Math.abs(z - radio.getZ());
            if (distance < nearestDistance){
                nearestDistance = distance;
                nearest = radio;
            }
        }
        return nearest;
    }

    public static void saveRadioString(Level world){
        String workRadioToString = "";
        Set<BlockPos> levelRadios = getLevelRadios(world);
        if (levelRadios.size() != 0){
            for (BlockPos pos : levelRadios){
                String buffer = pos.getX() + "," + pos.getY() + "," + pos.getZ() + ";";
                workRadioToString += buffer;
            }
        }
        MapVariables.get(world).radioLocation = workRadioToString;
        MapVariables.get(world).syncData(world);
    }

    public static void loadRadioString(LevelAccessor world){
        String workRadioToString = MapVariables.get(world).radioLocation;
        if ("\"\"".equals(workRadioToString))
            workRadioToString = "";
        if (!"".equals(workRadioToString)){
            Set<BlockPos> levelRadios = new HashSet<>();
            for (String posStr : workRadioToString.split(";")){
                String[] pos = posStr.split(",");
                levelRadios.add(new BlockPos(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2])));
            }
            workRadios.put((Level) world, levelRadios);
        }
    }

    public static void addRadio(BlockPos pos, Level world){
        getLevelRadios(world).add(pos);
        saveRadioString(world);
    }

    public static void removeRadio(BlockPos pos, Level world){
        getLevelRadios(world).remove(pos);
        saveRadioString(world);
    }

    public static boolean hasRadio(BlockPos pos, Level world) {
        return getLevelRadios(world).contains(pos);
    }

    public static Set<BlockPos> getLevelRadios(Level world) {
        if (!workRadios.containsKey(world))
            workRadios.put(world, new HashSet<>());
        return workRadios.get(world);
    }



    @Override
    public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block block, BlockPos blockPos2, boolean bl) {
        boolean bl2 = world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.above());
        if (!bl2 && blockstate.getValue(TURN_ON)){
            removeRadio(pos, world);
            if (world.getBlockEntity(pos) instanceof ShortwaveRadioBlockEntity blockEntity)
                blockEntity.clearTime();
            world.setBlock(pos, blockstate.setValue(TURN_ON, false), 3);
        }else if (bl2 && !blockstate.getValue(TURN_ON)){
            addRadio(pos, world);
            world.setBlock(pos, blockstate.setValue(TURN_ON, true), 3);
            world.playSound(null, pos, ZombieKitSounds.radio_static.get(), SoundSource.BLOCKS, 1, 1);
        }
    }

    @Override
    public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(blockstate, world, pos, oldState, moving);
        world.scheduleTick(pos, this, 20);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if(state.getValue(TURN_ON))
                removeRadio(pos, world);
            super.onRemove(state, world, pos, newState, isMoving);
        }else if(state.getValue(TURN_ON) && !newState.getValue(TURN_ON)){
            removeRadio(pos, world);
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(TURN_ON);
        builder.add(TIME);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel().hasNeighborSignal(context.getClickedPos()) || context.getLevel().hasNeighborSignal(context.getClickedPos().above())){
            addRadio(context.getClickedPos(), context.getLevel());
            return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(TURN_ON, true);
        }
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(TURN_ON, false);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        if (player.getInventory().getSelected().getItem() instanceof PickaxeItem tieredItem)
            return HarvestTiers.getLevel(tieredItem.getTier()) >= 1;
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> dropsOriginal = super.getDrops(state, builder);
        if (!dropsOriginal.isEmpty())
            return dropsOriginal;
        return Collections.singletonList(new ItemStack(this, 1));
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Nullable
    protected static <T extends BlockEntity> BlockEntityTicker<T> createShortwaveRadioTicker(Level level, BlockEntityType<T> blockEntityType1, BlockEntityType<? extends ShortwaveRadioBlockEntity> blockEntityType2) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType1, blockEntityType2, ShortwaveRadioBlockEntity::serverTick);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState blockState, BlockEntityType<T> blockEntityType) {
        return createShortwaveRadioTicker(level, blockEntityType, (BlockEntityType<ShortwaveRadioBlockEntity>) ZombieKitBlockEntities.SHORTWAVE_RADIO.get());
    }
}
