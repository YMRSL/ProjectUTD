package com.scarasol.zombiekit.inventory;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.init.ZombieKitMenus;
import com.scarasol.zombiekit.item.weapon.parts.BattleParts;
import com.scarasol.zombiekit.item.weapon.parts.ChargingParts;
import com.scarasol.zombiekit.item.weapon.parts.GripParts;
import com.scarasol.zombiekit.item.api.ModifiableWeapon;
import com.scarasol.zombiekit.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class WeaponModificationMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
    public final static HashMap<String, Object> guiState = new HashMap<>();
    public final Level world;
    public final Player player;

    private IItemHandler internal;
    private final Map<Integer, Slot> customSlots = new HashMap<>();
    public ItemStack itemStack;

    public WeaponModificationMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(ZombieKitMenus.WEAPON_MODIFICATION_GUI.get(), id);
        this.player = inv.player;
        this.world = inv.player.level();
        this.internal = new ItemStackHandler(3);
        itemStack = player.getMainHandItem();
        this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 41, 16) {
            private final int slot = 0;

            @Override
            public void setChanged() {
                slotChanged(slot);
                super.setChanged();

            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof BattleParts battleParts && battleParts.canUse(itemStack);
            }
        }));

        this.customSlots.put(1, this.addSlot(new SlotItemHandler(internal, 1, 145, 51) {
            private final int slot = 1;

            @Override
            public void setChanged() {
                slotChanged(slot);
                super.setChanged();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ChargingParts chargingParts && chargingParts.canUse(itemStack);
            }
        }));

        this.customSlots.put(2, this.addSlot(new SlotItemHandler(internal, 2, 21, 61) {
            private final int slot = 2;

            @Override
            public void setChanged() {
                slotChanged(slot);
                super.setChanged();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GripParts gripParts && gripParts.canUse(itemStack);
            }
        }));

        if (itemStack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
            if (modifiableWeapon.getBattleParts(itemStack) != null)
                customSlots.get(0).set(new ItemStack(modifiableWeapon.getBattleParts(itemStack)));
            if (modifiableWeapon.getChargingParts(itemStack) != null)
                customSlots.get(1).set(new ItemStack(modifiableWeapon.getChargingParts(itemStack)));
            if (modifiableWeapon.getGripParts(itemStack) != null)
                customSlots.get(2).set(new ItemStack(modifiableWeapon.getGripParts(itemStack)));
        }

        for (int si = 0; si < 3; ++si)
            for (int sj = 0; sj < 9; ++sj)
                this.addSlot(new Slot(inv, sj + (si + 1) * 9, 2 + 8 + sj * 18, 9 + 84 + si * 18));
        for (int si = 0; si < 9; ++si)
            this.addSlot(new Slot(inv, si, 2 + 8 + si * 18, 9 + 142));

    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.getMainHandItem() == itemStack;
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
                if (slot.mayPlace(itemstack) && !itemstack.isEmpty() && ItemStack.isSameItemSameTags(p_38904_, itemstack)) {
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

//    @Override
//    public void removed(Player playerIn) {
//        super.removed(playerIn);
//        if (!bound && playerIn instanceof ServerPlayer serverPlayer) {
//            if (!serverPlayer.isAlive() || serverPlayer.hasDisconnected()) {
//                for (int j = 0; j < internal.getSlots(); ++j) {
//                    playerIn.drop(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
//                }
//            } else {
//                for (int i = 0; i < internal.getSlots(); ++i) {
//                    playerIn.getInventory().placeItemBackInInventory(internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
//                }
//            }
//        }
//    }

    public Map<Integer, Slot> get() {
        return customSlots;
    }

    private void slotChanged(int slotId) {
        Slot slot = customSlots.get(slotId);
        ItemStack itemStack = slot.getItem();
        if (this.itemStack.getItem() instanceof ModifiableWeapon modifiableWeapon) {
            if (itemStack.isEmpty()) {
                if (slotId == 0)
                    modifiableWeapon.clearBattleParts(this.itemStack);
                else if (slotId == 1)
                    modifiableWeapon.clearChargingParts(this.itemStack);
                else
                    modifiableWeapon.clearGripParts(this.itemStack);
            }else if (itemStack.getItem() instanceof GripParts gripParts)
                modifiableWeapon.setGripParts(this.itemStack, gripParts);
            else if (itemStack.getItem() instanceof ChargingParts chargingParts)
                modifiableWeapon.setChargingParts(this.itemStack, chargingParts);
            else if (itemStack.getItem() instanceof BattleParts battleParts)
                modifiableWeapon.setBattleParts(this.itemStack, battleParts);
        }

    }

}
