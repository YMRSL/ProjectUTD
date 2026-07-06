package io.github.ymrsl.firstpersonfoodeating.client;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class PlayerArmAnimationEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean ENABLE_THIRD_PERSON_ARM_ANIMATION = false;
    private static final Map<Integer, ArmBackup> BACKUPS = new HashMap<>();
    private static int logBudget = 8;
    private static int skipLogBudget = 20;
    private static boolean disabledLogged = false;

    private PlayerArmAnimationEvents() {
    }

    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!ENABLE_THIRD_PERSON_ARM_ANIMATION) {
            if (!disabledLogged) {
                disabledLogged = true;
                LOGGER.info("[firstpersonfoodeating] Third-person arm bone animation disabled (static hold fallback)");
            }
            return;
        }
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        if (!(event.getRenderer() instanceof PlayerRenderer renderer)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || player != minecraft.player) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        Map<String, FoodAnimationController.BonePose> bones = FirstPersonHandEvents.getScriptedBonePose(stack);
        if (bones.isEmpty()) {
            if (skipLogBudget > 0) {
                skipLogBudget--;
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                LOGGER.info("[firstpersonfoodeating] Third-person arm pose skipped (no bones): item={}, stackCount={}, scripted={}",
                        itemId, stack.getCount(), FirstPersonHandEvents.getScriptedDebugSummary());
            }
            return;
        }

        PlayerModel<?> model = renderer.getModel();
        BACKUPS.put(player.getId(), ArmBackup.capture(model));
        model.attackTime = 0.0f;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        applyBoneToArm(model.rightArm, resolveArmBone(bones, true), true);
        applyBoneToArm(model.leftArm, resolveArmBone(bones, false), false);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        if (logBudget > 0) {
            logBudget--;
            LOGGER.info("[firstpersonfoodeating] Third-person arm pose applied, bones={}", bones.keySet());
        }
    }

    private static FoodAnimationController.BonePose resolveArmBone(
            Map<String, FoodAnimationController.BonePose> bones,
            boolean rightArm
    ) {
        FoodAnimationController.BonePose direct = bones.get(rightArm ? "righthand" : "lefthand");
        if (direct != null) {
            return direct;
        }
        return bones.get(rightArm ? "righthand_pos" : "lefthand_pos");
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!ENABLE_THIRD_PERSON_ARM_ANIMATION) {
            return;
        }
        ArmBackup backup = BACKUPS.remove(event.getEntity().getId());
        if (backup == null) {
            return;
        }
        if (!(event.getRenderer() instanceof PlayerRenderer renderer)) {
            return;
        }
        PlayerModel<?> model = renderer.getModel();
        backup.restore(model);
    }

    private static void applyBoneToArm(ModelPart arm, FoodAnimationController.BonePose pose, boolean rightArm) {
        if (pose == null) {
            return;
        }
        float x = pose.rotationDeg().x();
        float y = pose.rotationDeg().y();
        float z = pose.rotationDeg().z();

        arm.xRot = (float) Math.toRadians(-x);
        arm.yRot = (float) Math.toRadians(-y);
        arm.zRot = (float) Math.toRadians(z);

        float positionWeight = 0.15f;
        float px = pose.position().x() / 16.0f;
        float py = pose.position().y() / 16.0f;
        float pz = pose.position().z() / 16.0f;
        arm.x += (rightArm ? -px : px) * positionWeight;
        arm.y += -py * positionWeight;
        arm.z += pz * positionWeight;
    }

    private record ArmState(float x, float y, float z, float xRot, float yRot, float zRot) {
        private static ArmState capture(ModelPart part) {
            return new ArmState(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot);
        }

        private void apply(ModelPart part) {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
        }
    }

    private record ArmBackup(
            ArmState rightArm,
            ArmState leftArm,
            ArmState rightSleeve,
            ArmState leftSleeve,
            float attackTime,
            HumanoidModel.ArmPose rightArmPose,
            HumanoidModel.ArmPose leftArmPose
    ) {
        private static ArmBackup capture(PlayerModel<?> model) {
            return new ArmBackup(
                    ArmState.capture(model.rightArm),
                    ArmState.capture(model.leftArm),
                    ArmState.capture(model.rightSleeve),
                    ArmState.capture(model.leftSleeve),
                    model.attackTime,
                    model.rightArmPose,
                    model.leftArmPose
            );
        }

        private void restore(PlayerModel<?> model) {
            rightArm.apply(model.rightArm);
            leftArm.apply(model.leftArm);
            rightSleeve.apply(model.rightSleeve);
            leftSleeve.apply(model.leftSleeve);
            model.attackTime = attackTime;
            model.rightArmPose = rightArmPose;
            model.leftArmPose = leftArmPose;
        }
    }
}
