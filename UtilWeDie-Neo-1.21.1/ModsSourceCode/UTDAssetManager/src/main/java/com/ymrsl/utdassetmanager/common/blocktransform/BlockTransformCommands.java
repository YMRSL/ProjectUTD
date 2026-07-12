package com.ymrsl.utdassetmanager.common.blocktransform;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ymrsl.utdassetmanager.UTDAssetManagerMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Server-side diagnostics for the block-transform rule source and runtime compiler. */
@EventBusSubscriber(modid = UTDAssetManagerMod.MOD_ID)
public final class BlockTransformCommands {
    private BlockTransformCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("utdasset")
                        .then(
                                Commands.literal("transforms")
                                        .then(Commands.literal("status")
                                                .executes(context -> status(context.getSource())))
                                        .then(Commands.literal("reload")
                                                .requires(BlockTransformCommands::canManage)
                                                .executes(context -> reload(context.getSource())))
                                        .then(Commands.literal("validate")
                                                .requires(BlockTransformCommands::canManage)
                                                .executes(context -> validate(context.getSource())))
                                        .then(Commands.literal("promote")
                                                .requires(BlockTransformCommands::canManage)
                                                .then(Commands.argument("sha256", StringArgumentType.word())
                                                        .executes(context -> promote(
                                                                context.getSource(),
                                                                StringArgumentType.getString(
                                                                        context, "sha256"))))))
        );
    }

    private static int status(CommandSourceStack source) {
        BlockTransformDiagnostics.Status status = BlockTransformDiagnostics.status();
        return send(source, "UTD transforms status", BlockTransformPaths.display(false), "", status.generation(),
                status.total(), status.enabled(), status.usable(), status.error());
    }

    private static int reload(CommandSourceStack source) {
        BlockTransformDiagnostics.Status status = BlockTransformDiagnostics.reload();
        return send(source, "UTD transforms reload (forced active-file reload)",
                BlockTransformPaths.display(false), "", status.generation(),
                status.total(), status.enabled(), status.usable(), status.error());
    }

    private static int validate(CommandSourceStack source) {
        BlockTransformDiagnostics.Validation validation = BlockTransformDiagnostics.validate();
        return send(source, "UTD transforms validate candidate", BlockTransformPaths.display(true),
                validation.sha256(), null,
                validation.total(), validation.enabled(), validation.usable(), validation.error());
    }

    private static int promote(CommandSourceStack source, String expectedSha256) {
        BlockTransformDiagnostics.Promotion promotion = BlockTransformDiagnostics.promote(expectedSha256);
        StringBuilder message = new StringBuilder("UTD transforms promote: candidate=")
                .append(BlockTransformPaths.display(true))
                .append(", active=").append(BlockTransformPaths.display(false))
                .append(", sha256=").append(promotion.sha256().isBlank() ? "<unavailable>" : promotion.sha256())
                .append(", generation=").append(promotion.generation())
                .append(", total=").append(promotion.total())
                .append(", enabled=").append(promotion.enabled())
                .append(", promoted=").append(promotion.promoted())
                .append(", usable=").append(promotion.usable())
                .append(", error=").append(promotion.error().isBlank() ? "<none>" : singleLine(promotion.error()));
        Component output = Component.literal(message.toString());
        if (!promotion.usable()) {
            source.sendFailure(output);
            return 0;
        }
        source.sendSuccess(() -> output, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int send(
            CommandSourceStack source,
            String operation,
            String displayPath,
            String sha256,
            Long generation,
            int total,
            int enabled,
            boolean usable,
            String error) {
        StringBuilder message = new StringBuilder(operation)
                .append(": path=").append(displayPath);
        if (!sha256.isBlank()) message.append(", sha256=").append(sha256);
        if (generation != null) message.append(", generation=").append(generation);
        message.append(", total=").append(total)
                .append(", enabled=").append(enabled)
                .append(", usable=").append(usable)
                .append(", error=").append(error.isBlank() ? "<none>" : singleLine(error));
        Component output = Component.literal(message.toString());
        if (!usable) {
            source.sendFailure(output);
            return 0;
        }
        source.sendSuccess(() -> output, false);
        return Command.SINGLE_SUCCESS;
    }

    private static String singleLine(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    private static boolean canManage(CommandSourceStack source) {
        boolean singleplayerOwner = source.getEntity() instanceof ServerPlayer player
                && source.getServer().isSingleplayerOwner(player.getGameProfile());
        return BlockTransformCommandAccess.managementAllowed(source.hasPermission(2), singleplayerOwner);
    }
}
