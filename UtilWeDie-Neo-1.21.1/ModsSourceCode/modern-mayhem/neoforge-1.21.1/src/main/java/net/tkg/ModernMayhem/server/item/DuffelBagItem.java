package net.tkg.ModernMayhem.server.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.tkg.ModernMayhem.server.block.DuffelBagBlock;
import net.tkg.ModernMayhem.server.block.entity.DuffelBagBlockEntity;
import net.tkg.ModernMayhem.server.registry.BlockRegistryMM;
import net.tkg.ModernMayhem.server.registry.ItemRegistryMM;

public class DuffelBagItem
extends BundleItem {
    protected final int size;

    public DuffelBagItem(Item.Properties properties, int size) {
        super(properties);
        this.size = size;
    }

    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        Direction clickedFace = context.getClickedFace();
        ItemStack stack = context.getItemInHand();
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        BlockPos targetPos = context.getClickedPos();
        BlockState targetState = level.getBlockState(targetPos);
        BlockPos pos = targetState.canBeReplaced() ? targetPos : targetPos.relative(clickedFace);
        BlockState placeState = level.getBlockState(pos);
        if (!placeState.canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy((BlockGetter)level, below, Direction.UP)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            boolean waterlogged = level.getFluidState(pos).getType() == Fluids.WATER;
            Direction facing = player.getDirection().getOpposite();
            BlockState placedState = (BlockState)((BlockState)((Block)BlockRegistryMM.DUFFEL_BAG_BLOCK.get()).defaultBlockState().setValue((Property)DuffelBagBlock.FACING, (Comparable)facing)).setValue((Property)DuffelBagBlock.WATERLOGGED, (Comparable)Boolean.valueOf(waterlogged));
            AABB box = ((Block)BlockRegistryMM.DUFFEL_BAG_BLOCK.get()).defaultBlockState().getCollisionShape((BlockGetter)level, pos).bounds().move(pos);
            List blockingEntities = level.getEntities((Entity)null, box, entity -> entity.isAlive() && !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrb) && !(entity instanceof AbstractArrow) && !(entity instanceof ThrownTrident));
            if (!blockingEntities.isEmpty()) {
                return InteractionResult.FAIL;
            }
            if (level.setBlock(pos, placedState, 3)) {
                level.gameEvent((Entity)player, GameEvent.BLOCK_PLACE, pos);
                SoundType soundType = placedState.getSoundType((LevelReader)level, pos, (Entity)player);
                level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f, soundType.getPitch());
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof DuffelBagBlockEntity) {
                    DuffelBagBlockEntity entity2 = (DuffelBagBlockEntity)blockEntity;
                    entity2.setDuffelBag(stack.copy());
                }
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.SUCCESS;
    }

    public boolean overrideStackedOnOther(@Nonnull ItemStack stack, @Nonnull Slot slot, @Nonnull ClickAction action, @Nonnull Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack var5 = slot.getItem();
        if (var5.isEmpty()) {
            DuffelBagItem.removeOne(stack).ifPresent(p_150740_ -> {
                DuffelBagItem.add(stack, slot.safeInsert(p_150740_), this.size, player);
                this.playRemoveOneSound((Entity)player);
            });
        } else if (var5.getItem().canFitInsideContainerItems()) {
            int var6 = (this.size - DuffelBagItem.getContentWeight(stack, 64)) / DuffelBagItem.getWeight(var5, 64);
            DuffelBagItem.add(stack, slot.safeTake(var5.getCount(), var6, player), this.size, player);
            this.playInsertSound((Entity)player);
        }
        return true;
    }

    public boolean overrideOtherStackedOnMe(@Nonnull ItemStack stack1, @Nonnull ItemStack stack2, @Nonnull Slot slot, @Nonnull ClickAction action, @Nonnull Player player, @Nonnull SlotAccess slotAccess) {
        if (action == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (stack2.isEmpty()) {
                Optional<ItemStack> var10000 = DuffelBagItem.removeOne(stack1);
                var10000.ifPresent(arg_0 -> slotAccess.set(arg_0));
                this.playRemoveOneSound((Entity)player);
            } else {
                stack2.shrink(DuffelBagItem.add(stack1, stack2, this.size, player));
                this.playInsertSound((Entity)player);
            }
            return true;
        }
        return false;
    }

    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, Player player, @Nonnull InteractionHand hand) {
        ItemStack var4 = player.getItemInHand(hand);
        if (DuffelBagItem.dropContents(var4, player)) {
            player.awardStat(Stats.ITEM_USED.get((Item)this));
            this.playDropContentsSound((Entity)player);
            return InteractionResultHolder.sidedSuccess(var4, level.isClientSide());
        }
        return InteractionResultHolder.fail(var4);
    }

    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return DuffelBagItem.getContentWeight(stack, 64) > 0;
    }

    public int getBarWidth(@Nonnull ItemStack stack) {
        return Math.min(1 + 12 * DuffelBagItem.getContentWeight(stack, 64) / this.size, 13);
    }

    public int getBarColor(@Nonnull ItemStack stack) {
        return super.getBarColor(stack);
    }

    @Nonnull
    public Optional<TooltipComponent> getTooltipImage(@Nonnull ItemStack stack) {
        return Optional.of(new BundleTooltip(stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)));
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, List<Component> components, @Nonnull TooltipFlag flag) {
        components.add((Component)Component.translatable((String)"item.minecraft.bundle.fullness", (Object[])new Object[]{DuffelBagItem.getContentWeight(stack, 64), this.size}).withStyle(ChatFormatting.GRAY));
    }

    public void onDestroyed(@Nonnull ItemEntity entity) {
        ItemUtils.onContainerDestroyed((ItemEntity)entity, DuffelBagItem.getContents(entity.getItem()).toList());
    }

    private static List<ItemStack> getItems(ItemStack bundleStack) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        bundleStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemCopyStream().forEach(items::add);
        return items;
    }

    private static void setItems(ItemStack bundleStack, List<ItemStack> items) {
        bundleStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
    }

    private static int add(ItemStack bundleStack, ItemStack addStack, int size, @Nullable Player player) {
        if (addStack.isEmpty() || !addStack.getItem().canFitInsideContainerItems()) {
            return 0;
        }
        int contentWeight = DuffelBagItem.getContentWeight(bundleStack, 64);
        int addStackWeight = DuffelBagItem.getWeight(addStack, 64);
        if (addStackWeight == 0) {
            if (player != null) {
                player.displayClientMessage((Component)Component.translatable((String)"text.betterbundles.stack_size_to_large"), false);
            }
            return 0;
        }
        int remainingSlots = Math.min(addStack.getCount(), (size - contentWeight) / addStackWeight);
        if (remainingSlots <= 0) {
            return 0;
        }
        int putSize = remainingSlots;
        List<ItemStack> items = DuffelBagItem.getItems(bundleStack);
        for (ItemStack existing : items) {
            if (remainingSlots <= 0) break;
            if (!ItemStack.isSameItemSameComponents((ItemStack)existing, (ItemStack)addStack) || existing.getCount() >= existing.getMaxStackSize()) continue;
            int free = Math.min(existing.getMaxStackSize() - existing.getCount(), remainingSlots);
            existing.grow(free);
            remainingSlots -= free;
        }
        if (remainingSlots > 0) {
            ItemStack copy = addStack.copy();
            copy.setCount(remainingSlots);
            items.add(0, copy);
        }
        DuffelBagItem.setItems(bundleStack, items);
        return putSize;
    }

    private static int getWeight(ItemStack stack, int size) {
        CustomData blockEntityData;
        if (stack.is((Item)ItemRegistryMM.DUFFEL_BAG.get())) {
            return 4 + DuffelBagItem.getContentWeight(stack, size);
        }
        if ((stack.is(Items.BEEHIVE) || stack.is(Items.BEE_NEST)) && (blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA)) != null && !blockEntityData.copyTag().getList("Bees", 10).isEmpty()) {
            return size;
        }
        return size / stack.getMaxStackSize();
    }

    private static int getContentWeight(ItemStack bundleStack, int size) {
        return DuffelBagItem.getContents(bundleStack).mapToInt(stack -> DuffelBagItem.getWeight(stack, size) * stack.getCount()).sum();
    }

    private static Optional<ItemStack> removeOne(ItemStack stack) {
        List<ItemStack> items = DuffelBagItem.getItems(stack);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        ItemStack removed = items.remove(0);
        DuffelBagItem.setItems(stack, items);
        return Optional.of(removed);
    }

    private static boolean dropContents(ItemStack stack, Player player) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) {
            return false;
        }
        stack.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        if (player instanceof ServerPlayer) {
            contents.itemCopyStream().forEach(stack2 -> player.drop(stack2, true));
        }
        return true;
    }

    private static Stream<ItemStack> getContents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY).itemCopyStream();
    }

    private void playRemoveOneSound(Entity pEntity) {
        pEntity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + pEntity.level().getRandom().nextFloat() * 0.4f);
    }

    private void playInsertSound(Entity pEntity) {
        pEntity.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + pEntity.level().getRandom().nextFloat() * 0.4f);
    }

    private void playDropContentsSound(Entity pEntity) {
        pEntity.playSound(SoundEvents.BUNDLE_DROP_CONTENTS, 0.8f, 0.8f + pEntity.level().getRandom().nextFloat() * 0.4f);
    }
}
