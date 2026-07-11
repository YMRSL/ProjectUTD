package com.ymrsl.utdassetmanager.client;

import com.mojang.brigadier.Command;
import com.ymrsl.utdassetmanager.UTDAssetManagerMod;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = UTDAssetManagerMod.MOD_ID, value = Dist.CLIENT)
public final class ClientAssetCommands {
    private ClientAssetCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("utdasset")
                        .executes(context -> open())
                        .then(Commands.literal("open").executes(context -> open()))
                        .then(Commands.literal("markhand").executes(context -> markHand(context.getSource())))
                        .then(Commands.literal("unmarkhand").executes(context -> unmarkHand(context.getSource())))
                        .then(Commands.literal("export").executes(context -> export(context.getSource())))
                        .then(Commands.literal("reload").executes(context -> {
                            AssetRepository.get().forceReloadManifest();
                            context.getSource().sendSuccess(() -> Component.literal("UTD status_manifest.json 已重载"), false);
                            return Command.SINGLE_SUCCESS;
                        }))
        );
    }

    private static int open() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new AssetManagerScreen(minecraft.screen));
        return Command.SINGLE_SUCCESS;
    }

    private static int markHand(net.minecraft.commands.CommandSourceStack source) {
        ItemStack stack = heldStack();
        AssetRecord record = AssetStackCodec.capture(stack);
        if (record == null) {
            source.sendFailure(Component.literal("主手没有可标注物品"));
            return 0;
        }
        try {
            AssetRepository.get().select(record);
            source.sendSuccess(() -> Component.literal("已标注 " + record.displayNameZhCn + " // "
                    + record.assetKey.substring(0, Math.min(18, record.assetKey.length()))), false);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException error) {
            source.sendFailure(Component.literal("标注失败: " + error.getMessage()));
            return 0;
        }
    }

    private static int unmarkHand(net.minecraft.commands.CommandSourceStack source) {
        AssetRecord record = AssetStackCodec.capture(heldStack());
        if (record == null) {
            source.sendFailure(Component.literal("主手物品尚未标注"));
            return 0;
        }
        try {
            if (!AssetRepository.get().unselect(record.assetKey)) {
                source.sendFailure(Component.literal("主手物品尚未标注"));
                return 0;
            }
            source.sendSuccess(() -> Component.literal("已取消标注 " + record.displayNameZhCn), false);
            return Command.SINGLE_SUCCESS;
        } catch (RuntimeException error) {
            source.sendFailure(Component.literal("取消标注失败: " + error.getMessage()));
            return 0;
        }
    }

    private static int export(net.minecraft.commands.CommandSourceStack source) {
        try {
            Path output = AssetRepository.get().exportSnapshot();
            source.sendSuccess(() -> Component.literal("已导出 " + output), false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception error) {
            source.sendFailure(Component.literal("导出失败: " + error.getMessage()));
            return 0;
        }
    }

    private static ItemStack heldStack() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getMainHandItem();
    }
}
