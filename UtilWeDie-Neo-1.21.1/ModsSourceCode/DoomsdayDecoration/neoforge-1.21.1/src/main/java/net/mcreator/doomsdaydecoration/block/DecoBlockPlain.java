package net.mcreator.doomsdaydecoration.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Decoration block with no extra blockstate properties.
 *
 * Part of the DecoBlock family that fixes the construction-order crash: the
 * original single {@code DecoBlock} read instance fields ({@code hasFacing}/
 * {@code hasWaterlogged}) inside {@code createBlockStateDefinition}, but that
 * method runs during {@code super(props)} before subclass fields are assigned,
 * so the properties were never added and {@code setValue} later threw
 * "Cannot set property ...". Each subclass now hardcodes its property set, so
 * {@code createBlockStateDefinition} is correct even during super construction.
 */
public class DecoBlockPlain extends Block implements DecoShaped {
    public DecoBlockPlain(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(DecoBlockPlain::new);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = DecoShapeStore.shape(this, null);
        return s != null ? s : super.getShape(state, level, pos, ctx);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape s = DecoShapeStore.collisionShape(this, null);
        return s != null ? s : super.getCollisionShape(state, level, pos, ctx);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return DecoShapeStore.has(this) ? Shapes.empty() : super.getVisualShape(state, level, pos, ctx);
    }

    @Override
    public boolean ddOversized() {
        return DecoShapeStore.oversized(this);
    }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                            net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (DecoShapeStore.oversized(this)) DecoFillerManager.onPlaced(level, pos, this, null);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                            BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && DecoShapeStore.oversized(this)) {
            DecoFillerManager.onRemoved(level, pos, this, null);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
