package net.mcreator.doomsdaydecoration.block;

import net.mcreator.doomsdaydecoration.functionality.DoomsdayBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared behaviour for the lootable ("BlockEntity") variants of the Deco block family.
 *
 * <p>The four Deco base classes ({@link DecoBlockPlain}, {@link DecoBlockFacing},
 * {@link DecoBlockWaterlogged}, {@link DecoBlockFacingWaterlogged}) cannot share a
 * common subclass without losing their hard-coded blockstate property sets, so each
 * lootable variant ({@code DecoLootBlock*}) implements
 * {@link net.minecraft.world.level.block.EntityBlock} itself and delegates the
 * identical open / drop logic here.</p>
 *
 * <p>No ticker is registered: loot is rolled lazily when the menu is opened
 * ({@link DoomsdayBlockEntity#tryLoadLoot}), so the blocks never tick.</p>
 */
final class DecoLootBlockSupport {
    private DecoLootBlockSupport() {}

    /** A new lootable BlockEntity for any Deco loot variant. */
    static BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DoomsdayBlockEntity(pos, state);
    }

    /**
     * Empty-hand right-click opens the container.
     *
     * <p>Must fetch the BlockEntity directly: {@code state.getMenuProvider(...)} can NOT
     * be used because {@code BlockBehaviour#getMenuProvider} defaults to {@code null} and
     * only {@code BaseEntityBlock} overrides it to look up the BE. Our DecoLootBlock uses
     * the bare {@code EntityBlock} interface (it already extends a plain Deco block), so we
     * resolve the {@link MenuProvider} from the BlockEntity ourselves.
     */
    static InteractionResult openContainer(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MenuProvider provider) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Drops the container contents when the block is replaced/broken. Mirrors the
     * vanilla container behaviour but reads from the BE's {@link ItemStackHandler}
     * (our BE is not a {@code Container}). Must be called before
     * {@code super.onRemove} so the BlockEntity still exists.
     */
    static void dropContents(BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (state.is(newState.getBlock())) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof DoomsdayBlockEntity loot) {
            ItemStackHandler handler = loot.getLootHandler();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                }
            }
        }
    }
}
