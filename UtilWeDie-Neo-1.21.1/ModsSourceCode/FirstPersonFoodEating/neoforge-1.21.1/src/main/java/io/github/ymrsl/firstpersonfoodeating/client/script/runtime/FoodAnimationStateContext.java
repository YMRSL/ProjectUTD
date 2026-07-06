package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import io.github.ymrsl.firstpersonfoodeating.client.script.statemachine.AnimationStateContext;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public class FoodAnimationStateContext extends AnimationStateContext {
    private float putAwayTime = 0.0f;
    private float partialTicks = 0.0f;
    private boolean usingItem = false;
    private float useProgress = 0.0f;
    private boolean sprinting = false;
    private boolean moving = false;
    private boolean onGround = false;
    private float walkDist = 0.0f;
    private float walkDistAnchor = 0.0f;
    private ItemStack currentItem = ItemStack.EMPTY;
    private String useClipName = "use";

    public float getPutAwayTime() {
        return putAwayTime;
    }

    public void setPutAwayTime(float putAwayTime) {
        this.putAwayTime = putAwayTime;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public void setPartialTicks(float partialTicks) {
        this.partialTicks = partialTicks;
    }

    public boolean isUsingItem() {
        return usingItem;
    }

    public float getUseProgress() {
        return useProgress;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isMoving() {
        return moving;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    public void anchorWalkDist() {
        walkDistAnchor = walkDist;
    }

    public float getWalkDist() {
        return walkDist - walkDistAnchor;
    }

    public boolean isAiming() {
        return false;
    }

    public float getAimingProgress() {
        return 0.0f;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public String getUseClipName() {
        return useClipName;
    }

    public String getUseClip() {
        return useClipName;
    }

    public void setUseClipName(String useClipName) {
        if (useClipName == null || useClipName.isBlank()) {
            this.useClipName = "use";
            return;
        }
        this.useClipName = useClipName;
    }

    public void updateTickData(
            ItemStack item,
            boolean usingItem,
            float useProgress,
            boolean sprinting,
            boolean moving,
            boolean onGround,
            float walkDist
    ) {
        this.currentItem = item;
        this.usingItem = usingItem;
        this.useProgress = useProgress;
        this.sprinting = sprinting;
        this.moving = moving;
        this.onGround = onGround;
        this.walkDist = walkDist;
    }
}
