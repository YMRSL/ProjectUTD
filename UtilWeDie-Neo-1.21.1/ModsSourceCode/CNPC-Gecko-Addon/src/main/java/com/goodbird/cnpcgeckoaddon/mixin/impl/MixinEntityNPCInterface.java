package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * Gecko-NPC server-side behaviour hooks. Targets {@link EntityNPCInterface} because the methods we
 * need ({@code doHurtTarget}, {@code tick}, {@code tickDeath}) are declared here, not on EntityCustomNpc.
 *
 * IMPORTANT (root-cause of "attack animation never played"): the melee method
 * {@code doHurtTarget(Entity)} is declared on {@link EntityNPCInterface}, NOT on EntityCustomNpc
 * (verified with javap). Mixin {@code @Inject} only binds to methods that physically exist in the
 * targeted class, so the previous {@code @Mixin(EntityCustomNpc.class) @Inject(method = "doHurtTarget")}
 * silently failed to find its target. This mixin targets the class that actually declares the method.
 */
@Mixin(EntityNPCInterface.class)
public class MixinEntityNPCInterface {

    // How long (ticks) a gecko NPC corpse stays before CNPC's normal tickDeath removal/respawn runs.
    private static final int cnpcGeckoAddon$CORPSE_TICKS = 100; // ~5s at 20 tps

    // === Attack-telegraph state (per-NPC, server-side only) ====================================
    // Ticks left until the deferred melee hit lands. -1 = no attack in flight.
    @Unique
    private int cnpcGeckoAddon$pendingHitDelay = -1;
    // Target captured when the windup began; the deferred hit is applied against it.
    @Unique
    private Entity cnpcGeckoAddon$pendingTarget = null;
    // Re-entrancy guard: set while WE call doHurtTarget for the real (deferred) hit so the HEAD
    // inject lets it pass straight through to CNPC's damage logic instead of re-telegraphing.
    @Unique
    private boolean cnpcGeckoAddon$dealingRealHit = false;

