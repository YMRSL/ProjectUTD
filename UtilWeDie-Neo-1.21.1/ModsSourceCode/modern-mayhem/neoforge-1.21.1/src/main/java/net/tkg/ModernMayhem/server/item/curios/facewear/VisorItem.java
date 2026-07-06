package net.tkg.ModernMayhem.server.item.curios.facewear;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.tkg.ModernMayhem.client.renderer.curios.facewear.GenericSpecialGogglesRenderer;
import net.tkg.ModernMayhem.server.item.NVGGoggleList;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosFacewearProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.SlotContext;

public class VisorItem
extends GenericSpecialGogglesItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final NVGGoggleList config;

    public VisorItem(NVGGoggleList nvgGoggleList) {
        super(nvgGoggleList.getConfigs(), nvgGoggleList.getConfigIndex(), nvgGoggleList.getActivationSound(), nvgGoggleList.getDeactivationSound(), GenericSpecialGogglesItem.GoggleType.VISOR);
        this.config = nvgGoggleList;
    }

    @Override
    public boolean shouldRenderShader() {
        return false;
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        HashMultimap<Holder<Attribute>, AttributeModifier> multimap = HashMultimap.create();
        if (VisorItem.isNVGOnFace(stack)) {
            double protection = CuriosFacewearProperties.VISOR.getProtection();
            double toughness = CuriosFacewearProperties.VISOR.getToughness();
            double knockback = CuriosFacewearProperties.VISOR.getKnockback();
            if (protection > 0.0) {
                multimap.put(Attributes.ARMOR, new AttributeModifier(id.withSuffix("_armor"), protection, AttributeModifier.Operation.ADD_VALUE));
            }
            if (toughness > 0.0) {
                multimap.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(id.withSuffix("_toughness"), toughness, AttributeModifier.Operation.ADD_VALUE));
            }
            if (knockback > 0.0) {
                multimap.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(id.withSuffix("_knockback"), knockback, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        return multimap;
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private GenericSpecialGogglesRenderer<VisorItem> lRenderer;

            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.lRenderer == null) {
                    this.lRenderer = new GenericSpecialGogglesRenderer();
                }
                this.lRenderer.prepForRender((Entity)livingEntity, itemStack, equipmentSlot, original);
                return this.lRenderer;
            }
            private GenericSpecialGogglesRenderer.GenericNVGGogglesSlotRenderer<VisorItem> renderer = null;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GenericSpecialGogglesRenderer.GenericNVGGogglesSlotRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add((Component)Component.translatable((String)"description.mm.nvgs").withStyle(ChatFormatting.GRAY));
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public NVGGoggleList getConfig() {
        return this.config;
    }
}

