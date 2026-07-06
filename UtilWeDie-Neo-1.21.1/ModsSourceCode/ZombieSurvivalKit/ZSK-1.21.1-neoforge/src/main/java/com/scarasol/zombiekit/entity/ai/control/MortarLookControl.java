package com.scarasol.zombiekit.entity.ai.control;

import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import net.minecraft.world.entity.ai.control.LookControl;

public class MortarLookControl extends LookControl {

    private float wantAngle;
    private float wantAzimuth;

    public MortarLookControl(MortarEntity mob) {
        super(mob);
    }

    protected boolean resetXRotOnTick() {
        return false;
    }

    public void setLookView(float wantAzimuth, float wantAngle) {
        this.wantAzimuth = wantAzimuth;
        this.wantAngle = wantAngle;
        this.yMaxRotSpeed = (float)this.mob.getHeadRotSpeed();
        this.xMaxRotAngle = (float)this.mob.getMaxHeadXRot();
        this.lookAtCooldown = 1;
    }

    @Override
    public void tick() {
        if (this.resetXRotOnTick()) {
            this.mob.setXRot(0.0F);
        }

        MortarEntity mortar = ((MortarEntity)this.mob);
        if (this.lookAtCooldown > 0) {
            float azimuth = mortar.getAzimuth();
            float angle = mortar.getAngle();
            if (this.lookAtCooldown == 2) {
                this.getYRotD().ifPresent((p_287447_) -> {
                    mortar.setAzimuth(this.rotateTowards(mortar.getAzimuth(), p_287447_, this.yMaxRotSpeed));
                });
                this.getXRotD().ifPresent((p_289400_) -> {
                    mortar.setAngle(this.rotateTowards(mortar.getAngle(), p_289400_, this.xMaxRotAngle));
                });
            }else {
                mortar.setAzimuth(this.rotateTowards(mortar.getAzimuth(), this.wantAzimuth, this.yMaxRotSpeed));
                mortar.setAngle(this.rotateTowards(mortar.getAngle(), this.wantAngle, this.xMaxRotAngle));
            }
            if (azimuth == mortar.getAzimuth() && angle == mortar.getAngle()) {
                lookAtCooldown = 0;
            }
        }
        this.clampHeadRotationToBody();
    }
}
