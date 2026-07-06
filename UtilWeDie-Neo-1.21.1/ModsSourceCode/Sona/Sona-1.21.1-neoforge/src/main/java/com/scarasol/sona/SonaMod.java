package com.scarasol.sona;

import com.mojang.logging.LogUtils;
import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.init.SonaDataComponents;
import com.scarasol.sona.init.SonaEntities;
import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.sona.init.SonaSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Sona 主类（NeoForge 1.21.1）。
 *
 * <p>1.20.1 Forge → 1.21.1 NeoForge 迁移：</p>
 * <ul>
 *   <li>构造签名 {@code (IEventBus modBus, ModContainer modContainer)}，不再用
 *       {@code FMLJavaModLoadingContext} / {@code ModLoadingContext}。</li>
 *   <li>各 DeferredRegister 显式绑定 modBus；新增 {@link SonaDataComponents}（1.21 用
 *       DataComponent 取代 ItemStack NBT，承载腐烂/锈蚀/聊天数据，必须注册）。</li>
 *   <li>配置经 {@code modContainer.registerConfig(...)} 注册。</li>
 *   <li>网络（{@code NetworkHandler}）、各 init 类的属性注册、命令/Tick 事件胶水
 *       （{@code ManagerEventHandler}/{@code EffectEventHandler} 等）均通过
 *       {@code @EventBusSubscriber} 自动发现，无需在此手动 register。</li>
 *   <li>DROP：上游本类只注册了 Sounds/MobEffects/Entities/Network/Config —— 无僵尸实体、
 *       无 travelersbackpack/lostcities compat 注册（那些在已弃的 mixin/compat 里，不在此），
 *       故迁移后保持一致；实体仅保留 SOUND_DECOY（见 {@link SonaEntities}）。</li>
 * </ul>
 */
@Mod(SonaMod.MODID)
public class SonaMod {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "sona";

    public SonaMod(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "sona-common.toml");

        SonaSounds.REGISTRY.register(modBus);
        SonaMobEffects.REGISTRY.register(modBus);
        SonaEntities.REGISTRY.register(modBus);
        SonaDataComponents.COMPONENTS.register(modBus);
    }
}
