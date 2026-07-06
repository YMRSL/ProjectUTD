package com.scarasol.zombiekit.inventory;

import com.scarasol.zombiekit.init.ZombieKitMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class ShortwaveRadioMenu extends AbstractContainerMenu {

    public final static HashMap<String, Object> guistate = new HashMap<>();
    public final Level level;
    public final Player player;
    public int x, y, z;


    public ShortwaveRadioMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(id, inv, extraData.readBlockPos());
    }

    public ShortwaveRadioMenu(int id, Inventory inv, BlockPos pos) {
        super(ZombieKitMenus.SHORTWAVE_RADIO_GUI.get(), id);
        this.player = inv.player;
        this.level = inv.player.level();
        if (pos != null) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player p_38874_) {
        return true;
    }
}
