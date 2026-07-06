package com.scarasol.zombiekit;

import com.mojang.logging.LogUtils;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.*;
import com.scarasol.zombiekit.item.armor.ModArmorMaterial;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(ZombieKitMod.MODID)
public class ZombieKitMod {
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "zombiekit";


    public ZombieKitMod(IEventBus modBus, ModContainer modContainer) {
        // DeferredRegisters
        ZombieKitDataComponents.COMPONENTS.register(modBus);
        ZombieKitSounds.REGISTRY.register(modBus);
        ZombieKitParticleTypes.REGISTRY.register(modBus);
        ZombieKitBlocks.REGISTRY.register(modBus);
        ModArmorMaterial.REGISTRY.register(modBus);
        ZombieKitItems.REGISTRY.register(modBus);
        ZombieKitEntities.REGISTRY.register(modBus);
        ZombieKitBlockEntities.REGISTRY.register(modBus);
        ZombieKitMenus.REGISTRY.register(modBus);
        ZombieKitTabs.REGISTRY.register(modBus);

        // Config
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "zombiekit-common.toml");

        // 服务端/通用事件由独立的 @EventBusSubscriber 类(EventHandler 等)处理；
        // 本主类无 @SubscribeEvent 方法，1.21.1 下 register(this) 会抛 "no @SubscribeEvent methods" 崩溃，故移除。

        // 网络包：由 network.NetworkHandler 监听 RegisterPayloadHandlersEvent(modBus) 自注册，
        // 见 NetworkHandler（network 代理负责）。如该类提供静态注册入口，可在此调用。
    }
}
