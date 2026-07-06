package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.concurrent.atomic.AtomicBoolean;

public class ZombieKitTabs {

    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ZombieKitMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ZOMBIEKIT_MISC = REGISTRY.register("zombiekit_misc",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.zombiekit_misc"))
                    .icon(() -> new ItemStack(ZombieKitItems.PLASTICS.get()))
                    .displayItems((features, output) -> {
                        AtomicBoolean flag = new AtomicBoolean(false);
                        ZombieKitItems.REGISTRY.getEntries().forEach(item -> {
                            if (item.get() == ZombieKitItems.DESERT_CAMOUFLAGE_DYE.get())
                                flag.set(true);
                            if (item.get() == ZombieKitItems.SCARASOL.get())
                                flag.set(false);
                            if (flag.get())
                                output.accept(item.get());
                        });
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_ZOMBIEKIT_COMBAT = REGISTRY.register("zombiekit_combat",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.zombiekit_combat"))
                    .icon(() -> new ItemStack(ZombieKitItems.MOLOTOV_COCKTAIL.get()))
                    .displayItems((features, output) -> {
                        AtomicBoolean flag = new AtomicBoolean(true);
                        ZombieKitItems.REGISTRY.getEntries().forEach(item -> {
                            if (item.get() == ZombieKitItems.PRIMARY_LIGHT_WEIGHTED_GRIP.get())
                                flag.set(false);
                            if (flag.get())
                                output.accept(item.get());
                        });
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_ZOMBIEKIT_TOOL = REGISTRY.register("zombiekit_tool",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.zombiekit_tool"))
                    .icon(() -> new ItemStack(ZombieKitItems.MIRACLE.get()))
                    .displayItems((features, output) -> {
                        AtomicBoolean flag = new AtomicBoolean(false);
                        ZombieKitItems.REGISTRY.getEntries().forEach(item -> {
                            if (item.get() == ZombieKitItems.BANDAGE.get())
                                flag.set(true);
                            if (item.get() == ZombieKitItems.DESERT_CAMOUFLAGE_DYE.get())
                                flag.set(false);
                            if (flag.get())
                                output.accept(item.get());
                        });
                    }).build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB_ZOMBIEKIT_PARTS = REGISTRY.register("zombiekit_parts",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.zombiekit_parts"))
                    .icon(() -> new ItemStack(ZombieKitItems.PRIMARY_LIGHT_WEIGHTED_GRIP.get()))
                    .displayItems((features, output) -> {
                        AtomicBoolean flag = new AtomicBoolean(false);
                        ZombieKitItems.REGISTRY.getEntries().forEach(item -> {
                            if (item.get() == ZombieKitItems.PRIMARY_LIGHT_WEIGHTED_GRIP.get())
                                flag.set(true);
                            if (item.get() == ZombieKitItems.BANDAGE.get())
                                flag.set(false);
                            if (flag.get())
                                output.accept(item.get());
                        });
                    }).build());

}
