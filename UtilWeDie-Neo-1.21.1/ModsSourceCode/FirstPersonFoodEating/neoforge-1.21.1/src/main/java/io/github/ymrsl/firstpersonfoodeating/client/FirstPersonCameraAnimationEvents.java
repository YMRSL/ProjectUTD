package io.github.ymrsl.firstpersonfoodeating.client;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class FirstPersonCameraAnimationEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final float CAMERA_ROT_MULTIPLIER = 1.65f;
    private static int cameraLogBudget = 12;

    private FirstPersonCameraAnimationEvents() {
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        if (!minecraft.options.bobView().get()) {
            return;
        }
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        Map<String, FoodAnimationController.BonePose> bones = FirstPersonHandEvents.getScriptedBonePose(stack);
        if (bones.isEmpty()) {
            return;
        }

        FoodAnimationController.BonePose cameraPose = bones.get("camera");
        if (cameraPose == null) {
            cameraPose = bones.get("ep_camera");
        }
        if (cameraPose == null) {
            return;
        }

        Quaternionf quaternion = toCameraQuaternion(cameraPose, CAMERA_ROT_MULTIPLIER);
        if (quaternion.equals(0.0f, 0.0f, 0.0f, 1.0f)) {
            return;
        }

        Vector3f rot = toYawPitchRollDeg(quaternion);
        event.setYaw(event.getYaw() + rot.y());
        event.setPitch(event.getPitch() + rot.x());
        event.setRoll(event.getRoll() + rot.z());

        if (cameraLogBudget > 0) {
            cameraLogBudget--;
            Vector3f src = cameraPose.rotationDeg();
            LOGGER.info("[firstpersonfoodeating] Camera animation applied: bone=({},{},{}), add=({}, {}, {})",
                    src.x(), src.y(), src.z(), rot.x(), rot.y(), rot.z());
        }
    }

    private static Quaternionf toCameraQuaternion(FoodAnimationController.BonePose pose, float multiplier) {
        Vector3f source = pose.rotationDeg();
        float x = (float) Math.toRadians(source.x() * multiplier);
        float y = (float) Math.toRadians(source.y() * multiplier);
        // Keep TACZ-compatible sign for Z channel.
        float z = (float) Math.toRadians(-source.z() * multiplier);
        return new Quaternionf().rotationXYZ(x, y, z);
    }

    private static Vector3f toYawPitchRollDeg(Quaternionf q) {
        double yaw = Math.asin(2.0 * (q.w() * q.y() - q.x() * q.z()));
        double pitch = Math.atan2(2.0 * (q.w() * q.x() + q.y() * q.z()), 1.0 - 2.0 * (q.x() * q.x() + q.y() * q.y()));
        double roll = Math.atan2(2.0 * (q.w() * q.z() + q.x() * q.y()), 1.0 - 2.0 * (q.y() * q.y() + q.z() * q.z()));
        return new Vector3f((float) Math.toDegrees(pitch), (float) Math.toDegrees(yaw), (float) Math.toDegrees(roll));
    }
}
