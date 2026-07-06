package net.mcreator.survivalinstinct.block.entity;

import io.netty.buffer.Unpooled;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModBlockEntities;
import net.mcreator.survivalinstinct.world.inventory.TrashCanGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TrashCanBlockEntity
extends RandomizableContainerBlockEntity
implements WorldlyContainer {
    private NonNullList<ItemStack> stacks = NonNullList.withSize((int)27, ItemStack.EMPTY);

    public TrashCanBlockEntity(BlockPos position, BlockState state) {
        super((BlockEntityType)SurvivalInstinctModBlockEntities.TRASH_CAN.get(), position, state);
    }

    public void loadAdditional(CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(compound, provider);
        if (!this.tryLoadLootTable(compound)) {
            this.stacks = NonNullList.withSize((int)this.getContainerSize(), ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems((CompoundTag)compound, this.stacks, provider);
    }

    public void saveAdditional(CompoundTag compound, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(compound, provider);
        if (!this.trySaveLootTable(compound)) {
            ContainerHelper.saveAllItems((CompoundTag)compound, this.stacks, provider);
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider provider) {
        return this.saveWithFullMetadata(provider);
    }

    public int getContainerSize() {
        return this.stacks.size();
    }

    public boolean isEmpty() {
        for (ItemStack itemstack : this.stacks) {
            if (itemstack.isEmpty()) continue;
            return false;
        }
        return true;
    }

    public Component getDefaultName() {
        return Component.literal((String)"trash_can");
    }

    public int getMaxStackSize() {
        return 64;
    }

    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new TrashCanGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
    }

    public Component getDisplayName() {
        return Component.literal((String)"Trash Can");
    }

    protected NonNullList<ItemStack> getItems() {
        return this.stacks;
    }

    protected void setItems(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    public boolean canPlaceItem(int index, ItemStack stack) {
        return true;
    }

    public int[] getSlotsForFace(Direction side) {
        return IntStream.range(0, this.getContainerSize()).toArray();
    }

    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(index, stack);
    }

    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
        return true;
    }
}

