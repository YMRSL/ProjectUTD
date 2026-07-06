package com.scarasol.zombiekit.block;

import com.scarasol.zombiekit.entity.mechanics.UvLampEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpreadLight extends Block {

    public SpreadLight(Properties properties) {
        super(properties);
    }



    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public void neighborChanged(BlockState blockstate, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean moving) {
        if (!world.isClientSide()) {
            if (!world.getBlockState(fromPos).is(this)) {
                AABB area = new AABB(fromPos).inflate(UvLampEntity.RANGE);
                world.getEntitiesOfClass(
                        UvLampEntity.class, area
                ).forEach(uvLampEntity -> {
                    uvLampEntity.addBlockPos(fromPos);
                    uvLampEntity.addBlockPos(pos);
                });
            }
            boolean hasNeighbor = false;
            for (Direction direction : Direction.values()) {
                if (world.getBlockState(pos.relative(direction)).is(this)) {
                    hasNeighbor = true;
                    break;
                }
            }
            if (!hasNeighbor)
                world.removeBlock(pos, false);
        }
        super.neighborChanged(blockstate, world, pos, neighborBlock, fromPos, moving);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }



}
