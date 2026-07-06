package net.mcreator.doomsdaydecoration.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import javax.annotation.Nullable;

/**
 * Lootable ({@code blockEntity:true}) variant of {@link DecoBlockWaterlogged}. Keeps
 * the WATERLOGGED blockstate intact and additionally carries a
 * {@code DoomsdayBlockEntity} container (opened on empty-hand use, drops on break).
 */
public class DecoLootBlockWaterlogged extends DecoBlockWaterlogged implements EntityBlock {
    public DecoLootBlockWaterlogged(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(DecoLootBlockWaterlogged::new);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return DecoLootBlockSupport.newBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return DecoLootBlockSupport.openContainer(state, level, pos, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        DecoLootBlockSupport.dropContents(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moved);
    }
}
