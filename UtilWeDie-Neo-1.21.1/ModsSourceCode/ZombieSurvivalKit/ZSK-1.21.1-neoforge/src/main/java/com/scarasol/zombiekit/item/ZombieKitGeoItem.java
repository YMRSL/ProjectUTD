package com.scarasol.zombiekit.item;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.item.api.BaseZombieKitGeoItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ZombieKitGeoItem extends Item implements BaseZombieKitGeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ZombieKitGeoItem(Properties properties) {
        super(properties);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getTexture() {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "textures/item/" + id + ".png");
    }

    @Override
    public ResourceLocation getModel() {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "geo/" + id + ".geo.json");
    }
}
