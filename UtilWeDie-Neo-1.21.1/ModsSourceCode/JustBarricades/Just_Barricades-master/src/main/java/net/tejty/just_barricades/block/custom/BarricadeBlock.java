package net.tejty.just_barricades.block.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import net.tejty.just_barricades.config.JustBarricadesCommonConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class BarricadeBlock extends HorizontalDirectionalBlock {
    private static final HashMap<Direction, VoxelShape> PLANK_0 = createRotatedShape(Shapes.or(
            Block.box(10, 8, 8, 13, 16, 9),
            Block.box(9, 0, 8, 12, 8, 9)
    ));
    private static final HashMap<Direction, VoxelShape> PLANK_1 = createRotatedShape(Shapes.or(
            Block.box(2, 10, 8, 5, 16, 9),
            Block.box(3, 4, 8, 6, 10, 9),
            Block.box(4, 0, 8, 7, 4, 9)
    ));
    private static final HashMap<Direction, VoxelShape> PLANK_2 = createRotatedShape(
            Block.box(0, 12, 7, 16, 15, 8)
    );
    private static final HashMap<Direction, VoxelShape> PLANK_3 = createRotatedShape(Shapes.or(
            Block.box(0, 5, 7, 4, 8, 8),
            Block.box(4, 6, 7, 10, 9, 8),
            Block.box(10, 7, 7, 16, 10, 8)
    ));
    private static final HashMap<Direction, VoxelShape> PLANK_4 = createRotatedShape(Shapes.or(
            Block.box(8, 0, 7, 16, 3, 8),
            Block.box(0, 1, 7, 8, 4, 8)
    ));
    private static final HashMap<Direction, VoxelShape> FULL = createRotatedShape(
            Block.box(0, 0, 7, 16, 16, 9)
    );

    public static final IntegerProperty HEALTH = IntegerProperty.create("health", 0, 3);

    public BarricadeBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HEALTH, 3));
    }

    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite()).setValue(HEALTH, 3);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, HEALTH);
    }

    private static VoxelShape rotated(VoxelShape shape, int direction) {
        List<AABB> aabbs = shape.toAabbs();
        for (int i = 0; i < aabbs.size(); i++) {
            for (int j = 0; j < direction; j++) {
                AABB aabb = aabbs.get(i);
                aabbs.set(i, new AABB(1-aabb.minZ, aabb.minY, aabb.maxX, 1-aabb.maxZ, aabb.maxY, aabb.minX));
            }
        }
        VoxelShape toReturn = Shapes.empty();
        for (AABB aabb : aabbs) {
            toReturn = Shapes.or(toReturn, Shapes.create(aabb));
        }
        return toReturn;
    }

    private static HashMap<Direction, VoxelShape> createRotatedShape(VoxelShape shape) {
        HashMap<Direction, VoxelShape> toReturn = new HashMap<>();

        toReturn.put(Direction.NORTH, shape);
        toReturn.put(Direction.EAST, rotated(shape, 1));
        toReturn.put(Direction.SOUTH, rotated(shape, 2));
        toReturn.put(Direction.WEST, rotated(shape, 3));

        return toReturn;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.or(
                pState.getValue(HEALTH) > 1 ? PLANK_0.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 2 ? PLANK_1.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 0 ? PLANK_2.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 2 ? PLANK_3.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 0 ? PLANK_4.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) == 0 ? FULL.get(pState.getValue(FACING)) : Shapes.empty()
        );
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.or(
                pState.getValue(HEALTH) > 1 ? PLANK_0.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 2 ? PLANK_1.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 0 ? PLANK_2.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 2 ? PLANK_3.get(pState.getValue(FACING)) : Shapes.empty(),
                pState.getValue(HEALTH) > 0 ? PLANK_4.get(pState.getValue(FACING)) : Shapes.empty()
        );
    }

    @Override
    public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
        return pType == PathComputationType.LAND;
    }

    @Override
    public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
        if (!pLevel.isClientSide()) {
            if (pEntity instanceof Monster monster && monster.isAlive()) {
                if (breakLayer(pLevel, pPos, pState, JustBarricadesCommonConfig.ZOMBIE_BREAK_CHANCE.get(), true)) {
                    monster.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pHand == InteractionHand.OFF_HAND) {
            return InteractionResult.FAIL;
        }

        if (pState.getValue(HEALTH) < 3) {
            String itemName = JustBarricadesCommonConfig.REPAIR_ITEM.get();
            if (!itemName.isEmpty() && !pPlayer.isCreative()) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));
                int slot = pPlayer.getInventory().findSlotMatchingItem(new ItemStack(item));
                if (slot == -1) {
                    return InteractionResult.FAIL;
                }
                else {
                    pPlayer.getInventory().getItem(slot).shrink(1);
                }
            }
            pLevel.setBlock(pPos, pState.cycle(HEALTH), 3);
            pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_BREAK, SoundSource.BLOCKS, 2F, 0.5F);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        if (breakLayer(pLevel, pPos, pState, 1D, false)) {
            pPlayer.stopUsingItem();
            pPlayer.swinging = false;
            pPlayer.swingTime = -1;
        }
    }

    public boolean breakLayer(Level pLevel, BlockPos pPos, BlockState pState, Double chance, boolean isZombie) {
        if (pState.getValue(HEALTH) > 0) {
            if (pLevel.getRandom().nextFloat() < chance) {
                pLevel.setBlock(pPos, pState.setValue(HEALTH, pState.getValue(HEALTH) - 1), 3);
                if (isZombie && !JustBarricadesCommonConfig.REPAIR_AFTER_ZOMBIE.get() && pLevel.getBlockState(pPos).getValue(HEALTH) == 0) {
                    pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
                }
                pLevel.playSound(null, pPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.BLOCKS);

                String itemName = JustBarricadesCommonConfig.REPAIR_ITEM.get();
                if (!itemName.isEmpty()) {
                    if (pLevel.getRandom().nextFloat() < JustBarricadesCommonConfig.DROP_REPAIR_ITEM_CHANCE.get()) {
                        ItemEntity itemEntity = new ItemEntity(pLevel, pPos.getX() + 0.5, pPos.getY() + 0.5, pPos.getZ() + 0.5, new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName))));
                        pLevel.addFreshEntity(itemEntity);
                    }
                }

                return true;
            }
        } else if (!isZombie && JustBarricadesCommonConfig.PLAYER_CAN_BREAK_COMPLETELY.get()) {
            pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 3);
        }
        return false;
    }

    @Override
    public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        return pState.getValue(HEALTH) == 0;
    }
}
