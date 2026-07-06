package com.ymrsl.vehicleload;

import com.ymrsl.vehicleload.compat.CreateContraptionCompat;
import com.ymrsl.vehicleload.compat.VehicleCompat;
import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class VehicleLoadCommands {
    private static final int SCAN_RADIUS = 128;
    private final AutoSeatHandler handler;

    public VehicleLoadCommands(AutoSeatHandler handler) {
        this.handler = handler;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("vehiclelogoutput")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("dump")
                    .executes(ctx -> dump(ctx.getSource())))
                .then(Commands.literal("scan")
                    .executes(ctx -> scan(ctx.getSource())))
        );
    }

    private int dump(CommandSourceStack source) {
        if (handler != null) {
            handler.dumpDebug(source);
        } else {
            source.sendFailure(Component.literal("vehicleload: AutoSeatHandler missing."));
        }
        return 1;
    }

    private int scan(CommandSourceStack source) {
        if (!(source.getLevel() instanceof ServerLevel)) {
            source.sendFailure(Component.literal("vehicleload: server level not available."));
            return 0;
        }
        ServerLevel level = (ServerLevel) source.getLevel();
        if (!CreateContraptionCompat.isLoaded()) {
            source.sendFailure(Component.literal("vehicleload: Create not loaded or compat init failed."));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("vehicleload: player only."));
            return 0;
        }
        Vec3 center = player.position();
        AABB scanBox = new AABB(center, center).inflate(SCAN_RADIUS);
        List<Entity> contraptions = level.getEntitiesOfClass(
            Entity.class,
            scanBox,
            CreateContraptionCompat::isContraptionEntity
        );
        int vehicleCount = level.getEntitiesOfClass(
            Entity.class,
            scanBox,
            VehicleCompat::isTargetVehicle
        ).size();

        VehicleLoadMod.LOGGER.info(
            "vehicleload scan: player={} pos={} contraptions={} vehicles={} radius={}",
            player.getGameProfile().getName(),
            formatVec(center),
            contraptions.size(),
            vehicleCount,
            SCAN_RADIUS
        );
        for (Entity contraption : contraptions) {
            List<BlockPos> seats = CreateContraptionCompat.getSeatPositions(contraption);
            Map<UUID, Integer> mapping = CreateContraptionCompat.getSeatMapping(contraption);
            VehicleLoadMod.LOGGER.info(
                "vehicleload scan contraption: id={} type={} seats={} mapping={}",
                contraption.getId(),
                contraption.getType().toString(),
                seats.size(),
                mapping.size()
            );
        }
        source.sendSuccess(
            () -> Component.literal("vehicleload scan complete. Check latest.log for details."),
            false
        );
        return 1;
    }

    private String formatVec(Vec3 vec) {
        return String.format("%.2f, %.2f, %.2f", vec.x, vec.y, vec.z);
    }
}
