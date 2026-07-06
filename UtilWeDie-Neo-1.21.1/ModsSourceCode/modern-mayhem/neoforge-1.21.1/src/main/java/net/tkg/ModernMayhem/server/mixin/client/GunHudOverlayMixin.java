package net.tkg.ModernMayhem.server.mixin.client;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem;
import net.tkg.ModernMayhem.server.mixin.client.GunHudOverlayAccessor;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import net.tkg.ModernMayhem.server.util.ItemNBTUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(value=Dist.CLIENT)
@Mixin(value={GunHudOverlay.class})
public class GunHudOverlayMixin {
    @Inject(method={"handleInventoryAmmo"}, at={@At(value="RETURN")}, remap=false)
    private static void addRigAmmo(ItemStack gunStack, Inventory inventory, CallbackInfo ci) {
        Player player = inventory.player;
        ItemStack rigItem = CuriosUtil.getRigItem(player);
        if (rigItem == null || rigItem.isEmpty()) {
            return;
        }
        if (!GunHudOverlayMixin.modernMayhem$canRigSupplyAmmo(rigItem)) {
            return;
        }
        int size = GunHudOverlayMixin.modernMayhem$getInventorySizeFromRig(rigItem);
        if (size <= 0) {
            return;
        }
        CompoundTag tag = ItemNBTUtil.getTag(rigItem);
        if (tag == null || !tag.contains("inventory")) {
            return;
        }
        RegistryAccess provider = player.level().registryAccess();
        ItemStackHandler inventoryHandler = new ItemStackHandler(size);
        inventoryHandler.deserializeNBT(provider, tag.getCompound("inventory"));
        for (int i = 0; i < inventoryHandler.getSlots(); ++i) {
            IAmmoBox iBox;
            IAmmo iAmmo;
            ItemStack slotStack = inventoryHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) continue;
            Item item = slotStack.getItem();
            if (item instanceof IAmmo && (iAmmo = (IAmmo)item).isAmmoOfGun(gunStack, slotStack)) {
                GunHudOverlayAccessor.setCacheInventoryAmmoCount(GunHudOverlayAccessor.getCacheInventoryAmmoCount() + slotStack.getCount());
            }
            if (!(item instanceof IAmmoBox) || !(iBox = (IAmmoBox)item).isAmmoBoxOfGun(gunStack, slotStack)) continue;
            if (iBox.isAllTypeCreative(slotStack) || iBox.isCreative(slotStack)) {
                GunHudOverlayAccessor.setCacheInventoryAmmoCount(9999);
                return;
            }
            GunHudOverlayAccessor.setCacheInventoryAmmoCount(GunHudOverlayAccessor.getCacheInventoryAmmoCount() + iBox.getAmmoCount(slotStack));
        }
    }

    @Unique
    private static boolean modernMayhem$canRigSupplyAmmo(ItemStack rigItem) {
        Item item = rigItem.getItem();
        if (item instanceof GenericBackpackItem) {
            GenericBackpackItem backpackItem = (GenericBackpackItem)item;
            return backpackItem.canSupplyAmmo();
        }
        return false;
    }

    @Unique
    private static int modernMayhem$getInventorySizeFromRig(ItemStack rigItem) {
        Item item = rigItem.getItem();
        if (item instanceof GenericBackpackItem) {
            GenericBackpackItem backpackItem = (GenericBackpackItem)item;
            return backpackItem.getInventorySize();
        }
        return -1;
    }
}

