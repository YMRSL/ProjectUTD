package net.tkg.ModernMayhem.server.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.tkg.ModernMayhem.server.registry.ItemRegistryMM;

public class CreativeTabsRegistryMM {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create((ResourceKey)Registries.CREATIVE_MODE_TAB, (String)"mm");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MM = TABS.register("mm", () -> CreativeModeTab.builder().title((Component)Component.translatable((String)"item_group.mm")).icon(() -> new ItemStack((ItemLike)ItemRegistryMM.MENU_ITEM.get())).displayItems((pParameters, pOutput) -> {
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_GPNVG.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_GPNVG.get());
        pOutput.accept((ItemLike)ItemRegistryMM.ULTRA_GAMER_GPNVG.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_PVS14.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_PVS14.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_PVS14.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_PVS7.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_VISOR.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_VISOR.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_TVG.get());
        pOutput.accept((ItemLike)ItemRegistryMM.COTI.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_BACKPACK_T1.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_BACKPACK_T2.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_BACKPACK_T3.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_BACKPACK_T1.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_BACKPACK_T2.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_BACKPACK_T3.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_BACKPACK_T1.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_BACKPACK_T2.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_BACKPACK_T3.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_PLATE_CARRIER.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_PLATE_CARRIER_AMMO.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_PLATE_CARRIER_POUCHES.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_PLATE_CARRIER.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_PLATE_CARRIER_AMMO.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_PLATE_CARRIER_POUCHES.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_BANDOLEER.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_RECON.get());
        pOutput.accept((ItemLike)ItemRegistryMM.HEXAGON_RIG.get());
        pOutput.accept((ItemLike)ItemRegistryMM.C1300.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_COMBAT_HELMET.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_KEVLAR_CHESTPLATE.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_KEVLAR_LEGGINGS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_KEVLAR_BOOTS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_IOLA.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_KNEE_PADS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_SSH68_HELMET.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_KEVLAR_CHESTPLATE.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_KEVLAR_LEGGINGS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_KEVLAR_BOOTS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_IOLA.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_KNEE_PADS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_COMBAT_HELMET.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_KEVLAR_CHESTPLATE.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_KEVLAR_LEGGINGS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_KEVLAR_BOOTS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_IOLA.get());
        pOutput.accept((ItemLike)ItemRegistryMM.TAN_KNEE_PADS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_HEAD_MOUNT.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_RONIN_HELMET.get());
        pOutput.accept((ItemLike)ItemRegistryMM.YELLOW_HAZMAT_HELMET.get());
        pOutput.accept((ItemLike)ItemRegistryMM.YELLOW_HAZMAT_CHESTPLATE.get());
        pOutput.accept((ItemLike)ItemRegistryMM.YELLOW_HAZMAT_LEGGINGS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.ORANGE_HAZMAT_HELMET.get());
        pOutput.accept((ItemLike)ItemRegistryMM.ORANGE_HAZMAT_CHESTPLATE.get());
        pOutput.accept((ItemLike)ItemRegistryMM.ORANGE_HAZMAT_LEGGINGS.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BALACLAVA.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_GLASSES.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_GOGGLES.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_HEADSET.get());
        pOutput.accept((ItemLike)ItemRegistryMM.BLACK_MILITARY_BALACLAVA.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GP5_GAS_MASK.get());
        pOutput.accept((ItemLike)ItemRegistryMM.DUFFEL_BAG.get());
        pOutput.accept((ItemLike)ItemRegistryMM.WHITE_PHOSPHOR.get());
        pOutput.accept((ItemLike)ItemRegistryMM.GREEN_PHOSPHOR.get());
        pOutput.accept((ItemLike)ItemRegistryMM.RED_PHOSPHOR.get());
        pOutput.accept((ItemLike)BlockRegistryMM.IR_LIGHT_BLOCK.get());
    }).build());

    public static void init(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}

