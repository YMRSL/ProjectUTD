package com.yitianys.BlockZ.capability;

import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.init.BlockZDataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

public class NestedStorageItemHandler implements IItemHandler {
    private final Supplier<ItemStack> stackSupplier;
    private ItemStack observedStack = ItemStack.EMPTY;
    private ItemStackHandler cachedHandler = new ItemStackHandler(0);
    private CompoundTag observedInventoryTag = new CompoundTag();
    private int observedSlotCount = 0;

    public NestedStorageItemHandler(Supplier<ItemStack> stackSupplier) {
        this.stackSupplier = stackSupplier;
    }

    public void syncToStack() {
        refreshState(true);
    }

    private void refreshCache() {
        refreshState(false);
    }

    private void refreshState(boolean persistCacheChanges) {
        ItemStack currentStack = stackSupplier.get();
        int currentSlots = Math.max(BlockZConfigs.getBackpackSlots(currentStack), 0);
        CompoundTag currentInventoryTag = readInventoryTag(currentStack);
        CompoundTag cachedInventoryTag = snapshotCachedInventory();
        boolean cacheChanged = !cachedInventoryTag.equals(observedInventoryTag);
        boolean stackChanged = currentStack != observedStack || currentSlots != observedSlotCount;
        boolean externalInventoryChanged = !currentInventoryTag.equals(observedInventoryTag);

        if (stackChanged) {
            if (cacheChanged) {
                saveCurrent();
            }
            loadFromStack(currentStack, currentSlots, currentInventoryTag);
            return;
        }

        if (externalInventoryChanged && !cacheChanged) {
            loadFromStack(currentStack, currentSlots, currentInventoryTag);
            return;
        }

        if (persistCacheChanges && cacheChanged) {
            saveCurrent();
        }
    }

    private void loadFromStack(ItemStack currentStack, int slots, CompoundTag inventoryTag) {
        observedStack = currentStack;
        observedSlotCount = slots;
        observedInventoryTag = inventoryTag.copy();
        cachedHandler = new ItemStackHandler(slots);
        if (currentStack.isEmpty() || inventoryTag.isEmpty()) {
            return;
        }
        cachedHandler.deserializeNBT(provider(), inventoryTag);
    }

    private void saveCurrent() {
        if (observedStack.isEmpty()) {
            return;
        }
        boolean hasItems = hasAnyItems();
        if (!hasItems) {
            // 清空：移除内嵌库存组件
            observedStack.remove(BlockZDataComponents.BACKPACK_INVENTORY.get());
            observedInventoryTag = new CompoundTag();
            return;
        }
        CompoundTag serialized = cachedHandler.serializeNBT(provider());
        observedStack.set(BlockZDataComponents.BACKPACK_INVENTORY.get(), serialized.copy());
        observedInventoryTag = serialized.copy();
    }

    private CompoundTag readInventoryTag(ItemStack stack) {
        if (stack.isEmpty()) {
            return new CompoundTag();
        }
        CompoundTag tag = stack.get(BlockZDataComponents.BACKPACK_INVENTORY.get());
        if (tag == null) {
            return new CompoundTag();
        }
        return tag.copy();
    }

    private CompoundTag snapshotCachedInventory() {
        if (!hasAnyItems()) {
            return new CompoundTag();
        }
        return cachedHandler.serializeNBT(provider());
    }

    private boolean hasAnyItems() {
        for (int i = 0; i < cachedHandler.getSlots(); i++) {
            if (!cachedHandler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidSlot(int slot) {
        refreshCache();
        return slot >= 0 && slot < cachedHandler.getSlots();
    }

    /**
     * 解析当前上下文的 HolderLookup.Provider（ItemStackHandler 序列化所需）。
     * 服务端走运行中的 server.registryAccess()；客户端走 Minecraft 当前世界。
     */
    private static HolderLookup.Provider provider() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            HolderLookup.Provider clientProvider = ClientProviderAccess.get();
            if (clientProvider != null) {
                return clientProvider;
            }
        }
        throw new IllegalStateException("BlockZ: no HolderLookup.Provider available for nested storage serialization");
    }

    /** 客户端 registryAccess 访问隔离到内部类，避免服务端加载 Minecraft 类。 */
    private static final class ClientProviderAccess {
        private static HolderLookup.Provider get() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.level != null) {
                return mc.level.registryAccess();
            }
            return null;
        }
    }

    @Override
    public int getSlots() {
        refreshCache();
        return cachedHandler.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return cachedHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = cachedHandler.insertItem(slot, stack, simulate);
        if (!simulate) {
            saveCurrent();
        }
        return remaining;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = cachedHandler.extractItem(slot, amount, simulate);
        if (!simulate && !extracted.isEmpty()) {
            saveCurrent();
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (!isValidSlot(slot)) {
            return 0;
        }
        return cachedHandler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return isValidSlot(slot) && cachedHandler.isItemValid(slot, stack);
    }
}
