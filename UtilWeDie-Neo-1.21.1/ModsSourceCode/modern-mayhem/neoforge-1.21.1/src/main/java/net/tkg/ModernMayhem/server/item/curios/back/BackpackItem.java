package net.tkg.ModernMayhem.server.item.curios.back;

import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.tkg.ModernMayhem.client.renderer.curios.back.BackpackRenderer;
import net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem;
import net.tkg.ModernMayhem.server.util.BackpackStorageProperties;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class BackpackItem
extends GenericBackpackItem
implements GeoItem,
ICurioItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final int variant;
    private final int tier;

    public BackpackItem(int variant, int tier) {
        super((byte)0);
        this.variant = variant;
        this.tier = tier;
    }

    @Override
    public int getInventoryLines() {
        return BackpackStorageProperties.getByTier(this.tier).getLines();
    }

    @Override
    public int getInventoryColumns() {
        return BackpackStorageProperties.getByTier(this.tier).getColumns();
    }

    @Override
    public boolean canSupplyAmmo() {
        return false;
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private GeoArmorRenderer<?> lRenderer;

            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.lRenderer == null) {
                    this.lRenderer = new BackpackRenderer();
                }
                this.lRenderer.prepForRender((Entity)livingEntity, itemStack, equipmentSlot, original);
                return this.lRenderer;
            }
            private BackpackRenderer.BackpackSlotRenderer renderer = null;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new BackpackRenderer.BackpackSlotRenderer();
                }
                return this.renderer;
            }
        });
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "controller", 0, state -> PlayState.CONTINUE)});
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public int getVariant() {
        return this.variant;
    }

    public int getTier() {
        return this.tier;
    }
}

