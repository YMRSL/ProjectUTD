package com.scarasol.zombiekit.api;

import net.minecraft.client.CameraType;
import net.minecraft.world.entity.LivingEntity;

public interface FixedVehicle {
    boolean validXRot(LivingEntity livingEntity, float xRot);
    boolean validYRot(LivingEntity livingEntity, float yRot);
    CameraType getVehicleCameraType();
}
