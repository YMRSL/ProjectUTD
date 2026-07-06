package com.scarasol.zombiekit.item;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.item.api.BaseZombieKitGeoItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ZombieKitGeoBlockItem extends BlockItem implements BaseZombieKitGeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ZombieKitGeoBlockItem(Block block, Properties properties) {
        super(block, properties);
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
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "textures/block/" + id + ".png");
    }

    @Override
    public ResourceLocation getModel() {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        return ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "geo/" + id + ".geo.json");
    }
}
