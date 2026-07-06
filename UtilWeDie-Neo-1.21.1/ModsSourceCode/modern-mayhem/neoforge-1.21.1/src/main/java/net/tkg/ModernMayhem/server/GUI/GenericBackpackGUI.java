package net.tkg.ModernMayhem.server.GUI;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem;
import net.tkg.ModernMayhem.server.registry.GUIRegistryMM;
import net.tkg.ModernMayhem.server.util.AbstractContainerMenuUtil;
import net.tkg.ModernMayhem.server.util.ItemNBTUtil;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public class GenericBackpackGUI
extends AbstractContainerMenuUtil
implements Supplier<Map<Integer, Slot>> {
    private int backpackSize;
    private int numberOfLine;
    private int slotPerLine;
    private ItemStackHandler itemHandler;
    private final Map<Integer, Slot> slots;
    private int backpackSlotID;
    private boolean isCuriosBackpack;
    private Inventory playerInventory;
    private ICuriosItemHandler playerCuriosInventory;
    private String curiosSlotType;

    public GenericBackpackGUI(int pContainerId, Inventory pPlayerInventory, RegistryFriendlyByteBuf data) {
        this(pContainerId, pPlayerInventory, null, data);
        new SimpleContainerData(0);
    }

    public GenericBackpackGUI(int pContainerId, Inventory pPlayerInventory, RegistryFriendlyByteBuf data, ICuriosItemHandler pPlayerCuriosInventory) {
        this(pContainerId, pPlayerInventory, pPlayerCuriosInventory, data);
    }

    public GenericBackpackGUI(int pContainerId, Inventory pPlayerInventory, ICuriosItemHandler pPlayerCuriosInventory, RegistryFriendlyByteBuf pExtraData) {
        super((MenuType)GUIRegistryMM.BACKPACK_GUI.get(), pContainerId);
        this.slots = new HashMap<Integer, Slot>();
        this.backpackSlotID = -1;
        this.isCuriosBackpack = false;
        this.curiosSlotType = "";
        this.numberOfLine = 1;
        this.slotPerLine = 1;
        this.itemHandler = new ItemStackHandler(this.backpackSize){

            protected void onContentsChanged(int slot) {
                GenericBackpackGUI.this.updateBackpack();
                super.onContentsChanged(slot);
            }
        };
        this.playerInventory = pPlayerInventory;
        this.playerCuriosInventory = pPlayerCuriosInventory;
        if (pExtraData != null) {
            this.numberOfLine = pExtraData.readByte();
            this.slotPerLine = pExtraData.readByte();
            CompoundTag tag = pExtraData.readNbt();
            if (tag != null) {
                this.itemHandler.deserializeNBT(pExtraData.registryAccess(), tag);
            }
            this.isCuriosBackpack = pExtraData.readBoolean();
            if (this.isCuriosBackpack) {
                this.backpackSlotID = pExtraData.readByte();
                switch (pExtraData.readByte()) {
                    case 0 -> this.curiosSlotType = "back";
                    case 1 -> this.curiosSlotType = "body";
                    default -> this.curiosSlotType = "";
                }
                if (pPlayerCuriosInventory == null) {
                    this.playerCuriosInventory = (ICuriosItemHandler)CuriosApi.getCuriosInventory((LivingEntity)this.playerInventory.player).get();
                }
            } else {
                ItemStack itemInHand = ItemStack.OPTIONAL_STREAM_CODEC.decode(pExtraData);
                if (!itemInHand.isEmpty()) {
                    for (int i = 0; i < pPlayerInventory.getContainerSize(); ++i) {
                        if (!ItemStack.isSameItemSameComponents((ItemStack)pPlayerInventory.getItem(i), (ItemStack)itemInHand)) continue;
                        this.backpackSlotID = i;
                        break;
                    }
                }
            }
        }
        this.backpackSize = this.numberOfLine * this.slotPerLine;
        int slotID = 0;
        for (int row = 0; row < this.numberOfLine; ++row) {
            for (int collumn = 0; collumn < this.slotPerLine; ++collumn) {
                this.slots.put(slotID, this.addSlot((Slot)new SlotItemHandler((IItemHandler)this.itemHandler, slotID, this.getBackpackInventoryLeftPos(this.slotPerLine) + 1 + collumn * 18, 8 + row * 18){

                    public void initialize(ItemStack stack) {
                    }

                    public boolean mayPlace(@NotNull ItemStack stack) {
                        if (stack.getItem() instanceof GenericBackpackItem) {
                            return false;
                        }
                        return super.mayPlace(stack);
                    }
                }));
                ++slotID;
            }
        }
        this.createPlayerInventory(pPlayerInventory, this.backpackSlotID, !this.isCuriosBackpack, this.numberOfLine, this.slotPerLine);
        this.createPlayerHotbar(pPlayerInventory, this.backpackSlotID, !this.isCuriosBackpack, this.numberOfLine, this.slotPerLine);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        Slot originSlot = this.getSlot(pIndex);
        ItemStack originStack = originSlot.getItem();
        if (!originSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack copyOriginStack = originStack.copy();
        int playerInventoryStart = this.backpackSize;
        int playerInventoryEnd = this.backpackSize + 27;
        int hotbarStart = this.backpackSize + 27;
        int hotbarEnd = this.backpackSize + 36;
        if (pIndex < this.backpackSize) {
            if (!this.moveItemStackTo(originStack, hotbarStart, hotbarEnd, false) && !this.moveItemStackTo(originStack, playerInventoryStart, playerInventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        } else if (pIndex >= playerInventoryStart && pIndex < hotbarEnd) {
            if (!this.moveItemStackTo(originStack, 0, this.backpackSize, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.err.println("Invalid slot index: " + pIndex);
            return ItemStack.EMPTY;
        }
        if (originStack.isEmpty()) {
            originSlot.set(ItemStack.EMPTY);
        } else {
            originSlot.setChanged();
        }
        if (originStack.getCount() == copyOriginStack.getCount()) {
            return ItemStack.EMPTY;
        }
        originSlot.onTake(pPlayer, originStack);
        this.updateBackpack();
        return copyOriginStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return true;
    }

    @Override
    public Map<Integer, Slot> get() {
        return this.slots;
    }

    private void updateBackpack() {
        RegistryAccess provider = this.playerInventory.player.level().registryAccess();
        if (this.isCuriosBackpack) {
            Optional<ICurioStacksHandler> temp = this.playerCuriosInventory.getStacksHandler(this.curiosSlotType);
            if (temp.isEmpty()) {
                System.err.println("Curios inventory not found : " + this.curiosSlotType);
                return;
            }
            temp.ifPresent(iCurioStacksHandler -> {
                ItemStack backpack = iCurioStacksHandler.getStacks().getStackInSlot(this.backpackSlotID);
                CompoundTag tag = ItemNBTUtil.getOrCreateTag(backpack);
                tag.put("inventory", (Tag)this.itemHandler.serializeNBT(provider));
                ItemNBTUtil.setTag(backpack, tag);
                iCurioStacksHandler.getStacks().setStackInSlot(this.backpackSlotID, backpack);
            });
        } else if (this.backpackSlotID > -1) {
            ItemStack backpack = this.playerInventory.getItem(this.backpackSlotID);
            CompoundTag tag = ItemNBTUtil.getOrCreateTag(backpack);
            tag.put("inventory", (Tag)this.itemHandler.serializeNBT(provider));
            ItemNBTUtil.setTag(backpack, tag);
            this.playerInventory.setItem(this.backpackSlotID, backpack);
        }
    }

    @Override
    public int getSlotPerLine() {
        return this.slotPerLine;
    }

    @Override
    public int getNumberOfLine() {
        return this.numberOfLine;
    }
}
