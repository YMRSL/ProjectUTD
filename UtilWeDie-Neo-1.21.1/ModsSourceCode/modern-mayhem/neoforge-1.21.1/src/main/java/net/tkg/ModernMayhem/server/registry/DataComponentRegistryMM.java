package net.tkg.ModernMayhem.server.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义 DataComponentType 注册。1.20.1 把 COTI 夹片以嵌套 ItemStack 形式存进护目镜的 NBT
 * (getOrCreateTag/ItemStack.of); 1.21.1 用一个 DataComponentType<ItemStack> 承载, 由组件系统
 * 自带 codec 序列化 —— 调用点无需 HolderLookup.Provider。
 */
public class DataComponentRegistryMM {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "mm");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStack>> COTI_CONTENTS = DATA_COMPONENTS.register("coti_contents", () -> DataComponentType.<ItemStack>builder()
            .persistent(ItemStack.CODEC)
            .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC)
            .build());

    public static void init(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
