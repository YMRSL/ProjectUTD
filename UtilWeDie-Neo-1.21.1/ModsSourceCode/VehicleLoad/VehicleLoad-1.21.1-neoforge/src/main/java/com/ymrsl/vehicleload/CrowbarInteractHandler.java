package com.ymrsl.vehicleload;

import com.ymrsl.vehicleload.compat.CreateContraptionCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class CrowbarInteractHandler {
    private static final ResourceLocation CROWBAR_ITEM_ID =
        ResourceLocation.fromNamespaceAndPath("superbwarfare", "crowbar");
    private static final TagKey<Item> CROWBAR_TAG = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("forge", "tools/crowbar")
    );

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        Entity target = event.getTarget();
        if (player == null || target == null) {
            return;
        }
        if (!VehicleCompat.isTargetVehicle(target)) {
            return;
        }
        if (!isSuperbCrowbar(player.getMainHandItem())) {
            return;
        }

        // If vehicle is riding a Create contraption, crowbar cancels riding and adds cooldown.
        Entity mount = target.getVehicle();
        if (mount != null && CreateContraptionCompat.isContraptionEntity(mount)) {
            if (!level.isClientSide) {
                target.stopRiding();
                SeatCooldowns.block(target, level.getGameTime());
            }
            event.setCanceled(true);
            return;
        }

        // If vehicle is locked to a Sable structure seat, crowbar releases it and adds cooldown.
        if (SableSeatLockManager.isLocked(target)) {
            if (!level.isClientSide) {
                SableSeatLockManager.unlock(target);
                SeatCooldowns.block(target, level.getGameTime());
            }
            event.setCanceled(true);
            return;
        }

        // Disable crowbar packing for survival players on Superb vehicles.
        if (player.isShiftKeyDown() && VehicleCompat.isSuperbwarfareVehicle(target) && isSurvivalPlayer(player)) {
            event.setCanceled(true);
        }
    }

    private boolean isSuperbCrowbar(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!ModList.get().isLoaded("superbwarfare")) {
            return false;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (CROWBAR_ITEM_ID.equals(key)) {
            return true;
        }
        return stack.is(CROWBAR_TAG);
    }

    private boolean isSurvivalPlayer(Player player) {
        return player != null && !player.isCreative() && !player.isSpectator();
    }
}
