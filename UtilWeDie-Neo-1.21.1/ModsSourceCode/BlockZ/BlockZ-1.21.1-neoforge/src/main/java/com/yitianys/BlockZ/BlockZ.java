package com.yitianys.BlockZ;

import com.yitianys.BlockZ.capability.BlockZPlayerItemHandler;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.event.ModEvents;
import com.yitianys.BlockZ.init.BlockZAttachments;
import com.yitianys.BlockZ.init.BlockZDataComponents;
import com.yitianys.BlockZ.init.ModCreativeTabs;
import com.yitianys.BlockZ.init.ModItems;
import com.yitianys.BlockZ.init.ModMenus;
import com.yitianys.BlockZ.init.ModSounds;
import com.yitianys.BlockZ.network.NetworkHandler;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod(BlockZ.MODID)
public class BlockZ {
    public static final String MODID = "blockz";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlockZ(IEventBus modBus, ModContainer modContainer) {
        BlockZConfigs.register();
        modContainer.registerConfig(ModConfig.Type.COMMON, BlockZConfigs.COMMON_SPEC);

        // 注册器（绑定 modBus）
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modBus);
        ModMenus.MENUS.register(modBus);
        BlockZAttachments.ATTACHMENTS.register(modBus);
        BlockZDataComponents.COMPONENTS.register(modBus);

        // 网络包注册：NetworkHandler 通过 @EventBusSubscriber(bus=MOD) 自监听 RegisterPayloadHandlersEvent，无需在此显式调用。

        // modBus 生命周期 / capability / 配置重载
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::onConfigReload);

        // GAME bus（命令等）
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ItemSizeManager::loadCustomSizes);
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerEntity(
                Capabilities.ItemHandler.ENTITY,
                EntityType.PLAYER,
                (player, ctx) -> new BlockZPlayerItemHandler(player)
        );
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("blockz")
                .requires(stack -> stack.hasPermission(2))
                .then(Commands.literal("reload")
                    .executes(context -> {
                        ItemSizeManager.loadCustomSizes();
                        MinecraftServer server = context.getSource().getServer();
                        ModEvents.broadcastServerConfigs(server);
                        context.getSource().sendSuccess(() -> Component.literal("BlockZ 配置已重载！"), true);
                        return 1;
                    })
                )
        );
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        ModConfig config = event.getConfig();
        if (config == null || config.getSpec() == null) {
            return;
        }
        if (config.getSpec() != BlockZConfigs.COMMON_SPEC) {
            return;
        }
        ItemSizeManager.loadCustomSizes();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ModEvents.broadcastServerConfigs(server);
        }
    }
}
