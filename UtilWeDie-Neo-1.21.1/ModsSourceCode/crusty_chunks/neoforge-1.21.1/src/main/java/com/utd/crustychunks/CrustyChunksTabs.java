package com.utd.crustychunks;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrustyChunksTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CrustyChunksMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PARTS = TABS.register("parts", () ->
            CreativeModeTab.builder()
                    .title(Component.literal("Warium Parts"))
                    .icon(() -> new ItemStack(CrustyChunksItems.STEEL_COMPONENT.get()))
                    .displayItems((params, output) -> CrustyChunksItems.ALL.forEach(i -> output.accept(i.get())))
                    .build());

    private CrustyChunksTabs() {
    }
}
