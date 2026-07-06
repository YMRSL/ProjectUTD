package com.github.sculkhorde.core;

import com.github.sculkhorde.common.advancement.ContributeTrigger;
import com.github.sculkhorde.common.advancement.GravemindEvolveImmatureTrigger;
import com.github.sculkhorde.common.advancement.GravemindEvolveMatureTrigger;
import com.github.sculkhorde.common.advancement.SculkHordeDefeatTrigger;
import com.github.sculkhorde.common.advancement.SculkHordeStartTrigger;
import com.github.sculkhorde.common.advancement.SculkNodeSpawnTrigger;
import com.github.sculkhorde.common.advancement.SoulHarvesterTrigger;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义成就触发器注册。
 *
 * 1.21 起 {@code minecraft:trigger_type} 是正式注册表, 在注册阶段结束后即冻结。原代码在 FMLCommonSetupEvent
 * 的 afterCommonSetup() 里用 CriteriaTriggers.register(...) 注册 → 此时已冻结 → "Registry is already frozen"。
 * 改用 DeferredRegister 在注册阶段(mod 总线)注册。以 {@code () -> XxxTrigger.INSTANCE} 注册既有单例,
 * 使注册表对象与各处引用的 {@code XxxTrigger.INSTANCE} 为同一实例, 触发逻辑无需改动。
 */
public class ModTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, SculkHorde.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, GravemindEvolveImmatureTrigger> GRAVEMIND_EVOLVE_IMMATURE =
            TRIGGERS.register(GravemindEvolveImmatureTrigger.ID.getPath(), () -> GravemindEvolveImmatureTrigger.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, GravemindEvolveMatureTrigger> GRAVEMIND_EVOLVE_MATURE =
            TRIGGERS.register(GravemindEvolveMatureTrigger.ID.getPath(), () -> GravemindEvolveMatureTrigger.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, SculkHordeStartTrigger> SCULK_HORDE_START =
            TRIGGERS.register(SculkHordeStartTrigger.ID.getPath(), () -> SculkHordeStartTrigger.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, SculkNodeSpawnTrigger> SCULK_NODE_SPAWN =
            TRIGGERS.register(SculkNodeSpawnTrigger.ID.getPath(), () -> SculkNodeSpawnTrigger.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, SoulHarvesterTrigger> SOUL_HARVESTER =
            TRIGGERS.register(SoulHarvesterTrigger.ID.getPath(), () -> SoulHarvesterTrigger.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, SculkHordeDefeatTrigger> SCULK_HORDE_DEFEAT =
            TRIGGERS.register(SculkHordeDefeatTrigger.ID.getPath(), () -> SculkHordeDefeatTrigger.INSTANCE);
    public static final DeferredHolder<CriterionTrigger<?>, ContributeTrigger> CONTRIBUTE =
            TRIGGERS.register(ContributeTrigger.ID.getPath(), () -> ContributeTrigger.INSTANCE);

    public static void register(IEventBus bus) {
        TRIGGERS.register(bus);
    }
}
