package net.tkg.ModernMayhem.server.item.curios.facewear;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.tkg.ModernMayhem.client.renderer.curios.facewear.GenericSpecialGogglesRenderer;
import net.tkg.ModernMayhem.server.item.NVGGoggleList;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NVGGogglesItem
extends GenericSpecialGogglesItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final boolean isGamerNVG;
    private final NVGGoggleList config;

    public NVGGogglesItem(NVGGoggleList nvgGoggleList) {
        super(nvgGoggleList.getConfigs(), nvgGoggleList.getConfigIndex(), nvgGoggleList.getActivationSound(), nvgGoggleList.getDeactivationSound(), GenericSpecialGogglesItem.GoggleType.NIGHT_VISION, NVGGogglesItem.canHoldCoti(nvgGoggleList), NVGGogglesItem.hasAutoGain(nvgGoggleList), NVGGogglesItem.hasAutoGating(nvgGoggleList));
        this.isGamerNVG = nvgGoggleList == NVGGoggleList.GAMER_GPNVG;
        this.config = nvgGoggleList;
    }

    private static boolean hasAutoGating(NVGGoggleList config) {
        return config == NVGGoggleList.BLACK_PVS14 || config == NVGGoggleList.TAN_PVS14 || config == NVGGoggleList.GREEN_PVS14 || config == NVGGoggleList.BLACK_GPNVG || config == NVGGoggleList.TAN_GPNVG;
    }

    private static boolean hasAutoGain(NVGGoggleList config) {
        return config == NVGGoggleList.BLACK_PVS14 || config == NVGGoggleList.TAN_PVS14 || config == NVGGoggleList.GREEN_PVS14 || config == NVGGoggleList.BLACK_GPNVG || config == NVGGoggleList.TAN_GPNVG;
    }

    private static boolean canHoldCoti(NVGGoggleList config) {
        return config == NVGGoggleList.BLACK_PVS14 || config == NVGGoggleList.TAN_PVS14 || config == NVGGoggleList.GREEN_PVS14 || config == NVGGoggleList.BLACK_PVS7;
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private GenericSpecialGogglesRenderer<NVGGogglesItem> lRenderer;

            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.lRenderer == null) {
                    this.lRenderer = new GenericSpecialGogglesRenderer();
                }
                this.lRenderer.prepForRender((Entity)livingEntity, itemStack, equipmentSlot, original);
                return this.lRenderer;
            }
            private GenericSpecialGogglesRenderer.GenericNVGGogglesSlotRenderer<NVGGogglesItem> renderer = null;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new GenericSpecialGogglesRenderer.GenericNVGGogglesSlotRenderer();
                }
                return this.renderer;
            }
        });
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (this.isGamerNVG) {
            tooltip.add((Component)Component.translatable((String)"description.mm.ultra_gamer_gpnvg").withStyle(ChatFormatting.RED));
        }
        tooltip.add((Component)Component.translatable((String)"description.mm.nvgs").withStyle(ChatFormatting.GRAY));
    }

    public boolean isGamerNVG() {
        return this.isGamerNVG;
    }

    /** IR 红外照明分档 (单/双/四镜头)。供动态光锥系统按档位算亮度与范围。 */
    public NVGGoggleList.IrTier getIrTier() {
        return this.config.getIrTier();
    }

    /** 是否四镜头 GPNVG (仅它的 IR 可主动开关)。 */
    public boolean isQuad() {
        return this.config.isQuad();
    }

    @Override
    public NVGGoggleList getConfig() {
        return this.config;
    }
}

