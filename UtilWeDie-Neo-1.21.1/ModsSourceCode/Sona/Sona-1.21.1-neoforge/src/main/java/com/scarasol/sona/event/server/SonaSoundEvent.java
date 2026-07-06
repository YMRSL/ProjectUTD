package com.scarasol.sona.event.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * @author Scarasol
 *
 * <p>NeoForge: 取消能力改为实现 {@link ICancellableEvent}（原 isCancelable()=true）。</p>
 */
public class SonaSoundEvent extends Event implements ICancellableEvent {
    private final ServerLevel serverLevel;
    private final BlockPos blockPos;
    private final int amplifier;
    private final State state;

    public SonaSoundEvent(ServerLevel serverLevel, BlockPos blockPos, int amplifier, State state) {
        this.serverLevel = serverLevel;
        this.blockPos = blockPos;
        this.amplifier = amplifier;
        this.state = state;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public State getState() {
        return state;
    }


    public enum State {
        /**
         * LivingAddEffect
         */
        LIVING,

        /**
         * SpawnDecoy
         */
        DECOY
    }
}
