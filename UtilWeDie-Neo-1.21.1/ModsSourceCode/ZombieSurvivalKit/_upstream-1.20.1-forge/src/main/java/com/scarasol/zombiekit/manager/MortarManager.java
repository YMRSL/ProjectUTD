package com.scarasol.zombiekit.manager;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.api.MortarLevel;
import com.scarasol.zombiekit.data.LaunchSchedule;
import com.scarasol.zombiekit.entity.ai.goal.MortarUsingGoal;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.network.MapVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class MortarManager {
    private final Set<MortarUsingGoal<? extends MortarEntity>> artillerymen = new LinkedHashSet<>();
    private final Set<LaunchSchedule> onGoingSchedules = new LinkedHashSet<>();
    private final Level level;

    public MortarManager(Level level) {
        this.level = level;
    }

    public void subscribe(MortarUsingGoal<? extends MortarEntity> artilleryman, long gameTime) {
        checkGoal();
        this.artillerymen.add(artilleryman);
        checkSchedules(gameTime);
        syncSchedule(artilleryman);
//        ZombieKitMod.LOGGER.info("CurrentSize: " + artillerymen.size());
//        ZombieKitMod.LOGGER.info("SubscribePos: " + artilleryman.getMob().getOnPos());
    }

    public void unsubscribe(MortarUsingGoal<? extends MortarEntity> artilleryman, long gameTime) {
        checkGoal();
        this.artillerymen.remove(artilleryman);
        checkSchedules(gameTime);
//        ZombieKitMod.LOGGER.info("CurrentSize: " + artillerymen.size());
//        ZombieKitMod.LOGGER.info("UnsubscribePos: " + artilleryman.getMob().getOnPos());
    }

    public void checkGoal() {
        new ArrayList<>(artillerymen).stream().filter(e -> e.getMob().level().getEntity(e.getMob().getId()) == null).forEach(this::abortGoal);
    }

    public void syncSchedule(MortarUsingGoal<? extends Mob> artilleryman) {
        artilleryman.syncSchedule(new ArrayList<>(this.onGoingSchedules));
    }

    public void checkSchedules(long gameTime){
        new ArrayList<>(onGoingSchedules).stream().filter(launchSchedule -> launchSchedule.isTimeout(gameTime)).forEach(this::abortSchedule);
    }

    public void postSchedule(LaunchSchedule launchSchedule) {
        checkSchedules(launchSchedule.getCreateTime());
        if (this.onGoingSchedules.add(launchSchedule)) {
//            ZombieKitMod.LOGGER.info("PostPos: " + launchSchedule.getCoordinate());
            addForceLoadRange(launchSchedule);
            broadcastSchedule(launchSchedule);
        }
    }

    public void broadcastSchedule(LaunchSchedule launchSchedule) {
        artillerymen.forEach(goal -> goal.pushSchedule(launchSchedule));
    }

    public void abortSchedule(LaunchSchedule launchSchedule) {
//        ZombieKitMod.LOGGER.info("Abort!");
        this.onGoingSchedules.remove(launchSchedule);
        endForceLoadRange(launchSchedule);
    }

    public void abortGoal(MortarUsingGoal<? extends Mob> artilleryman) {
        this.artillerymen.remove(artilleryman);
    }

    public void addForceLoadRange(LaunchSchedule launchSchedule) {
        BlockPos coordinate = launchSchedule.getCoordinate();
//        ZombieKitMod.LOGGER.info("Add!");
        if (level instanceof MortarLevel mortarLevel) {
            for (Map.Entry<UUID, ChunkPos> entry : MapVariables.get(level).mortarPos.entrySet()) {
                ChunkPos chunkPos = entry.getValue();
                if (chunkPos.getMiddleBlockPosition(coordinate.getY()).distSqr(coordinate) < 57600) {
                    if (launchSchedule.getMortarPos().add(chunkPos)) {
                        mortarLevel.getServerChunkCache().chunkMap.getDistanceManager().addRegionTicket(TicketType.FORCED, chunkPos, 3, chunkPos);
                    }
                }

            }

        }

    }

    public void endForceLoadRange(LaunchSchedule launchSchedule) {

        if (level instanceof MortarLevel mortarLevel) {
            for (ChunkPos chunkPos : launchSchedule.getMortarPos()) {
                mortarLevel.getServerChunkCache().chunkMap.getDistanceManager().removeRegionTicket(TicketType.FORCED, chunkPos, 15, chunkPos);
            }

        }
//        ZombieKitMod.LOGGER.info("Remove!");

    }

}
