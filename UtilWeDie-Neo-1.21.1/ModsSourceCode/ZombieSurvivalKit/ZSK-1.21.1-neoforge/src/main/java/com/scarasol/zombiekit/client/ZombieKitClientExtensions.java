package com.scarasol.zombiekit.client;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.client.model.ZombieKitGeoItemModel;
import com.scarasol.zombiekit.client.renderer.FlameThrowerRenderer;
import com.scarasol.zombiekit.client.renderer.ZombieKitGeoItemRenderer;
import com.scarasol.zombiekit.item.ZombieKitGeoItem;
import com.scarasol.zombiekit.item.api.BaseZombieKitGeoItem;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * 1.21.1 NeoForge 移除了 Item#initializeClient，GeoLib 物品的自定义渲染器(BEWLR)改由
 * {@link RegisterClientExtensionsEvent} 注册。上游每个 GeoItem 在 initializeClient 里挂渲染器，
 * 迁移时这部分被删但未补回，导致喷火器/炮弹架/迫击炮弹等 geo 物品在物品栏里不渲染(空白)。
 * 这里统一补上:所有 {@link BaseZombieKitGeoItem} 用 GeoItemRenderer，Flamethrower 用其专属渲染器。
 */
@EventBusSubscriber(modid = ZombieKitMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ZombieKitClientExtensions {

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        final IClientItemExtensions geoExt = new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer =
                    new ZombieKitGeoItemRenderer<ZombieKitGeoItem>(new ZombieKitGeoItemModel<ZombieKitGeoItem>());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };
        final IClientItemExtensions flameExt = new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer =
                    new FlameThrowerRenderer(new ZombieKitGeoItemModel<Flamethrower>());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        };

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof BaseZombieKitGeoItem)) {
                continue;
            }
            if (item instanceof Flamethrower) {
                event.registerItem(flameExt, item);
            } else {
                event.registerItem(geoExt, item);
            }
        }
    }
}
