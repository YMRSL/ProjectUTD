package com.projectutd.loot;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Small diagnostics surface used by the in-game acceptance checklist. */
public final class UtdLootCommands {
    private final LootCatalog catalog;

    public UtdLootCommands(LootCatalog catalog) {
        this.catalog = catalog;
    }

    @SubscribeEvent
    public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("utdloot")
                .then(Commands.literal("status")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                            () -> Component.literal(
                                "UTD Loot Core: registry=" + catalog.registryCount()
                                    + " (enabled=" + catalog.enabledCount() + ")"
                                    + ", containers=" + catalog.containerCount()
                                    + ", templates=" + catalog.templateCount()
                            ),
                            false
                        );
                        return 1;
                    }))
                .then(Commands.literal("pity")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        context.getSource().sendSuccess(
                            () -> Component.literal("UTD pity: " + PityState.summary(player)),
                            false
                        );
                        return 1;
                    })
                    .then(Commands.literal("reset")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PityState.reset(player);
                            context.getSource().sendSuccess(() -> Component.literal("UTD pity counters reset"), false);
                            return 1;
                        })))
                .then(Commands.literal("set")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("channel", StringArgumentType.word())
                        .then(Commands.argument("tier4Miss", IntegerArgumentType.integer(0))
                            .then(Commands.argument("tier5Miss", IntegerArgumentType.integer(0))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String channel = StringArgumentType.getString(context, "channel");
                                    String tier4Key = PityState.tier4Key(channel);
                                    if (tier4Key == null) {
                                        context.getSource().sendFailure(Component.literal(
                                            "channel must be civilian, medical, or military"
                                        ));
                                        return 0;
                                    }
                                    PityState.set(
                                        player,
                                        tier4Key,
                                        IntegerArgumentType.getInteger(context, "tier4Miss")
                                    );
                                    String tier5Key = PityState.tier5Key(channel);
                                    if (tier5Key != null) {
                                        PityState.set(
                                            player,
                                            tier5Key,
                                            IntegerArgumentType.getInteger(context, "tier5Miss")
                                        );
                                    }
                                    context.getSource().sendSuccess(
                                        () -> Component.literal("UTD pity: " + PityState.summary(player)),
                                        false
                                    );
                                    return 1;
                                })))))
        );
    }
}
