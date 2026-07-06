package com.scarasol.zombiekit.block.entity;

import com.scarasol.sona.manager.RotManager;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.init.ZombieKitBlockEntities;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.inventory.ShortwaveRadioMenu;
import com.scarasol.zombiekit.inventory.VacuumPackagingMachineMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;

public class VacuumPackagingMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());

    //0 食物 1 打包食物 2 打包袋 3 电池
    protected NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{1, 3};
    private static final int[] SLOTS_FOR_SIDES = new int[]{2, 3};

    private int vacuumPackagingTime;
    private final int maxVacuumPackagingTime = 200;
    private int batteryDuration;
    private final int maxBatteryDuration = 5;

    private final ContainerData dataAccess = new ContainerData() {
        public int get(int id) {
            return switch (id) {
                case 0 -> VacuumPackagingMachineBlockEntity.this.vacuumPackagingTime;
                case 1 -> VacuumPackagingMachineBlockEntity.this.maxVacuumPackagingTime;
                default -> 0;
            };
        }

        public void set(int id, int value) {
            if (id == 0) {
                VacuumPackagingMachineBlockEntity.this.vacuumPackagingTime = value;
            }

        }

        public int getCount() {
            return 2;
        }
    };

    public VacuumPackagingMachineBlockEntity(BlockPos position, BlockState state) {
        super(ZombieKitBlockEntities.VACUUM_PACKAGING_MACHINE.get(), position, state);
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items);
        this.vacuumPackagingTime = compoundTag.getInt("VacuumPackagingTime");
        this.batteryDuration = compoundTag.getInt("BatteryDuration");
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        ContainerHelper.saveAllItems(compoundTag, this.items);
        compoundTag.putInt("VacuumPackagingTime", this.vacuumPackagingTime);
        compoundTag.putInt("BatteryDuration", this.batteryDuration);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState blockState, VacuumPackagingMachineBlockEntity blockEntity) {
        if (blockEntity.canPackage()) {
            blockEntity.vacuumPackagingTime = (blockEntity.vacuumPackagingTime + 1) % blockEntity.maxVacuumPackagingTime;
            if (blockEntity.vacuumPackagingTime == 0) {
                blockEntity.packageFood();
            }
            blockEntity.setChanged();
        }else {
            if (blockEntity.vacuumPackagingTime > 0) {
                blockEntity.vacuumPackagingTime = 0;
                blockEntity.setChanged();
            }
        }
    }

    private void packageFood() {
        ItemStack food = getItem(0);
        ItemStack warpedFood = getItem(1);
        if (warpedFood.isEmpty()) {
            warpedFood = food.copy();
            warpedFood.setCount(1);
            RotManager.putWarp(warpedFood, true);

        } else {
            RotManager.rotWhenStack(warpedFood, RotManager.getRot(warpedFood), RotManager.getRot(food), warpedFood.getCount(), 1, level.getGameTime());
            warpedFood.grow(1);
        }
        batteryDuration = (batteryDuration + 1) % maxBatteryDuration;
        if (batteryDuration == 0) {
            ItemStack battery = getItem(3);
            battery.setDamageValue(battery.getDamageValue() + 1);
        }
        removeItem(0, 1);
        removeItem(2, 1);
        setItem(1, warpedFood);
    }

    public boolean canPackage() {
        if (hasPower()) {
            if (!this.items.get(2).isEmpty() && !this.items.get(0).isEmpty()) {
                if (this.items.get(1).isEmpty() || ItemStack.isSameItem(this.items.get(1), this.items.get(0)))
                    return items.get(1).getCount() < items.get(1).getMaxStackSize();
            }
        }
        return false;
    }

    private boolean hasPower() {
        return !items.get(3).isEmpty() && items.get(3).getDamageValue() < 100;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("zombiekit.container.vacuum_packaging_machine");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new VacuumPackagingMachineMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(getBlockPos()), dataAccess);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN)
            return SLOTS_FOR_DOWN;
        else if (direction == Direction.UP)
            return SLOTS_FOR_UP;
        return SLOTS_FOR_SIDES;
    }

    @Override
    public boolean canPlaceItem(int id, ItemStack itemStack) {
        if (id == 0)
            return itemStack.isEdible() && RotManager.canBeRotten(itemStack) && !RotManager.isWarped(itemStack);
        if (id == 1)
            return false;
        if (id == 2)
            return itemStack.getItem() == ZombieKitItems.PLASTIC_BAG.get();
        return itemStack.getItem() == ZombieKitItems.BATTERY.get();
    }

    @Override
    public boolean canPlaceItemThroughFace(int id, ItemStack itemStack, @Nullable Direction direction) {
        return canPlaceItem(id, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int id, ItemStack itemStack, Direction direction) {
        if (direction == Direction.DOWN) {
            if (id == 1)
                return true;
            else
                return id == 3 && itemStack.getDamageValue() >= 100;
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.size();
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
        if (id > 3 || id < 0)
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
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
            return handlers[facing.ordinal()].cast();
        return super.getCapability(capability, facing);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        for (LazyOptional<? extends IItemHandler> handler : handlers)
            handler.invalidate();
    }

}
