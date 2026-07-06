package com.scarasol.zombiekit.block;

import com.scarasol.zombiekit.entity.projectile.LandmineEntity;
import com.scarasol.zombiekit.init.ZombieKitBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LandmineBlock extends Block implements BombBlock {

    public List<Tuple<Tuple<String, Integer>, Integer>> areaEffectCloudEffect = new ArrayList<>();

    public LandmineBlock(Properties properties, List<Tuple<Tuple<String, Integer>, Integer>> areaEffectCloudEffect) {
        super(properties);
        this.areaEffectCloudEffect.addAll(areaEffectCloudEffect);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Vec3 offset = state.getOffset(world, pos);
        return box(0, 0, 0, 16, 1, 16).move(offset.x, offset.y, offset.z);
    }

    @Override
    public boolean canSurvive(BlockState blockstate, LevelReader worldIn, BlockPos pos) {
        if (worldIn instanceof LevelAccessor world) {
            return world.getBlockState(pos.below()).canOcclude();
        }
        return super.canSurvive(blockstate, worldIn, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos, BlockPos facingPos) {
        return !state.canSurvive(world, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, world, currentPos, facingPos);
    }

    @Override
    public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
        return PathType.WALKABLE;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    @Override
    public int getExplodeLevel() {
        return areaEffectCloudEffect.isEmpty() ? 4 : 1;
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
        if (player.getInventory().getSelected().getItem() instanceof ShovelItem tieredItem)
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
    public boolean onDestroyedByPlayer(BlockState blockstate, Level world, BlockPos pos, Player entity, boolean willHarvest, FluidState fluid) {
        boolean retval = super.onDestroyedByPlayer(blockstate, world, pos, entity, willHarvest, fluid);
        if (entity.getMainHandItem().getItem() instanceof ShovelItem || entity.isCreative())
            return retval;
        exploded(world, pos.getX(), pos.getY(), pos.getZ(), getExplodeLevel());
        return retval;
    }

    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion e) {
        super.wasExploded(world, pos, e);
        exploded(world, pos.getX(), pos.getY(), pos.getZ(), getExplodeLevel());
        if (!areaEffectCloudEffect.isEmpty())
            spawnAreaEffectCloud(world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void attack(BlockState blockstate, Level world, BlockPos pos, Player entity) {
        super.attack(blockstate, world, pos, entity);
        if (entity.getMainHandItem().getItem() instanceof ShovelItem || entity.isCreative())
            return;
        exploded(world, pos.getX(), pos.getY(), pos.getZ(), getExplodeLevel());
        if (!areaEffectCloudEffect.isEmpty())
            spawnAreaEffectCloud(world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void entityInside(BlockState blockstate, Level world, BlockPos pos, Entity entity) {
        super.entityInside(blockstate, world, pos, entity);
        if (entity instanceof LandmineEntity)
            return;
        exploded(world, pos.getX(), pos.getY(), pos.getZ(), getExplodeLevel());
        if (!areaEffectCloudEffect.isEmpty())
            spawnAreaEffectCloud(world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        exploded(world, x, y, z, getExplodeLevel());
        if (!areaEffectCloudEffect.isEmpty())
            spawnAreaEffectCloud(world, x, y, z);
        return InteractionResult.SUCCESS;
    }


    public void spawnAreaEffectCloud(Level level, int x, int y, int z){
        AreaEffectCloud areaEffectCloud = new AreaEffectCloud(level, x, y, z);
        areaEffectCloud.setDuration(200);
        areaEffectCloud.setRadius(4);
        for (Tuple<Tuple<String, Integer>, Integer> effectInstance : areaEffectCloudEffect){
            Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(effectInstance.getA().getA()));
            effect.ifPresent(mobEffectHolder -> areaEffectCloud.addEffect(new MobEffectInstance(mobEffectHolder, effectInstance.getA().getB(), effectInstance.getB(), false, false)));
        }
        level.addFreshEntity(areaEffectCloud);
    }

}
