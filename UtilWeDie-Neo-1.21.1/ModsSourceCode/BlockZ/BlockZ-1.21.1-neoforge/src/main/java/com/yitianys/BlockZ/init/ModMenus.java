package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BlockZ.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<DayZInventoryMenu>> DAYZ_INVENTORY =
            MENUS.register("dayz_inventory", () -> IMenuTypeExtension.create(DayZInventoryMenu::fromNetwork));
}