    // --- Issue #1 + attack telegraph: intercept the melee hit at HEAD so we can run a windup.
    //
    // CNPC's EntityAIAttackTarget.tick() (verified via javap) does, on a single tick when in range
    // and its private attackTick cooldown has elapsed:
    //     attackTick = melee.getDelay();   // cooldown reset FIRST -> won't re-fire during our windup
    //     npc.swing(MAIN_HAND);
    //     npc.doHurtTarget(target);        // <-- instant damage, we intercept here
    // Because the cooldown is reset before doHurtTarget is even called, deferring the actual hit by
    // a few ticks does NOT make CNPC fire again mid-windup, so there is no fighting over attack pacing
    // as long as AttackHitTick < AttackSpeed (the configured melee delay). We still guard against a
    // second attack starting while one telegraph is already in flight.
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true, remap = false)
    public void cnpcGeckoAddon$onDoHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;
        if (self.level().isClientSide) return;
        // Let our own deferred hit run through untouched (real damage + MeleeAttackEvent + knockback).
        if (cnpcGeckoAddon$dealingRealHit) return;
        if (!(self.display instanceof IDataDisplay)) return;
        IDataDisplay display = (IDataDisplay) self.display;
        // hasCustomModel() guarantees this is a gecko NPC with a live EntityCustomModel.
        if (!display.hasCustomModel()) return;
        String attackAnim = display.getCustomModelData().getAttackAnim();
        if (attackAnim == null || attackAnim.isEmpty()) {
            // No gecko attack animation (e.g. a lurker-style model): keep CNPC's instant hit as-is.
            return;
        }
        int hitTick = display.getCustomModelData().getAttackHitTick();
        if (hitTick <= 0) {
            // Telegraph disabled for this NPC: play the anim as a flourish and let the hit land now
            // (legacy behaviour). Damage stays on this same tick.
            NetworkWrapper.sendAll(new PacketSyncAnimation(self.getId(), RawAnimation.begin().thenPlay(attackAnim)));
            return;
        }
        // A telegraph is already mid-flight: swallow this extra trigger so we neither double-play the
        // windup nor stack two pending hits. (Shouldn't normally happen while hitTick < AttackSpeed.)
        if (cnpcGeckoAddon$pendingHitDelay >= 0) {
            cir.setReturnValue(false);
            return;
        }
        // Begin the windup: broadcast the one-shot attack anim, remember the target, start the
        // countdown, and suppress CNPC's instant damage (return false = "no hit dealt this tick").
        NetworkWrapper.sendAll(new PacketSyncAnimation(self.getId(), RawAnimation.begin().thenPlay(attackAnim)));
        cnpcGeckoAddon$pendingHitDelay = hitTick;
        cnpcGeckoAddon$pendingTarget = target;
        cir.setReturnValue(false);
    }

    // --- Drive the telegraph countdown each server tick and apply the deferred hit at the hit frame.
    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    public void cnpcGeckoAddon$onTick(CallbackInfo ci) {
        if (cnpcGeckoAddon$pendingHitDelay < 0) return;
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;
        if (self.level().isClientSide) return;
        // Abort the windup if we died or lost the target during it.
        if (!self.isAlive()) {
            cnpcGeckoAddon$resetTelegraph();
            return;
        }
        if (cnpcGeckoAddon$pendingHitDelay > 0) {
            cnpcGeckoAddon$pendingHitDelay--;
            return;
        }
        // Hit frame reached.
        Entity target = cnpcGeckoAddon$pendingTarget;
        cnpcGeckoAddon$resetTelegraph();
        if (target == null || !target.isAlive()) return;
        // Only connect if the target is STILL inside the melee reach at the hit frame, so a player who
        // dodged out of range during the windup avoids the blow (the whole point of the telegraph).
        //
        // We replicate CNPC's OWN in-range test from EntityAIAttackTarget.tick() (verified via javap on
        // CustomNPCs-Unofficial-NeoForge-1.21.1) BYTE-FOR-BYTE, so "would CNPC have let this hit start"
        // and "does this hit land" use identical geometry — no drift, no double-counting of widths:
        //   y      = target.boundingBox.minY                 (foot level, not centre)
        //   distSq = npc.distanceToSqr(target.x, y, target.z)
        //   reach1 = range*range + target.bbWidth
        //   reach2 = (npc.bbWidth*2)*(npc.bbWidth*2) + target.bbWidth   (fat-NPC floor)
        //   inRange = distSq <= max(reach1, reach2)
        // getRange() returns DataMelee.attackRange (the per-NPC "AttackRange", ~2 for the clones) — NOT
        // follow/aggro range. With range=2 the threshold is sqrt(4+0.6)=~2.14 blocks horizontally, which
        // is exactly the "dash 2-3 blocks out and you're safe" feel that was wanted.
        //
        // The PREVIOUS code computed (range + 2.0 + width)^2 against a 3D centre-to-centre distance:
        // with range=2 that swelled the threshold to ~4.6 blocks, so a player who had already fled still
        // ate the hit. That extra "+2.0D" slack (on top of squaring an already-summed reach) was the bug.
        if (target instanceof LivingEntity) {
            double targetY = target.getBoundingBox() != null
                    ? target.getBoundingBox().minY
                    : target.getY();
            double distSq = self.distanceToSqr(target.getX(), targetY, target.getZ());
            int meleeRange = self.stats.melee.getRange();
            float npcWidth = self.getBbWidth();
            float targetWidth = target.getBbWidth();
            // reach1 = range*range + targetWidth   (bytecode offsets 146-171)
            double reach = (double) ((float) (meleeRange * meleeRange) + targetWidth);
            // reach2 = (npcWidth*2)*npcWidth*2 + targetWidth   (bytecode offsets 175-201): a floor so a
            // very wide NPC can still reach; identical operation order to CNPC's AI.
            double npcReach = (double) (npcWidth * 2.0F * npcWidth * 2.0F + targetWidth);
            if (npcReach > reach) reach = npcReach;
            // Out of melee reach at the hit frame -> the dodge worked: cancel this strike entirely
            // (the windup animation has already played client-side; we simply skip doHurtTarget).
            if (distSq > reach) return;
        }
        // Apply the real hit now. The re-entrancy flag makes the HEAD inject above pass this through
        // to CNPC's full damage/event/knockback logic instead of starting another windup.
        cnpcGeckoAddon$dealingRealHit = true;
        try {
            self.doHurtTarget(target);
        } finally {
            cnpcGeckoAddon$dealingRealHit = false;
        }
    }

    @Unique
    private void cnpcGeckoAddon$resetTelegraph() {
        cnpcGeckoAddon$pendingHitDelay = -1;
        cnpcGeckoAddon$pendingTarget = null;
    }

    // --- Issue #4: keep gecko NPC corpses for ~5s so the death animation can play out.
    // CNPC's tickDeath() removes the entity almost immediately after death. For gecko NPCs only, we
    // take over the early ticks: keep incrementing deathTime ourselves and cancel the original until
    // the corpse timer elapses, then let the original tickDeath run for removal/respawn handling.
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true, remap = false)
    public void cnpcGeckoAddon$onTickDeath(CallbackInfo ci) {
        EntityNPCInterface self = (EntityNPCInterface) (Object) this;
        if (self.level().isClientSide) return;            // client increments/holds its own deathTime
        if (!(self.display instanceof IDataDisplay)) return;
        if (!((IDataDisplay) self.display).hasCustomModel()) return; // gecko NPCs only
        if (self.deathTime < cnpcGeckoAddon$CORPSE_TICKS) {
            self.deathTime++;     // advance the corpse timer without letting the original remove us yet
            ci.cancel();
        }
        // deathTime >= CORPSE_TICKS: fall through to the real tickDeath (removal / respawn).
    }
}
