package net.tkg.ModernMayhem.server.mixin.client;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import net.tkg.ModernMayhem.server.util.ItemNBTUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={GunAnimationStateContext.class}, remap=false)
public class GunAnimationStateContextMixin {
    @Shadow
    private ItemStack currentGunItem;
    @Shadow
    private IGun iGun;

    @Inject(method={"hasAmmoToConsume"}, at={@At(value="RETURN")}, cancellable=true)
    private void checkRigForAmmoOnClient(CallbackInfoReturnable<Boolean> cir) {
        if (((Boolean)cir.getReturnValue()).booleanValue()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (this.iGun != null && this.iGun.useDummyAmmo(this.currentGunItem)) {
            return;
        }
        boolean hasRigAmmo = this.modernMayhem$checkRigHasAmmo((Player)player);
        if (hasRigAmmo) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean modernMayhem$checkRigHasAmmo(Player player) {
        ItemStack rigItem = CuriosUtil.getRigItem(player);
        if (rigItem == null || rigItem.isEmpty()) {
            return false;
        }
        if (!this.modernMayhem$canRigSupplyAmmo(rigItem)) {
            return false;
        }
        int size = this.modernMayhem$getRigInventorySize(rigItem);
        if (size <= 0) {
            return false;
        }
        CompoundTag tag = ItemNBTUtil.getTag(rigItem);
        if (tag == null || !tag.contains("inventory")) {
            return false;
        }
        RegistryAccess provider = player.level().registryAccess();
        ItemStackHandler inventory = new ItemStackHandler(size);
        inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        for (int i = 0; i < inventory.getSlots(); ++i) {
            int boxAmmo;
            IAmmoBox iBox;
            IAmmo iAmmo;
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item instanceof IAmmo && (iAmmo = (IAmmo)item).isAmmoOfGun(this.currentGunItem, stack)) {
                return true;
            }
            item = stack.getItem();
            if (!(item instanceof IAmmoBox) || !(iBox = (IAmmoBox)item).isAmmoBoxOfGun(this.currentGunItem, stack) || (boxAmmo = iBox.getAmmoCount(stack)) <= 0) continue;
            return true;
        }
        return false;
    }

    @Unique
    private boolean modernMayhem$canRigSupplyAmmo(ItemStack rigItem) {
        Item item = rigItem.getItem();
        if (item instanceof GenericBackpackItem) {
            GenericBackpackItem backpackItem = (GenericBackpackItem)item;
            return backpackItem.canSupplyAmmo();
        }
        return false;
    }

    @Unique
    private int modernMayhem$getRigInventorySize(ItemStack rigItem) {
        Item item = rigItem.getItem();
        if (item instanceof GenericBackpackItem) {
            GenericBackpackItem backpackItem = (GenericBackpackItem)item;
            return backpackItem.getInventorySize();
        }
        return -1;
    }
}

