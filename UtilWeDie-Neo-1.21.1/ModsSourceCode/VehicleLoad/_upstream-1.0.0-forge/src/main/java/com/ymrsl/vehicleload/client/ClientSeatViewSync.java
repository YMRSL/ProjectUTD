package com.ymrsl.vehicleload.client;

import com.ymrsl.vehicleload.VehicleLoadMod;
import com.ymrsl.vehicleload.compat.CreateContraptionCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VehicleLoadMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientSeatViewSync {
    private static int prevContraptionId = -1;
    private static float prevYaw;
    private static float prevPitch;

    private ClientSeatViewSync() {
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!CreateContraptionCompat.isLoaded()) {
            reset();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isPassenger()) {
            reset();
            return;
        }
        Entity vehicle = player.getVehicle();
        if (vehicle == null || !VehicleCompat.isTargetVehicle(vehicle)) {
            reset();
            return;
        }
        Entity contraption = vehicle.getVehicle();
        if (!CreateContraptionCompat.isContraptionEntity(contraption)) {
            reset();
            return;
        }

        float partialTicks = event.renderTickTime;
        Float yaw = CreateContraptionCompat.getContraptionYaw(contraption, partialTicks);
        if (yaw == null) {
            reset();
            return;
        }
        Float pitch = CreateContraptionCompat.getContraptionPitch(contraption, partialTicks);
        if (pitch == null) {
            pitch = 0.0f;
        }

        int id = contraption.getId();
        if (prevContraptionId != id) {
            prevContraptionId = id;
            prevYaw = yaw;
            prevPitch = pitch;
            return;
        }

        float yawDiff = shortestAngleDiff(prevYaw, yaw);
        float pitchDiff = shortestAngleDiff(pitch, prevPitch);
        prevYaw = yaw;
        prevPitch = pitch;

        float yawRelativeToTrain = Mth.abs(shortestAngleDiff(player.getYRot(), -yaw - 90.0f));
        if (yawRelativeToTrain > 120.0f) {
            pitchDiff *= -1.0f;
        } else if (yawRelativeToTrain > 60.0f) {
            pitchDiff = 0.0f;
        }

        if (yawDiff == 0.0f && pitchDiff == 0.0f) {
            return;
        }
        player.setYRot(player.getYRot() + yawDiff);
        player.setXRot(player.getXRot() + pitchDiff);
    }

    private static void reset() {
        prevContraptionId = -1;
    }

    private static float shortestAngleDiff(float angle, float target) {
        return Mth.wrapDegrees(angle - target);
    }
}
