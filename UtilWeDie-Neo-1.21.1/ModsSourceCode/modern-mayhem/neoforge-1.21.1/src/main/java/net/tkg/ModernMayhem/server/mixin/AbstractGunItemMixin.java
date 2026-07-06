package net.tkg.ModernMayhem.server.mixin;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import java.util.Optional;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import net.tkg.ModernMayhem.server.util.ItemNBTUtil;
import net.tkg.ModernMayhem.server.util.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={AbstractGunItem.class})
public class AbstractGunItemMixin {
    @Unique
    private static final ThreadLocal<Boolean> EXTRACTING_FROM_RIG = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final ThreadLocal<Player> CURRENT_RELOADING_PLAYER = ThreadLocal.withInitial(() -> null);

    @Inject(method={"canReload"}, at={@At(value="RETURN")}, cancellable=true, remap=false)
    private void checkRigForAmmo(LivingEntity shooter, ItemStack gunItem, CallbackInfoReturnable<Boolean> cir) {
        int maxAmmoCount;
        if (((Boolean)cir.getReturnValue()).booleanValue()) {
            return;
        }
        if (!(shooter instanceof Player)) {
            return;
        }
        Player player = (Player)shooter;
        ResourceLocation gunId = ((AbstractGunItem)(Object)this).getGunId(gunItem);
        Optional optionalIndex = TimelessAPI.getCommonGunIndex((ResourceLocation)gunId);
        if (optionalIndex.isEmpty()) {
            return;
        }
        CommonGunIndex gunIndex = (CommonGunIndex)optionalIndex.get();
        int currentAmmoCount = ((AbstractGunItem)(Object)this).getCurrentAmmoCount(gunItem);
        if (currentAmmoCount >= (maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment((ItemStack)gunItem, (GunData)gunIndex.getGunData()))) {
            cir.setReturnValue(false);
            return;
        }
        ItemStack rigItem = CuriosUtil.getRigItem(player);
        if (rigItem == null || rigItem.isEmpty()) {
            return;
        }
        if (!this.modernMayhem$canRigSupplyAmmo(rigItem)) {
            return;
        }
        int size = this.modernMayhem$getRigInventorySize(rigItem);
        if (size <= 0) {
            return;
        }
        CompoundTag tag = ItemNBTUtil.getTag(rigItem);
        if (tag == null || !tag.contains("inventory")) {
            return;
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
            if (item instanceof IAmmo && (iAmmo = (IAmmo)item).isAmmoOfGun(gunItem, stack)) {
                CURRENT_RELOADING_PLAYER.set(player);
                cir.setReturnValue(true);
                return;
            }
            item = stack.getItem();
            if (!(item instanceof IAmmoBox) || !(iBox = (IAmmoBox)item).isAmmoBoxOfGun(gunItem, stack) || (boxAmmo = iBox.getAmmoCount(stack)) <= 0) continue;
            CURRENT_RELOADING_PLAYER.set(player);
            cir.setReturnValue(true);
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Inject(method={"findAndExtractInventoryAmmo"}, at={@At(value="RETURN")}, cancellable=true, remap=false)
    private void extractAmmoFromRigAfter(IItemHandler itemHandler, ItemStack gunItem, int needAmmoCount, CallbackInfoReturnable<Integer> cir) {
        if (EXTRACTING_FROM_RIG.get().booleanValue()) {
            return;
        }
        int alreadyFound = (Integer)cir.getReturnValue();
        int remainingToFind = needAmmoCount - alreadyFound;
        if (remainingToFind <= 0) {
            CURRENT_RELOADING_PLAYER.remove();
            return;
        }
        Player player = CURRENT_RELOADING_PLAYER.get();
        if (player == null) {
            return;
        }
        ItemStack rigItem = CuriosUtil.getRigItem(player);
        if (rigItem == null || rigItem.isEmpty()) {
            CURRENT_RELOADING_PLAYER.remove();
            return;
        }
        if (!this.modernMayhem$canRigSupplyAmmo(rigItem)) {
            CURRENT_RELOADING_PLAYER.remove();
            return;
        }
        int size = this.modernMayhem$getRigInventorySize(rigItem);
        if (size <= 0) {
            CURRENT_RELOADING_PLAYER.remove();
            return;
        }
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(rigItem);
        if (!tag.contains("inventory")) {
            CURRENT_RELOADING_PLAYER.remove();
            return;
        }
        RegistryAccess provider = player.level().registryAccess();
        ItemStackHandler rigInventory = new ItemStackHandler(size);
        rigInventory.deserializeNBT(provider, tag.getCompound("inventory"));
        EXTRACTING_FROM_RIG.set(true);
        try {
            int foundInRig = this.extractFromRig(rigInventory, gunItem, remainingToFind);
            if (foundInRig > 0) {
                tag.put("inventory", (Tag)rigInventory.serializeNBT(provider));
                ItemNBTUtil.setTag(rigItem, tag);
                cir.setReturnValue(alreadyFound + foundInRig);
            }
        }
        finally {
            EXTRACTING_FROM_RIG.set(false);
        }
    }

    @Unique
    private int extractFromRig(ItemStackHandler inventory, ItemStack gunItem, int needAmmoCount) {
        MutableInt remaining = new MutableInt(needAmmoCount);
        int found = 0;
        for (int i = 0; i < inventory.getSlots() && remaining.value > 0; ++i) {
            int boxAmmo;
            IAmmoBox iBox;
            IAmmo iAmmo;
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (item instanceof IAmmo && (iAmmo = (IAmmo)item).isAmmoOfGun(gunItem, stack)) {
                int toExtract = Math.min(stack.getCount(), remaining.value);
                ItemStack extracted = inventory.extractItem(i, toExtract, false);
                found += extracted.getCount();
                remaining.value -= extracted.getCount();
                continue;
            }
            Item toExtract = stack.getItem();
            if (!(toExtract instanceof IAmmoBox) || !(iBox = (IAmmoBox)toExtract).isAmmoBoxOfGun(gunItem, stack) || (boxAmmo = iBox.getAmmoCount(stack)) <= 0) continue;
            int extractCount = Math.min(boxAmmo, remaining.value);
            iBox.setAmmoCount(stack, boxAmmo - extractCount);
            if (boxAmmo - extractCount <= 0) {
                iBox.setAmmoId(stack, DefaultAssets.EMPTY_AMMO_ID);
            }
            found += extractCount;
            remaining.value -= extractCount;
        }
        return found;
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

