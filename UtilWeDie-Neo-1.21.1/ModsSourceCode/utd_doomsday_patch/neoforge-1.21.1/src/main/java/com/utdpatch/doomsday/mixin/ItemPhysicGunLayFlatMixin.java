package com.utdpatch.doomsday.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lays TaCZ guns flat on the ground under ItemPhysic instead of standing them on end.
 *
 * <p>ItemPhysic applies {@code Axis.XP.rotation(PI/2)} to every dropped item to "lay it flat" --
 * correct for flat 2D item sprites (vertical in the GUI), but TaCZ guns are 3D models whose GROUND
 * display is already horizontal, so that extra 90&deg; tips them upright (muzzle pointing up).
 *
 * <p>For the gun item ({@code tacz:modern_kinetic_gun}, shared by all TaCZ/daffas guns) we neutralise
 * <b>all</b> of ItemPhysic's rotations (the {@code XP} lay-flat, the {@code ZP} yaw spin and the
 * {@code YP} physics tilt). Skipping only the lay-flat left the yaw/tilt rotations operating without
 * their intended laid-flat frame, so every gun settled at a different random angle. With all three
 * neutralised the gun keeps purely its own horizontal GROUND display orientation while still resting
 * on the ground via ItemPhysic's translation -- consistent and flat for every gun. Other items are
 * untouched.
 */
@Mixin(targets = "team.creative.itemphysic.client.ItemPhysicClient", remap = false)
public abstract class ItemPhysicGunLayFlatMixin {

    @Redirect(
        method = "render",
        at = @At(value = "INVOKE",
                 target = "Lcom/mojang/math/Axis;rotation(F)Lorg/joml/Quaternionf;"),
        require = 0
    )
    private static Quaternionf utd$gunKeepFlat(Axis axis, float angle, ItemEntity entity) {
        if (utd$isTaczGun(entity)) {
            // ItemPhysic applies its three rotations in the order XP -> ZP -> YP, i.e. XP ends up
            // outermost. The gun's GROUND model stands upright, so:
            //  - ZP slot  -> fixed 90 degrees, tips the upright gun flat (same for every gun).
            //  - XP slot  -> reused as a stable per-entity random spin about the world-up axis,
            //                applied after the lay-flat, so dropped guns scatter to random facings.
            //  - YP slot  -> identity (drop ItemPhysic's physics tilt).
            if (axis == Axis.ZP) {
                return axis.rotation((float) (Math.PI / 2.0));
            }
            if (axis == Axis.XP) {
                long h = (entity.getId() * 2654435761L) >>> 11 & 0x1FFFFFL;
                float facing = (float) (h / (double) 0x200000 * (Math.PI * 2.0));
                return Axis.YP.rotation(facing);
            }
            return new Quaternionf();
        }
        return axis.rotation(angle);
    }

    @Inject(
        method = "render",
        at = @At(value = "INVOKE",
                 target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V",
                 ordinal = 0,
                 shift = At.Shift.BEFORE),
        require = 0
    )
    private static void utd$liftGun(ItemEntity entity, float entityYaw, float partialTicks, PoseStack pose,
            MultiBufferSource buffer, int packedLight, ItemRenderer itemRenderer, RandomSource rand,
            CallbackInfoReturnable<Boolean> cir) {
        if (utd$isTaczGun(entity)) {
            // Raise the gun ~1 grass-block pixel (1/16) before ItemPhysic's rotations -- i.e. in world
            // space -- so it rests on top of the ground instead of half-sunk into it. Being outermost,
            // it is a pure vertical lift regardless of the gun's random facing.
            pose.translate(0.0, 1.0 / 16.0, 0.0);
        }
    }

    private static boolean utd$isTaczGun(ItemEntity entity) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(entity.getItem().getItem());
        return id != null && id.toString().equals("tacz:modern_kinetic_gun");
    }
}
