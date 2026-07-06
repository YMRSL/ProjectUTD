package com.scarasol.zombiekit.inventory;

import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.init.ZombieKitMenus;
import com.scarasol.zombiekit.item.projectile.MortarShell;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MortarRackMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {

    public final static HashMap<String, Object> guiState = new HashMap<>();
    public final Level level;
    public final Player player;
    public int x, y, z;
    private final ContainerLevelAccess access;
    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    private final BlockEntity boundBlockEntity;

    public MortarRackMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos());
    }

    public MortarRackMenu(int id, Inventory inv, BlockPos pos) {
        super(ZombieKitMenus.MORTAR_RACK_MENU.get(), id);
        this.player = inv.player;
        this.level = inv.player.level();
        this.internal = new ItemStackHandler(10);
        this.x = pos.getX();
        this.y = pos.getY();
        this.z = pos.getZ();
        access = ContainerLevelAccess.create(level, pos);
        boundBlockEntity = this.level.getBlockEntity(pos);
        if (boundBlockEntity != null) {
            IItemHandler capability = this.level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (capability != null)
                this.internal = capability;
        }
        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 3; ++sj)
                this.customSlots.put(sj + si * 3, this.addSlot(new SlotItemHandler(internal, sj + si * 3, 62 + sj * 18, 17 + si * 18) {

                    @Override
                    public void setChanged() {
                        super.setChanged();
                        if (boundBlockEntity != null)
                            boundBlockEntity.setChanged();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof MortarShell;
                    }
                }));

        this.customSlots.put(9, this.addSlot(new SlotItemHandler(internal, 9, 26, 35) {

            @Override
            public void setChanged() {
                super.setChanged();
                if (boundBlockEntity != null)
                    boundBlockEntity.setChanged();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ZombieKitItems.SHOOTING_PARAMETER.get());
            }
        }));

        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 8 + sj * 18, 84 + si * 18));
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(inv, si, 8 + si * 18, 142));

    }


    @Override
    public boolean stillValid(Player player) {
        if (this.boundBlockEntity != null)
            return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 0) {
                if (!this.moveItemStackTo(itemstack1, 0, this.slots.size(), true))
                    return ItemStack.EMPTY;
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.moveItemStackTo(itemstack1, 0, 0, false)) {
                if (index < 27) {
                    if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true))
                        return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(itemstack1, 0, 27, false))
                        return ItemStack.EMPTY;
                }
                slot.setChanged();
                return ItemStack.EMPTY;
            }

            if (itemstack1.getCount() == 0)
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();
            if (itemstack1.getCount() == itemstack.getCount())
                return ItemStack.EMPTY;
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    @Override
    protected boolean moveItemStackTo(ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_) {
        boolean flag = false;
        int i = p_38905_;
        if (p_38907_) {
            i = p_38906_ - 1;
        }
        if (p_38904_.isStackable()) {
            while (!p_38904_.isEmpty()) {
                if (p_38907_) {
                    if (i < p_38905_) {
                        break;
                    }
                } else if (i >= p_38906_) {
                    break;
                }
                Slot slot = this.slots.get(i);
                ItemStack itemstack = slot.getItem();
                if (slot.mayPlace(itemstack) && !itemstack.isEmpty() && ItemStack.isSameItemSameComponents(p_38904_, itemstack)) {
                    int j = itemstack.getCount() + p_38904_.getCount();
                    int maxSize = Math.min(slot.getMaxStackSize(), p_38904_.getMaxStackSize());
                    if (j <= maxSize) {
                        p_38904_.setCount(0);
                        itemstack.setCount(j);
                        slot.set(itemstack);
                        flag = true;
                    } else if (itemstack.getCount() < maxSize) {
                        p_38904_.shrink(maxSize - itemstack.getCount());
                        itemstack.setCount(maxSize);
                        slot.set(itemstack);
                        flag = true;
                    }
                }
                if (p_38907_) {
                    --i;
                } else {
                    ++i;
                }
            }
        }
        if (!p_38904_.isEmpty()) {
            if (p_38907_) {
                i = p_38906_ - 1;
            } else {
                i = p_38905_;
            }
            while (true) {
                if (p_38907_) {
                    if (i < p_38905_) {
                        break;
                    }
                } else if (i >= p_38906_) {
                    break;
                }
                Slot slot1 = this.slots.get(i);
                ItemStack itemStack1 = slot1.getItem();
                if (itemStack1.isEmpty() && slot1.mayPlace(p_38904_)) {
                    if (p_38904_.getCount() > slot1.getMaxStackSize()) {
                        slot1.setByPlayer(p_38904_.split(slot1.getMaxStackSize()));
                    } else {
                        slot1.setByPlayer(p_38904_.split(p_38904_.getCount()));
                    }
                    slot1.setChanged();
                    flag = true;
                    break;
                }
                if (p_38907_) {
                    --i;
                } else {
                    ++i;
                }
            }
        }
        return flag;
    }

    @Override
    public Map<Integer, Slot> get() {
        return customSlots;
    }
}
