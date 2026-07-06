package net.mcreator.doomsdaydecoration.functionality;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * 27-slot lootable container menu, native NeoForge 1.21.1 port of
 * Raiiiden/DoomsdayFunctionality's {@code DoomsdayContainerMenu}.
 *
 * <p>Loot slots are read-only ({@code mayPlace == false}) so the player can only
 * take items out, never deposit. The client-side constructor reconstructs the menu
 * from the {@link FriendlyByteBuf} that {@code IMenuTypeExtension.create} delivers
 * (it carries the BlockPos written by {@link DoomsdayBlockEntity} via the standard
 * open-menu handshake).</p>
 */
public class DoomsdayContainerMenu extends AbstractContainerMenu {

    private static final int CONTAINER_SLOTS = 27;
    private final DoomsdayBlockEntity blockEntity;

    public DoomsdayContainerMenu(int id, Inventory playerInventory, DoomsdayBlockEntity be) {
        super(ModFunctionality.DOOMSDAY_MENU.get(), id);
        this.blockEntity = be;

        IItemHandler handler = be != null ? be.getLootHandler() : new ItemStackHandler(CONTAINER_SLOTS);

        int startX = 8;
        int backpackStartY = 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                this.addSlot(new SlotItemHandler(handler, index, startX + col * 18, backpackStartY + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        int inventoryStartY = backpackStartY + 66;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, inventoryStartY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, inventoryStartY + 58));
        }
    }

    /** Client-side factory used by {@code IMenuTypeExtension.create}. */
    public DoomsdayContainerMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf));
    }

    private static DoomsdayBlockEntity resolve(Inventory inv, FriendlyByteBuf buf) {
        BlockEntity be = inv.player.level().getBlockEntity(buf.readBlockPos());
        return be instanceof DoomsdayBlockEntity dbe ? dbe : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();

        if (index < CONTAINER_SLOTS) {
            // Shift-click out of the loot grid into the player inventory only.
            if (!this.moveItemStackTo(current, CONTAINER_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory slots: do not allow shift-clicking back into loot grid.
            return ItemStack.EMPTY;
        }

        if (current.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }
}
