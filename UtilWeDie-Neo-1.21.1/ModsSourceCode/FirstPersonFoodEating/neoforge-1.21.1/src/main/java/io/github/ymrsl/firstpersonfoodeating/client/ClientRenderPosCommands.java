package io.github.ymrsl.firstpersonfoodeating.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo.FoodFirstPersonGeoRenderer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class ClientRenderPosCommands {
    private ClientRenderPosCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("firstpersonfood")
                        .then(Commands.literal("renderpos")
                                .then(directionNode("up", 0, 1))
                                .then(directionNode("down", 0, -1))
                                .then(directionNode("left", -1, 0))
                                .then(directionNode("right", 1, 0))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("x", IntegerArgumentType.integer(-2000, 2000))
                                                .then(Commands.argument("y", IntegerArgumentType.integer(-2000, 2000))
                                                        .executes(context -> {
                                                            int x = IntegerArgumentType.getInteger(context, "x");
                                                            int y = IntegerArgumentType.getInteger(context, "y");
                                                            FoodFirstPersonGeoRenderer.setFirstPersonOffsetPixels(x, y);
                                                            sendOffsetFeedback(context.getSource(),
                                                                    "Set render offset");
                                                            return Command.SINGLE_SUCCESS;
                                                        }))))
                                .then(Commands.literal("show")
                                        .executes(context -> {
                                            sendOffsetFeedback(context.getSource(),
                                                    "Current render offset");
                                            return Command.SINGLE_SUCCESS;
                                        }))
                                .then(Commands.literal("undo")
                                        .executes(context -> {
                                            if (FoodFirstPersonGeoRenderer.undoFirstPersonOffsetPixels()) {
                                                sendOffsetFeedback(context.getSource(),
                                                        "Undo render offset");
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            context.getSource().sendFailure(Component.literal("No undo history for render offset"));
                                            return 0;
                                        }))
                                .then(Commands.literal("redo")
                                        .executes(context -> {
                                            if (FoodFirstPersonGeoRenderer.redoFirstPersonOffsetPixels()) {
                                                sendOffsetFeedback(context.getSource(),
                                                        "Redo render offset");
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            context.getSource().sendFailure(Component.literal("No redo history for render offset"));
                                            return 0;
                                        }))
                                .then(Commands.literal("reset")
                                        .executes(context -> {
                                            FoodFirstPersonGeoRenderer.resetFirstPersonOffsetPixels();
                                            sendOffsetFeedback(context.getSource(),
                                                    "Reset render offset");
                                            return Command.SINGLE_SUCCESS;
                                        })))
        );
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> directionNode(
            String directionName, int xSign, int ySign
    ) {
        return Commands.literal(directionName)
                .then(Commands.argument("pixels", IntegerArgumentType.integer(0, 2000))
                        .executes(context -> {
                            int pixels = IntegerArgumentType.getInteger(context, "pixels");
                            FoodFirstPersonGeoRenderer.adjustFirstPersonOffsetPixels(
                                    xSign * pixels,
                                    ySign * pixels
                            );
                            sendOffsetFeedback(context.getSource(),
                                    "Adjusted render offset");
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    private static void sendOffsetFeedback(CommandSourceStack source, String title) {
        float x = FoodFirstPersonGeoRenderer.getFirstPersonOffsetXPixels();
        float y = FoodFirstPersonGeoRenderer.getFirstPersonOffsetYPixels();
        source.sendSuccess(() -> Component.literal(title + ": x=" + Math.round(x) + "px, y=" + Math.round(y) + "px"), false);
    }
}
