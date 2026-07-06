package net.tkg.ModernMayhem.server.util;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.tkg.ModernMayhem.server.util.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractContainerMenuUtil
extends AbstractContainerMenu
implements ContainerUtil {
    public int getPlayerInventoryTopPos(int numberOfLineInBackpack) {
        return numberOfLineInBackpack * 18 + 4 + 14;
    }

    public int getPlayerInventoryLeftPos(int numberOfColumnInBackpack) {
        if (numberOfColumnInBackpack > 9) {
            int backpackInventoryWidth = 7 + 18 * numberOfColumnInBackpack + 7;
            return (backpackInventoryWidth - 176) / 2;
        }
        return 8;
    }

    public int getPlayerHotbarTopPos(int numberOfLineInBackpack) {
        return this.getPlayerInventoryTopPos(numberOfLineInBackpack) + 54 + 4;
    }

    public int getBackpackInventoryLeftPos(int numberOfColumnInBackpack) {
        if (numberOfColumnInBackpack < 9) {
            int backpackInventoryWidth = 7 + 18 * numberOfColumnInBackpack + 7;
            return (176 - backpackInventoryWidth) / 2 + 7;
        }
        return 7;
    }

    protected AbstractContainerMenuUtil(@Nullable MenuType<?> pMenuType, int pContainerId) {
        super(pMenuType, pContainerId);
    }

    public void createPlayerInventory(Inventory playerInventory, final int lockedSlotID, boolean applyLock, int numberOfLineInBackpack, int numberOfColumnInBackpack) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                if (applyLock) {
                    this.addSlot(new Slot((Container)playerInventory, 9 + column + row * 9, this.getPlayerInventoryLeftPos(numberOfColumnInBackpack) + column * 18, this.getPlayerInventoryTopPos(numberOfLineInBackpack) + row * 18){

                        public boolean mayPickup(@NotNull Player pPlayer) {
                            if (this.getContainerSlot() == lockedSlotID) {
                                return false;
                            }
                            return super.mayPickup(pPlayer);
                        }

                        public boolean mayPlace(@NotNull ItemStack pStack) {
                            if (this.getContainerSlot() == lockedSlotID) {
                                return false;
                            }
                            return super.mayPlace(pStack);
                        }
                    });
                    continue;
                }
                this.addSlot(new Slot((Container)playerInventory, 9 + column + row * 9, this.getPlayerInventoryLeftPos(numberOfColumnInBackpack) + column * 18, this.getPlayerInventoryTopPos(numberOfLineInBackpack) + row * 18));
            }
        }
    }

    public void createPlayerHotbar(Inventory playerInventory, final int lockedSlotID, boolean applyLock, int numberOfLineInBackpack, int numberOfColumnInBackpack) {
        for (int column = 0; column < 9; ++column) {
            if (applyLock) {
                this.addSlot(new Slot((Container)playerInventory, column, this.getPlayerInventoryLeftPos(numberOfColumnInBackpack) + column * 18, this.getPlayerHotbarTopPos(numberOfLineInBackpack)){

                    public boolean mayPickup(@NotNull Player pPlayer) {
                        if (this.getContainerSlot() == lockedSlotID) {
                            return false;
                        }
                        return super.mayPickup(pPlayer);
                    }

                    public boolean mayPlace(@NotNull ItemStack pStack) {
                        if (this.getContainerSlot() == lockedSlotID) {
                            return false;
                        }
                        return super.mayPlace(pStack);
                    }
                });
                continue;
            }
            this.addSlot(new Slot((Container)playerInventory, column, this.getPlayerInventoryLeftPos(numberOfColumnInBackpack) + column * 18, this.getPlayerHotbarTopPos(numberOfLineInBackpack)));
        }
    }

    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    public boolean stillValid(Player pPlayer) {
        return false;
    }
}

