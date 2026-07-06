package net.mcreator.survivalinstinct.world.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class GabageBagGUIMenu
extends AbstractContainerMenu
implements Supplier<Map<Integer, Slot>> {
    public static final HashMap<String, Object> guistate = new HashMap();
    public final Level world;
    public final Player entity;
    public int x;
    public int y;
    public int z;
    private ContainerLevelAccess access = ContainerLevelAccess.NULL;
    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<Integer, Slot>();
    private boolean bound = false;
    private Supplier<Boolean> boundItemMatcher = null;
    private Entity boundEntity = null;
    private BlockEntity boundBlockEntity = null;

    public GabageBagGUIMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super((MenuType)SurvivalInstinctModMenus.GABAGE_BAG_GUI.get(), id);
        int si;
        this.entity = inv.player;
        this.world = inv.player.level();
        this.internal = new ItemStackHandler(9);
        BlockPos pos = null;
        if (extraData != null) {
            pos = extraData.readBlockPos();
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.access = ContainerLevelAccess.create((Level)this.world, (BlockPos)pos);
        }
        if (pos != null) {
            if (extraData.readableBytes() == 1) {
                byte hand = extraData.readByte();
                ItemStack itemstack = hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem();
                this.boundItemMatcher = () -> itemstack == (hand == 0 ? this.entity.getMainHandItem() : this.entity.getOffhandItem());
                IItemHandler cap = itemstack.getCapability(Capabilities.ItemHandler.ITEM);
                if (cap != null) {
                    this.internal = cap;
                    this.bound = true;
                }
            } else if (extraData.readableBytes() > 1) {
                extraData.readByte();
                this.boundEntity = this.world.getEntity(extraData.readVarInt());
                if (this.boundEntity != null) {
                    IItemHandler cap = this.boundEntity.getCapability(Capabilities.ItemHandler.ENTITY, null);
                    if (cap != null) {
                        this.internal = cap;
                        this.bound = true;
                    }
                }
            } else {
                this.boundBlockEntity = this.world.getBlockEntity(pos);
                if (this.boundBlockEntity != null) {
                    IItemHandler cap = this.world.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    if (cap != null) {
                        this.internal = cap;
                        this.bound = true;
                    }
                }
            }
        }
        this.customSlots.put(0, this.addSlot((Slot)new SlotItemHandler(this.internal, 0, 7, 36)));
        this.customSlots.put(1, this.addSlot((Slot)new SlotItemHandler(this.internal, 1, 25, 36)));
        this.customSlots.put(2, this.addSlot((Slot)new SlotItemHandler(this.internal, 2, 43, 36)));
        this.customSlots.put(3, this.addSlot((Slot)new SlotItemHandler(this.internal, 3, 61, 36)));
        this.customSlots.put(4, this.addSlot((Slot)new SlotItemHandler(this.internal, 4, 79, 36)));
        this.customSlots.put(5, this.addSlot((Slot)new SlotItemHandler(this.internal, 5, 97, 36)));
        this.customSlots.put(6, this.addSlot((Slot)new SlotItemHandler(this.internal, 6, 115, 36)));
        this.customSlots.put(7, this.addSlot((Slot)new SlotItemHandler(this.internal, 7, 133, 36)));
        this.customSlots.put(8, this.addSlot((Slot)new SlotItemHandler(this.internal, 8, 151, 36)));
        for (si = 0; si < 3; ++si) {
            for (int sj = 0; sj < 9; ++sj) {
                this.addSlot(new Slot((Container)inv, sj + (si + 1) * 9, 8 + sj * 18, 84 + si * 18));
            }
        }
        for (si = 0; si < 9; ++si) {
            this.addSlot(new Slot((Container)inv, si, 8 + si * 18, 142));
        }
    }

    public boolean stillValid(Player player) {
        if (this.bound) {
            if (this.boundItemMatcher != null) {
                return this.boundItemMatcher.get();
            }
            if (this.boundBlockEntity != null) {
                return AbstractContainerMenu.stillValid((ContainerLevelAccess)this.access, (Player)player, (Block)this.boundBlockEntity.getBlockState().getBlock());
            }
            if (this.boundEntity != null) {
                return this.boundEntity.isAlive();
            }
        }
        return true;
    }

    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 9) {
                if (!this.moveItemStackTo(itemstack1, 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.moveItemStackTo(itemstack1, 0, 9, false)) {
                if (index < 36 ? !this.moveItemStackTo(itemstack1, 36, this.slots.size(), true) : !this.moveItemStackTo(itemstack1, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }
            if (itemstack1.getCount() == 0) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(playerIn, itemstack1);
        }
        return itemstack;
    }

    protected boolean moveItemStackTo(ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_) {
        boolean flag = false;
        int i = p_38905_;
        if (p_38907_) {
            i = p_38906_ - 1;
        }
        if (p_38904_.isStackable()) {
            while (!p_38904_.isEmpty() && !(!p_38907_ ? i >= p_38906_ : i < p_38905_)) {
                ItemStack itemstack;
                Slot slot = (Slot)this.slots.get(i);
                if (slot.mayPlace(itemstack = slot.getItem()) && !itemstack.isEmpty() && ItemStack.isSameItemSameComponents((ItemStack)p_38904_, (ItemStack)itemstack)) {
                    int maxSize;
                    int j = itemstack.getCount() + p_38904_.getCount();
                    if (j <= (maxSize = Math.min(slot.getMaxStackSize(), p_38904_.getMaxStackSize()))) {
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
                    continue;
                }
                ++i;
            }
        }
        if (!p_38904_.isEmpty()) {
            i = p_38907_ ? p_38906_ - 1 : p_38905_;
            while (!(!p_38907_ ? i >= p_38906_ : i < p_38905_)) {
                Slot slot1 = (Slot)this.slots.get(i);
                ItemStack itemstack1 = slot1.getItem();
                if (itemstack1.isEmpty() && slot1.mayPlace(p_38904_)) {
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
                    continue;
                }
                ++i;
            }
        }
        return flag;
    }

    public void removed(Player playerIn) {
        block4: {
            super.removed(playerIn);
            if (this.bound || !(playerIn instanceof ServerPlayer)) break block4;
            ServerPlayer serverPlayer = (ServerPlayer)playerIn;
            if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
                for (int j = 0; j < this.internal.getSlots(); ++j) {
                    playerIn.drop(this.internal.extractItem(j, this.internal.getStackInSlot(j).getCount(), false), false);
                }
            } else {
                for (int i = 0; i < this.internal.getSlots(); ++i) {
                    playerIn.getInventory().placeItemBackInInventory(this.internal.extractItem(i, this.internal.getStackInSlot(i).getCount(), false));
                }
            }
        }
    }

    @Override
    public Map<Integer, Slot> get() {
        return this.customSlots;
    }
}

