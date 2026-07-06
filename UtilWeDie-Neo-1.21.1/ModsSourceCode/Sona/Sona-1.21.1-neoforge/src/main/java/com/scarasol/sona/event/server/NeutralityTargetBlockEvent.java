package com.scarasol.sona.event.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import javax.annotation.Nullable;

/**
 * @author Scarasol
 *
 * <p>NeoForge: {@code net.neoforged.bus.api.Event} 不再自带 isCancelable/isCanceled，
 * 取消能力改为实现 {@link ICancellableEvent}。原 isCancelable()=true 语义保留。</p>
 */
public class NeutralityTargetBlockEvent extends Event implements ICancellableEvent {
    private final Mob mob;
    private final Level level;
    @Nullable
    private final LivingEntity target;

    public NeutralityTargetBlockEvent(Mob mob, Level level, @Nullable LivingEntity target) {
        this.mob = mob;
        this.level = level;
        this.target = target;
    }

    public Mob getMob() {
        return mob;
    }

    public Level getLevel() {
        return level;
    }

    @Nullable
    public LivingEntity getTarget() {
        return target;
    }
}
