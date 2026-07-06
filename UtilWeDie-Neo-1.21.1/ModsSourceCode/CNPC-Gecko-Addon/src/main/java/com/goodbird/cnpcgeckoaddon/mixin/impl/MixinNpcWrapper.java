package com.goodbird.cnpcgeckoaddon.mixin.impl;

import com.goodbird.cnpcgeckoaddon.data.CustomModelData;
import com.goodbird.cnpcgeckoaddon.mixin.IDataDisplay;
import com.goodbird.cnpcgeckoaddon.network.NetworkWrapper;
import com.goodbird.cnpcgeckoaddon.network.PacketSyncAnimation;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.wrapper.EntityLivingWrapper;
import noppes.npcs.api.wrapper.NPCWrapper;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import software.bernie.geckolib.animation.RawAnimation;

@Mixin(NPCWrapper.class)
public class MixinNpcWrapper<T extends EntityNPCInterface> extends EntityLivingWrapper<T> {
    public MixinNpcWrapper() {
        super(null);
    }

    @Unique
    public CustomModelData getModelData(){
        return ((IDataDisplay)getMCEntity().display).getCustomModelData();
    }

    @Unique
    public void setGeckoModel(String model) {
        getModelData().setModel(model);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoTexture(String texture) {
        getMCEntity().display.setSkinTexture(texture);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoAnimationFile(String animation) {
        getModelData().setAnimFile(animation);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoIdleAnimation(String animation) {
        getModelData().setIdleAnim(animation);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoWalkAnimation(String animation) {
        getModelData().setWalkAnim(animation);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoAttackAnimation(String animation) {
        getModelData().setAttackAnim(animation);
        getMCEntity().updateClient();
    }

    @Unique
    public void setGeckoDeathAnimation(String animation) {
        getModelData().setDeathAnim(animation);
        getMCEntity().updateClient();
    }

    // Attack-telegraph hit frame: ticks after the attack anim starts that the melee hit lands.
    // 0 disables the windup (instant damage; anim is just a flourish). Example (npcs script):
    //   npc.setGeckoAttackHitTick(15);   // 0.75s windup, matching Fungal's humanoid zombies
    @Unique
    public void setGeckoAttackHitTick(int tick) {
        getModelData().setAttackHitTick(tick);
        getMCEntity().updateClient();
    }

    @Unique
    public int getGeckoAttackHitTick() {
        return getModelData().getAttackHitTick();
    }

    @Unique
    public void syncAnimationsFor(IPlayer player, RawAnimation builder) {
        NetworkWrapper.send(player.getMCEntity(), new PacketSyncAnimation(entity.getId(),builder));
    }
    @Unique
    public void syncAnimationsForAll(RawAnimation builder) {
        NetworkWrapper.sendAll(new PacketSyncAnimation(entity.getId(),builder));
    }

    // --- Trigger-style one-shot animation API for scripts.
    // Plays the named animation once on every client's copy of this NPC's gecko model, then the
    // movement controller automatically resumes idle/walk (this rides the existing manualAnim
    // channel, which self-clears when the one-shot finishes). Example (npcs script):
    //   npc.playGeckoAnim("attack");
    @Unique
    public void playGeckoAnim(String animName) {
        if (animName == null || animName.isEmpty()) return;
        NetworkWrapper.sendAll(new PacketSyncAnimation(entity.getId(), RawAnimation.begin().thenPlay(animName)));
    }

    // Same as playGeckoAnim but only for one player (e.g. dialog-local effects).
    @Unique
    public void playGeckoAnimFor(IPlayer player, String animName) {
        if (animName == null || animName.isEmpty()) return;
        NetworkWrapper.send(player.getMCEntity(), new PacketSyncAnimation(entity.getId(), RawAnimation.begin().thenPlay(animName)));
    }
}
