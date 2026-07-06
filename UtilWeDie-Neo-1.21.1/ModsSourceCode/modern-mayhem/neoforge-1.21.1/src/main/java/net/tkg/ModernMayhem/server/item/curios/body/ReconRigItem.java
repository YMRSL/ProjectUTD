package net.tkg.ModernMayhem.server.item.curios.body;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.tkg.ModernMayhem.client.renderer.curios.body.ReconRigRenderer;
import net.tkg.ModernMayhem.server.config.CommonConfig;
import net.tkg.ModernMayhem.server.item.generic.GenericBackpackItem;
import net.tkg.ModernMayhem.server.util.CuriosBodyProperties;
import net.tkg.ModernMayhem.server.util.RigStorageProperties;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class ReconRigItem
extends GenericBackpackItem
implements GeoItem,
ICurioItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);

    public ReconRigItem() {
        super((byte)1);
    }

    @Override
    public int getInventoryLines() {
        return RigStorageProperties.RECON_RIG.getLines();
    }

    @Override
    public int getInventoryColumns() {
        return RigStorageProperties.RECON_RIG.getColumns();
    }

    @Override
    public boolean canSupplyAmmo() {
        return RigStorageProperties.RECON_RIG.suppliesAmmo();
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private GeoArmorRenderer<?> lRenderer;

            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.lRenderer == null) {
                    this.lRenderer = new ReconRigRenderer();
                }
                this.lRenderer.prepForRender((Entity)livingEntity, itemStack, equipmentSlot, original);
                return this.lRenderer;
            }
            private ReconRigRenderer.ReconRigSlotRenderer renderer = null;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ReconRigRenderer.ReconRigSlotRenderer();
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

    public boolean canBeDepleted() {
        return this.getMaxDamage(ItemStack.EMPTY) > 0;
    }

    public int getMaxDamage(ItemStack stack) {
        return CuriosBodyProperties.RECON_RIG.getDurability();
    }

    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        int newDamage;
        if (((Boolean)CommonConfig.ENABLE_DYNAMIC_ARMOR_STATS.get()).booleanValue() && (newDamage = stack.getDamageValue() + amount) >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage());
            return 0;
        }
        return super.damageItem(stack, amount, entity, onBroken);
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        int maxD;
        HashMultimap<Holder<Attribute>, AttributeModifier> multimap = HashMultimap.create();
        double protection = CuriosBodyProperties.RECON_RIG.getProtection();
        double toughness = CuriosBodyProperties.RECON_RIG.getToughness();
        double knockback = CuriosBodyProperties.RECON_RIG.getKnockback();
        if (((Boolean)CommonConfig.ENABLE_DYNAMIC_ARMOR_STATS.get()).booleanValue() && (maxD = stack.getMaxDamage()) > 0) {
            int currentD = stack.getDamageValue();
            double durabilityPercent = 1.0 - (double)currentD / (double)maxD;
            if (durabilityPercent < 0.0) {
                durabilityPercent = 0.0;
            }
            double factor = 0.1 + 0.9 * durabilityPercent;
            protection *= factor;
            toughness *= factor;
            knockback *= factor;
        }
        if (protection > 0.0) {
            multimap.put(Attributes.ARMOR, new AttributeModifier(id.withSuffix("_armor"), protection, AttributeModifier.Operation.ADD_VALUE));
        }
        if (toughness > 0.0) {
            multimap.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id.withSuffix("_toughness"), toughness, AttributeModifier.Operation.ADD_VALUE));
        }
        if (knockback > 0.0) {
            multimap.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id.withSuffix("_knockback"), knockback, AttributeModifier.Operation.ADD_VALUE));
        }
        return multimap;
    }
}

