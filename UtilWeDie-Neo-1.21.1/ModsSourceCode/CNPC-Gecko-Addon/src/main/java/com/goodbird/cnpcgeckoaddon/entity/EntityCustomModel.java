package com.goodbird.cnpcgeckoaddon.entity;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.GeckoLib;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.loading.object.BakedAnimations;
import software.bernie.geckolib.util.GeckoLibUtil;

public class EntityCustomModel extends Animal implements GeoAnimatable, GeoEntity {
    private AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    public ResourceLocation modelResLoc=ResourceLocation.fromNamespaceAndPath(CNPCGeckoAddon.MODID, "geo/geo_npc.geo.json");
    public ResourceLocation animResLoc=ResourceLocation.fromNamespaceAndPath(CNPCGeckoAddon.MODID , "animations/geo_npc.animation.json");
    public ResourceLocation textureResLoc = ResourceLocation.fromNamespaceAndPath("customnpcs","textures/entity/humanmale/steve.png");
    public String idleAnim = "";
    public String walkAnim = "";
    public String hurtAnim = "";
    public String attackAnim = "";
    public String deathAnim = "death";
    public RawAnimation dialogAnim = null;
    public RawAnimation manualAnim = null;
    public ItemStack leftHeldItem;
    public String headBoneName = "head";
    private EntityDimensions dims;
    public int size = 5;
    // Cached death animation builder so isCurrentAnimation() can match by identity across ticks.
    private RawAnimation deathRawAnim = null;
    private String deathRawAnimName = null;

    // Returns true only if the given animation name actually exists in this entity's loaded
    // animation file. Guards against GeckoLib throwing when an addon model has no "death"/etc.
    private boolean animationExists(String name) {
        if (name == null || name.isEmpty()) return false;
        BakedAnimations baked = GeckoLibCache.getBakedAnimations().get(animResLoc);
        if (baked == null) return false;
        return baked.animations().containsKey(name);
    }

    private PlayState predicateMovement(AnimationState<EntityCustomModel> event) {
        AnimationController<EntityCustomModel> controller = event.getController();
        // --- Death state machine (highest priority): once dying, play death once and hold the
        // last frame. Independent of the manual/dialog/movement channels so the corpse stays posed.
        if (deathTime > 0) {
            if (animationExists(deathAnim)) {
                if (deathRawAnim == null || !deathAnim.equals(deathRawAnimName)) {
                    deathRawAnim = RawAnimation.begin().thenPlayAndHold(deathAnim);
                    deathRawAnimName = deathAnim;
                }
                if (!event.isCurrentAnimation(deathRawAnim)) {
                    controller.forceAnimationReset();
                    controller.setAnimation(deathRawAnim);
                }
                return PlayState.CONTINUE;
            }
            // No death animation available: fall through to idle/walk so the corpse at least
            // keeps the render-side death tilt instead of T-posing.
        }
        if (manualAnim != null) {
            if (controller.getCurrentRawAnimation() == manualAnim && controller.getAnimationState() == AnimationController.State.STOPPED) {
                manualAnim = null;
            } else {
                if (controller.getCurrentRawAnimation() != manualAnim) {
                    controller.forceAnimationReset();
                }
                controller.setAnimation(manualAnim);
                return PlayState.CONTINUE;
            }
        }
        if (dialogAnim != null) {
            if (controller.getCurrentRawAnimation() == dialogAnim && controller.getAnimationState() == AnimationController.State.STOPPED) {
                dialogAnim = null;
            } else {
                if (controller.getCurrentRawAnimation() != dialogAnim) {
                    controller.forceAnimationReset();
                }
                controller.setAnimation(dialogAnim);
                return PlayState.CONTINUE;
            }
        }
        // --- Movement channel with animation guard (root-fix for jitter on fast mobs).
        // Only (re)issue setAnimation when the controller is NOT already on the target loop,
        // so a high-speed mob crossing the +/-0.15 limbSwing threshold doesn't restart the
        // animation from frame 0 every tick (which looked like jitter / a false jump).
        if ((event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F) || walkAnim.isEmpty()) {
            if (!idleAnim.isEmpty()) {
                setLoopGuarded(event, idleAnim);
            } else {
                return PlayState.STOP;
            }
        } else {
            setLoopGuarded(event, walkAnim);
        }
        return PlayState.CONTINUE;
    }

    // Cache the last looped builder so identity comparison via isCurrentAnimation works and we
    // only call setAnimation on an actual change of target animation.
    private RawAnimation loopRawAnim = null;
    private String loopRawAnimName = null;

    private void setLoopGuarded(AnimationState<EntityCustomModel> event, String animName) {
        if (loopRawAnim == null || !animName.equals(loopRawAnimName)) {
            loopRawAnim = RawAnimation.begin().thenLoop(animName);
            loopRawAnimName = animName;
        }
        if (!event.isCurrentAnimation(loopRawAnim)) {
            event.getController().setAnimation(loopRawAnim);
        }
    }

//    private <E extends IAnimatable> PlayState predicateAttack(AnimationEvent<E> event) {
//        return PlayState.CONTINUE;
//    }

    public EntityCustomModel(EntityType<? extends Animal> type, Level worldIn) {
        super(type, worldIn);
        this.noCulling = true;
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    public void setSize(float width, float height) {
        dims = EntityDimensions.scalable(width, height);
    }

    @Override
    public EntityDimensions getDimensions(Pose p_213305_1_) {
        if(dims==null){
            dims = EntityDimensions.scalable(0.7F, 2F);
        }
        return dims;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 10, this::predicateMovement));
        //controllers.add(new AnimationController<>(this, "attack", 10, this::predicateAttack));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }

    @Override
    public double getTick(Object entity) {
        return tickCount;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }

    public double getAttributeValue(Holder<Attribute> p_233637_1_) {
        try {
            return this.getAttributes().getValue(p_233637_1_);
        }catch (Exception e){
            return 1.0;
        }
    }
}