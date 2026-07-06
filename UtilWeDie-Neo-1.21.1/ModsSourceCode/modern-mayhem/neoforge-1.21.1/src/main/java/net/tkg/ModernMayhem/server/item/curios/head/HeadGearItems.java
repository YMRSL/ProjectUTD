package net.tkg.ModernMayhem.server.item.curios.head;

import java.util.function.Consumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.tkg.ModernMayhem.client.renderer.curios.head.HeadGearRenderer;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class HeadGearItems
extends Item
implements GeoItem,
ICurioItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private final int type;
    private final int variant;
    private final boolean hasFaceWearCapability;
    private static final TagKey<Item> GAS_MASK_INCOMPATIBLE = ItemTags.create((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"gas_mask_incompatible"));

    public HeadGearItems(int type, int variant) {
        this(type, variant, false);
    }

    public HeadGearItems(int type, int variant, boolean pHasFaceWearCapability) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
        this.type = type;
        this.variant = variant;
        this.hasFaceWearCapability = pHasFaceWearCapability;
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private GeoArmorRenderer<?> lRenderer;

            @NotNull
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.lRenderer == null) {
                    this.lRenderer = new HeadGearRenderer();
                }
                this.lRenderer.prepForRender((Entity)livingEntity, itemStack, equipmentSlot, original);
                return this.lRenderer;
            }
            private HeadGearRenderer.HeadGearItemRenderer renderer = null;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new HeadGearRenderer.HeadGearItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    public void curioTick(SlotContext slotContext, ItemStack stack) {
        ItemStack helmet;
        Player player;
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        if (this.type != 5) {
            return;
        }
        if (entity instanceof Player && !(player = (Player)entity).level().isClientSide() && !(helmet = player.getItemBySlot(EquipmentSlot.HEAD)).isEmpty() && helmet.is(GAS_MASK_INCOMPATIBLE)) {
            CuriosApi.getCuriosInventory((LivingEntity)player).ifPresent(curios -> curios.getStacksHandler(slotContext.identifier()).ifPresent(handler -> {
                ItemStack removedStack = handler.getStacks().getStackInSlot(slotContext.index()).copy();
                handler.getStacks().setStackInSlot(slotContext.index(), ItemStack.EMPTY);
                boolean added = player.getInventory().add(removedStack);
                if (!added) {
                    player.drop(removedStack, false);
                }
            }));
        }
    }

    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player)) {
            return false;
        }
        Player player = (Player)entity;
        if (!player.isAddedToLevel()) {
            return true;
        }
        if (this.type != 5) {
            return true;
        }
        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        return headItem.isEmpty() || !headItem.is(GAS_MASK_INCOMPATIBLE);
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public int getType() {
        return this.type;
    }

    public int getVariant() {
        return this.variant;
    }

    public boolean hasFaceWearCapability() {
        return this.hasFaceWearCapability;
    }
}

