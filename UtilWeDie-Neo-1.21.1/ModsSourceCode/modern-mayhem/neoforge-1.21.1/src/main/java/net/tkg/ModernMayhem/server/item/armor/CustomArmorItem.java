package net.tkg.ModernMayhem.server.item.armor;

import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.tkg.ModernMayhem.client.renderer.armor.CustomArmorRenderer;
import net.tkg.ModernMayhem.client.renderer.armor.item.CustomArmorItemRenderer;
import net.tkg.ModernMayhem.server.item.generic.GenericStatConfigurableArmorItem;
import net.tkg.ModernMayhem.server.util.ArmorProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

public class CustomArmorItem
extends GenericStatConfigurableArmorItem
implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final int variant;
    private final ArmorProperties config;

    public CustomArmorItem(ArmorProperties pConfig, ArmorItem.Type pType, int pVariant) {
        super(pConfig, pType);
        this.variant = pVariant;
        this.config = pConfig;
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private GeoArmorRenderer<?> armorRenderer;

            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new CustomArmorRenderer(slot);
                }
                this.armorRenderer.prepForRender((Entity)entity, stack, slot, original);
                return this.armorRenderer;
            }

            @Nullable
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new CustomArmorItemRenderer();
            }
        });
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public int getVariant() {
        return this.variant;
    }

    public ArmorProperties getConfig() {
        return this.config;
    }
}

