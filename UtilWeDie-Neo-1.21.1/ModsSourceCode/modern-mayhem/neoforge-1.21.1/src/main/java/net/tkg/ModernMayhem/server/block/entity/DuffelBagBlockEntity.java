package net.tkg.ModernMayhem.server.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.tkg.ModernMayhem.server.registry.BlockEntityRegistryMM;

public class DuffelBagBlockEntity
extends BlockEntity {
    private ItemStack duffelBag = ItemStack.EMPTY;

    public DuffelBagBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)BlockEntityRegistryMM.DUFFEL_BAG.get(), pos, state);
    }

    public void setDuffelBag(ItemStack stack) {
        this.duffelBag = stack.copy();
        this.setChanged();
    }

    public ItemStack getDuffelBag() {
        return this.duffelBag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.duffelBag.isEmpty()) {
            tag.put("Duffel", (Tag)this.duffelBag.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Duffel")) {
            this.duffelBag = ItemStack.parse(registries, tag.getCompound("Duffel")).orElse(ItemStack.EMPTY);
        }
    }
}

