package com.ymrsl.vehicleload;

import com.ymrsl.vehicleload.compat.CreateContraptionCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.ModList;

public class CrowbarInteractHandler {
    private static final ResourceLocation CROWBAR_ITEM_ID = new ResourceLocation("superbwarfare", "crowbar");
    private static final ResourceLocation CROWBAR_TAG_ID = new ResourceLocation("forge", "tools/crowbar");

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
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        // Disable crowbar packing for survival players on Superb vehicles.
        if (player.isShiftKeyDown() && VehicleCompat.isSuperbwarfareVehicle(target) && isSurvivalPlayer(player)) {
            event.setCancellationResult(InteractionResult.PASS);
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
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (CROWBAR_ITEM_ID.equals(key)) {
            return true;
        }
        return stack.is(ItemTags.create(CROWBAR_TAG_ID));
    }

    private boolean isSurvivalPlayer(Player player) {
        return player != null && !player.isCreative() && !player.isSpectator();
    }
}
