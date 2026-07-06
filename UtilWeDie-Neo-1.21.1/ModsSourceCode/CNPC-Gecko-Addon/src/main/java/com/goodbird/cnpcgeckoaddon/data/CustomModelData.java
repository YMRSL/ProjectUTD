package com.goodbird.cnpcgeckoaddon.data;

import com.goodbird.cnpcgeckoaddon.CNPCGeckoAddon;
import net.minecraft.nbt.CompoundTag;

public class CustomModelData {
    private String model = CNPCGeckoAddon.MODID + ":geo/geo_npc.geo.json";
    private String animFile = CNPCGeckoAddon.MODID + ":animations/geo_npc.animation.json";
    private String idleAnim = "idle";
    private String walkAnim = "walk";
    private String attackAnim = "";
    private String hurtAnim = "";
    private String deathAnim = "death";
    private String headBoneName = "head";
    private int transitionLengthTicks = 10;
    // Attack-telegraph hit frame: how many ticks AFTER the attack animation starts the melee
    // damage actually lands. 0 (or no attackAnim) => legacy behaviour: damage is dealt immediately
    // and the anim is just played as a flourish (used for models with no windup, e.g. lurker).
    // >0 => the addon suppresses CNPC's instant hit, plays the windup, then re-applies the hit at
    // this tick (matches Fungal's original 15-tick / 0.75s humanoid windup).
    private int attackHitTick = 15;
    private float width = 0.7f;
    private float height = 2f;
    // Default ON so gecko NPCs flash red on hit out of the box (matches the death-time red tint,
    // which is always active). Still GUI-toggleable per NPC via "Enable Hurt Tint".
    private boolean hurtTintEnabled = true;
    public CompoundTag writeToNBT(CompoundTag nbttagcompound) {
        nbttagcompound.putString("Model", model);
        nbttagcompound.putString("AnimFile", animFile);
        nbttagcompound.putString("IdleAnim", idleAnim);
        nbttagcompound.putString("WalkAnim", walkAnim);
        nbttagcompound.putString("AttackAnim", attackAnim);
        nbttagcompound.putString("HurtAnim", hurtAnim);
        nbttagcompound.putString("DeathAnim", deathAnim);
        nbttagcompound.putString("HeadBoneName", headBoneName);
        nbttagcompound.putInt("TransitionLengthTicks", transitionLengthTicks);
        nbttagcompound.putInt("AttackHitTick", attackHitTick);
        nbttagcompound.putFloat("Width",width);
        nbttagcompound.putFloat("Height",height);
        nbttagcompound.putBoolean("HurtTintEnabled",hurtTintEnabled);
        return nbttagcompound;
    }

    public void readFromNBT(CompoundTag nbttagcompound) {
        if (nbttagcompound.contains("Model")) {
            model = nbttagcompound.getString("Model");
            animFile = nbttagcompound.getString("AnimFile");
            idleAnim = nbttagcompound.getString("IdleAnim");
            walkAnim = nbttagcompound.getString("WalkAnim");
            hurtAnim = nbttagcompound.getString("HurtAnim");
            attackAnim = nbttagcompound.getString("AttackAnim");
            // Backward compatible: older clones saved before this field existed have no
            // "DeathAnim" key, so keep the default ("death") instead of clobbering it with "".
            if (nbttagcompound.contains("DeathAnim"))
                deathAnim = nbttagcompound.getString("DeathAnim");
            headBoneName = nbttagcompound.getString("HeadBoneName");
            if (nbttagcompound.contains("HeadBoneName"))
                headBoneName = nbttagcompound.getString("HeadBoneName");

            if (nbttagcompound.contains("Width"))
                width = nbttagcompound.getFloat("Width");

            if (nbttagcompound.contains("Height"))
                height = nbttagcompound.getFloat("Height");

            if (nbttagcompound.contains("TransitionLengthTicks"))
                transitionLengthTicks = nbttagcompound.getInt("TransitionLengthTicks");

            // Backward compatible: clones saved before this field existed keep the default (15).
            if (nbttagcompound.contains("AttackHitTick"))
                attackHitTick = nbttagcompound.getInt("AttackHitTick");

            if (nbttagcompound.contains("HurtTintEnabled"))
                hurtTintEnabled = nbttagcompound.getBoolean("HurtTintEnabled");
        }
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getAnimFile() {
        return animFile;
    }

    public void setAnimFile(String animFile) {
        this.animFile = animFile;
    }

    public String getIdleAnim() {
        return idleAnim;
    }

    public void setIdleAnim(String idleAnim) {
        this.idleAnim = idleAnim;
    }

    public String getWalkAnim() {
        return walkAnim;
    }

    public void setWalkAnim(String walkAnim) {
        this.walkAnim = walkAnim;
    }

    public String getAttackAnim() {
        return attackAnim;
    }

    public void setAttackAnim(String attackAnim) {
        this.attackAnim = attackAnim;
    }

    public String getHurtAnim() {
        return hurtAnim;
    }

    public void setHurtAnim(String hurtAnim) {
        this.hurtAnim = hurtAnim;
    }

    public String getDeathAnim() {
        return deathAnim;
    }

    public void setDeathAnim(String deathAnim) {
        this.deathAnim = deathAnim;
    }

    public String getHeadBoneName() {
        return headBoneName;
    }

    public void setHeadBoneName(String headBoneName) {
        this.headBoneName = headBoneName;
    }

    public int getTransitionLengthTicks() {
        return transitionLengthTicks;
    }

    public void setTransitionLengthTicks(int transitionLengthTicks) {
        this.transitionLengthTicks = transitionLengthTicks;
    }

    public int getAttackHitTick() {
        return attackHitTick;
    }

    public void setAttackHitTick(int attackHitTick) {
        this.attackHitTick = attackHitTick;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public boolean isHurtTintEnabled() {
        return hurtTintEnabled;
    }

    public void setHurtTintEnabled(boolean value) {
        this.hurtTintEnabled = value;
    }
}
