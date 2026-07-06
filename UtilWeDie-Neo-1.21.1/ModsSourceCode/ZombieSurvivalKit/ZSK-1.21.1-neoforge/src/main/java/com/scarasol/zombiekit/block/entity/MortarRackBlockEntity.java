package com.scarasol.zombiekit.block.entity;

import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitBlockEntities;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.inventory.MortarRackMenu;
import com.scarasol.zombiekit.item.projectile.MortarShell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.concurrent.atomic.AtomicReference;

public class MortarRackBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, GeoBlockEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected NonNullList<ItemStack> items = NonNullList.withSize(10, ItemStack.EMPTY);
    private static final int[] SLOTS = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};

    public MortarRackBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ZombieKitBlockEntities.MORTAR_RACK.get(), blockPos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, MortarRackBlockEntity blockEntity) {
        level.getEntitiesOfClass(MortarEntity.class, new AABB(pos).inflate(16D), mortarEntity -> !mortarEntity.getPassengers().isEmpty() && !(mortarEntity.getPassengers().get(0) instanceof Player) && (mortarEntity.getRack() == null))
                .forEach(mortarEntity -> mortarEntity.setRack(pos));
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items, provider);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        ContainerHelper.saveAllItems(compoundTag, this.items, provider);
    }


    public ItemStack popShell() {
        AtomicReference<ItemStack> shell = new AtomicReference<>(ItemStack.EMPTY);
        items.stream().filter(itemStack -> itemStack.getItem() instanceof MortarShell)
                .findFirst().ifPresent((itemStack) -> {
                    shell.set(itemStack.copy());
                    itemStack.shrink(1);
        });
        return shell.get();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.zombiekit.mortar_rack");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new MortarRackMenu(id, inventory, getBlockPos());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int id, ItemStack itemStack) {
        if (id != 9)
            return itemStack.getItem() instanceof MortarShell;
        return itemStack.is(ZombieKitItems.SHOOTING_PARAMETER.get());
    }

    @Override
    public boolean canPlaceItemThroughFace(int id, ItemStack itemStack, Direction direction) {
        return canPlaceItem(id, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int id, ItemStack itemStack, Direction direction) {
        return id != 9;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    public boolean isNoShell() {
        for(ItemStack itemstack : this.items) {
            if (itemstack.getItem() instanceof MortarShell) {
                return false;
            }
        }
        return true;
    }

    public boolean hasTable() {
        return !this.items.get(9).isEmpty();
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int id) {
        if (id > 9 || id < 0)
            return ItemStack.EMPTY;
        return items.get(id);
    }

    @Override
    public ItemStack removeItem(int slotId, int count) {
        return ContainerHelper.removeItem(this.items, slotId, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slotId) {
        return ContainerHelper.takeItem(this.items, slotId);
    }

    @Override
    public void setItem(int id, ItemStack itemStack) {
        this.items.set(id, itemStack);
        if (itemStack.getCount() > this.getMaxStackSize()) {
            itemStack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
