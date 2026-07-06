package com.scarasol.zombiekit.item;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.client.model.ZombieKitGeoItemModel;
import com.scarasol.zombiekit.client.renderer.ZombieKitGeoItemRenderer;
import com.scarasol.zombiekit.item.api.BaseZombieKitGeoItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;

public class ZombieKitGeoBlockItem extends BlockItem implements BaseZombieKitGeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ZombieKitGeoBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new ZombieKitGeoItemRenderer<>(new ZombieKitGeoItemModel<ZombieKitGeoBlockItem>());

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
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
        String id = ForgeRegistries.ITEMS.getKey(this).getPath();
        return new ResourceLocation(ZombieKitMod.MODID, "textures/block/" + id + ".png");
    }

    @Override
    public ResourceLocation getModel() {
        String id = ForgeRegistries.ITEMS.getKey(this).getPath();
        return new ResourceLocation(ZombieKitMod.MODID, "geo/" + id + ".geo.json");
    }
}
