package net.tkg.ModernMayhem.server.item.generic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.tkg.ModernMayhem.server.item.NVGGoggleList;
import net.tkg.ModernMayhem.server.registry.DataComponentRegistryMM;
import net.tkg.ModernMayhem.server.registry.ItemRegistryMM;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import net.tkg.ModernMayhem.server.util.ItemNBTUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public abstract class GenericSpecialGogglesItem
extends Item
implements GeoItem,
ICurioItem {
    private NVGConfig[] configs;
    private int configIndex = 0;
    private static int defaultConfigIndex = 0;
    public DeferredHolder<SoundEvent, SoundEvent> ACTIVATION_SOUND = null;
    public DeferredHolder<SoundEvent, SoundEvent> DEACTIVATION_SOUND = null;
    private static final NVGConfig FALLBACK_CONFIG = new NVGConfig(1.0f, 1.0f, 1.0f, 1.0f);
    public static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("opened");
    public static final RawAnimation ANIM_OPEN = RawAnimation.begin().thenPlay("opening").thenLoop("opened");
    public static final RawAnimation ANIM_CLOSE = RawAnimation.begin().thenPlay("closing").thenLoop("closed");
    private static final Map<UUID, Integer> lastConfigIndexMap = new HashMap<UUID, Integer>();
    private static final TagKey<Item> HAS_HEAD_MOUNT_TAG = ItemTags.create((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"has_head_mount"));
    private static final TagKey<Item> HAS_VISOR_MOUNT_TAG = ItemTags.create((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"mm", (String)"has_visor_mount"));
    private static final String COTI_CONTENTS_TAG = "CotiContents";
    private final GoggleType goggleType;
    private final boolean canHoldCoti;
    private final boolean hasAutoGain;
    private final boolean hasAutoGating;

    public abstract NVGGoggleList getConfig();

    public GenericSpecialGogglesItem(NVGConfig pConfig) {
        super(new Item.Properties().stacksTo(1).durability(0));
        defaultConfigIndex = 0;
        this.goggleType = GoggleType.NIGHT_VISION;
        this.canHoldCoti = false;
        this.hasAutoGain = false;
        this.hasAutoGating = false;
    }

    public GenericSpecialGogglesItem(NVGConfig pConfig, DeferredHolder<SoundEvent, SoundEvent> pActivationSound, DeferredHolder<SoundEvent, SoundEvent> pDeactivationSound) {
        super(new Item.Properties().stacksTo(1).durability(0));
        defaultConfigIndex = 0;
        this.ACTIVATION_SOUND = pActivationSound;
        this.DEACTIVATION_SOUND = pDeactivationSound;
        this.goggleType = GoggleType.NIGHT_VISION;
        this.canHoldCoti = false;
        this.hasAutoGain = false;
        this.hasAutoGating = false;
    }

    public GenericSpecialGogglesItem(NVGConfig[] pConfigs, int startConfigIndex, DeferredHolder<SoundEvent, SoundEvent> pActivationSound, DeferredHolder<SoundEvent, SoundEvent> pDeactivationSound) {
        super(new Item.Properties().stacksTo(1).durability(0));
        this.configs = pConfigs;
        defaultConfigIndex = startConfigIndex;
        this.ACTIVATION_SOUND = pActivationSound;
        this.DEACTIVATION_SOUND = pDeactivationSound;
        this.goggleType = GoggleType.NIGHT_VISION;
        this.canHoldCoti = false;
        this.hasAutoGain = false;
        this.hasAutoGating = false;
        this.calculateBrightnessRange();
    }

    public GenericSpecialGogglesItem(NVGConfig[] pConfigs, int startConfigIndex, DeferredHolder<SoundEvent, SoundEvent> pActivationSound, DeferredHolder<SoundEvent, SoundEvent> pDeactivationSound, GoggleType pGoggleType, boolean canHoldCoti, boolean pHasAutoGain, boolean pHasAutoGating) {
        super(new Item.Properties().stacksTo(1).durability(0));
        this.configs = pConfigs;
        defaultConfigIndex = startConfigIndex;
        this.ACTIVATION_SOUND = pActivationSound;
        this.DEACTIVATION_SOUND = pDeactivationSound;
        this.goggleType = pGoggleType;
        this.canHoldCoti = canHoldCoti;
        this.hasAutoGain = pHasAutoGain;
        this.hasAutoGating = pHasAutoGating;
        this.calculateBrightnessRange();
    }

    public GenericSpecialGogglesItem(NVGConfig[] pConfigs, int startConfigIndex, DeferredHolder<SoundEvent, SoundEvent> pActivationSound, DeferredHolder<SoundEvent, SoundEvent> pDeactivationSound, GoggleType pGoggleType) {
        super(new Item.Properties().stacksTo(1).durability(0));
        this.configs = pConfigs;
        defaultConfigIndex = startConfigIndex;
        this.ACTIVATION_SOUND = pActivationSound;
        this.DEACTIVATION_SOUND = pDeactivationSound;
        this.goggleType = pGoggleType;
        this.canHoldCoti = false;
        this.hasAutoGain = false;
        this.hasAutoGating = false;
        this.calculateBrightnessRange();
    }

    public boolean shouldRenderShader() {
        return true;
    }

    public boolean canHoldCoti() {
        return this.canHoldCoti;
    }

    public boolean hasAutoGain() {
        return this.hasAutoGain;
    }

    public boolean hasAutoGating() {
        return this.hasAutoGating;
    }

    public GoggleType getGoggleType() {
        return this.goggleType;
    }

    public boolean overrideStackedOnOther(@NotNull ItemStack nvgStack, @NotNull Slot slot, @NotNull ClickAction clickAction, @NotNull Player player) {
        ItemStack takenStack;
        if (!this.canHoldCoti || nvgStack.getCount() != 1 || clickAction != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack slotStack = slot.getItem();
        if (slotStack.isEmpty()) {
            ItemStack cotiStack;
            if (GenericSpecialGogglesItem.hasCoti(nvgStack) && !(cotiStack = this.removeCoti(nvgStack)).isEmpty()) {
                this.playRemoveSound((Entity)player);
                ItemStack remaining = slot.safeInsert(cotiStack);
                if (!remaining.isEmpty()) {
                    this.insertCoti(nvgStack, remaining);
                }
                return true;
            }
        } else if (this.isCotiItem(slotStack) && !GenericSpecialGogglesItem.hasCoti(nvgStack) && !(takenStack = slot.safeTake(slotStack.getCount(), 1, player)).isEmpty()) {
            this.insertCoti(nvgStack, takenStack);
            this.playInsertSound((Entity)player);
            return true;
        }
        return false;
    }

    public boolean overrideOtherStackedOnMe(@NotNull ItemStack nvgStack, @NotNull ItemStack otherStack, @NotNull Slot slot, @NotNull ClickAction clickAction, @NotNull Player player, @NotNull SlotAccess slotAccess) {
        if (!this.canHoldCoti || nvgStack.getCount() != 1) {
            return false;
        }
        if (clickAction == ClickAction.SECONDARY && slot.allowModification(player)) {
            if (otherStack.isEmpty()) {
                ItemStack cotiStack;
                if (GenericSpecialGogglesItem.hasCoti(nvgStack) && !(cotiStack = this.removeCoti(nvgStack)).isEmpty()) {
                    this.playRemoveSound((Entity)player);
                    slotAccess.set(cotiStack);
                    return true;
                }
            } else if (this.isCotiItem(otherStack) && !GenericSpecialGogglesItem.hasCoti(nvgStack)) {
                ItemStack toInsert = otherStack.split(1);
                if (this.insertCoti(nvgStack, toInsert)) {
                    this.playInsertSound((Entity)player);
                    return true;
                }
                otherStack.grow(1);
            }
        }
        return false;
    }

    public Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        ItemStack cotiStack;
        if (!this.canHoldCoti) {
            return Optional.empty();
        }
        if (GenericSpecialGogglesItem.hasCoti(stack) && !(cotiStack = GenericSpecialGogglesItem.getCoti(stack)).isEmpty()) {
            NonNullList<ItemStack> contents = NonNullList.create();
            contents.add(cotiStack);
            return Optional.of(new BundleTooltip(new BundleContents(contents)));
        }
        return Optional.empty();
    }

    public boolean insertCoti(ItemStack nvgStack, ItemStack cotiStack) {
        if (!this.canHoldCoti || GenericSpecialGogglesItem.hasCoti(nvgStack) || !this.isCotiItem(cotiStack)) {
            return false;
        }
        nvgStack.set(DataComponentRegistryMM.COTI_CONTENTS.get(), cotiStack.copy());
        return true;
    }

    public ItemStack removeCoti(ItemStack nvgStack) {
        if (!GenericSpecialGogglesItem.hasCoti(nvgStack)) {
            return ItemStack.EMPTY;
        }
        ItemStack cotiStack = nvgStack.getOrDefault(DataComponentRegistryMM.COTI_CONTENTS.get(), ItemStack.EMPTY).copy();
        nvgStack.remove(DataComponentRegistryMM.COTI_CONTENTS.get());
        return cotiStack;
    }

    public static boolean hasCoti(ItemStack nvgStack) {
        Item item = nvgStack.getItem();
        if (!(item instanceof GenericSpecialGogglesItem)) {
            return false;
        }
        GenericSpecialGogglesItem nvgItem = (GenericSpecialGogglesItem)item;
        if (!nvgItem.canHoldCoti()) {
            return false;
        }
        return nvgStack.has(DataComponentRegistryMM.COTI_CONTENTS.get());
    }

    public static ItemStack getCoti(ItemStack nvgStack) {
        if (!GenericSpecialGogglesItem.hasCoti(nvgStack)) {
            return ItemStack.EMPTY;
        }
        return nvgStack.getOrDefault(DataComponentRegistryMM.COTI_CONTENTS.get(), ItemStack.EMPTY).copy();
    }

    private boolean isCotiItem(ItemStack stack) {
        return stack.is((Item)ItemRegistryMM.COTI.get());
    }

    private void playRemoveSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8f, 0.8f + entity.level().getRandom().nextFloat() * 0.4f);
    }

    private void playInsertSound(Entity entity) {
        entity.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + entity.level().getRandom().nextFloat() * 0.4f);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (this.canHoldCoti && GenericSpecialGogglesItem.hasCoti(stack)) {
            tooltip.add((Component)Component.translatable((String)"description.mm.nvg.coti_installed").withStyle(ChatFormatting.YELLOW));
        }
    }

    public static NVGConfig getCurrentConfig(ItemStack item) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(item);
        GenericSpecialGogglesItem itemInstance = (GenericSpecialGogglesItem)item.getItem();
        if (itemInstance.configs == null || itemInstance.configs.length == 0) {
            if (!tag.contains("configIndex")) {
                tag.putInt("configIndex", 0);
            }
            return FALLBACK_CONFIG;
        }
        int safeDefaultIndex = Math.min(defaultConfigIndex, itemInstance.configs.length - 1);
        if (tag.contains("configIndex")) {
            int idx = tag.getInt("configIndex");
            if (idx < 0) {
                idx = 0;
            } else if (idx >= itemInstance.configs.length) {
                idx = itemInstance.configs.length - 1;
            }
            tag.putInt("configIndex", idx);
            ItemNBTUtil.setTag(item, tag);
            return itemInstance.configs[idx];
        }
        tag.putInt("configIndex", safeDefaultIndex);
        ItemNBTUtil.setTag(item, tag);
        return itemInstance.configs[safeDefaultIndex];
    }

    private void updateNVGMode(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
        if (tag.contains("NvgCheck")) {
            boolean nvgCheck = tag.getBoolean("NvgCheck");
            tag.putInt("nvg_mode", nvgCheck ? 1 : 0);
        } else {
            tag.putInt("nvg_mode", 0);
        }
        ItemNBTUtil.setTag(stack, tag);
    }

    public static int getNVGMode(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getTag(stack);
        if (tag != null && tag.contains("nvg_mode")) {
            return tag.getInt("nvg_mode");
        }
        return 0;
    }

    public static boolean getNVGCheck(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getTag(stack);
        if (tag != null && tag.contains("NvgCheck")) {
            return tag.getBoolean("NvgCheck");
        }
        return false;
    }

    public static void switchNVGMode(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
        if (tag.contains("NvgCheck")) {
            boolean nvgCheck = tag.getBoolean("NvgCheck");
            tag.putBoolean("NvgCheck", !nvgCheck);
        } else {
            tag.putBoolean("NvgCheck", true);
        }
        ItemNBTUtil.setTag(stack, tag);
    }

    public static void switchOnNVGMode(ItemStack item) {
        System.out.println("[ModernMayhem] Switching on NVG mode for item: " + item);
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(item);
        tag.putBoolean("NvgCheck", true);
        ItemNBTUtil.setTag(item, tag);
    }

    public static void switchOffNVGMode(ItemStack item) {
        System.out.println("[ModernMayhem] Switching off NVG mode for item: " + item);
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(item);
        tag.putBoolean("NvgCheck", false);
        ItemNBTUtil.setTag(item, tag);
    }

    /** IR 主动红外照明开关 (仅四镜头 GPNVG 有意义; 单/双镜头常开、不读此值)。默认关闭。 */
    public static boolean isIrActive(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getTag(stack);
        if (tag != null && tag.contains("IrActive")) {
            return tag.getBoolean("IrActive");
        }
        return false;
    }

    public static void switchIrMode(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
        if (tag.contains("IrActive")) {
            boolean current = tag.getBoolean("IrActive");
            tag.putBoolean("IrActive", !current);
        } else {
            tag.putBoolean("IrActive", true);
        }
        ItemNBTUtil.setTag(stack, tag);
    }

    public static void switchEquipState(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
        if (tag.contains("NvgOnFace")) {
            boolean nvgCheck = tag.getBoolean("NvgOnFace");
            tag.putBoolean("NvgOnFace", !nvgCheck);
        } else {
            tag.putBoolean("NvgOnFace", true);
        }
        ItemNBTUtil.setTag(stack, tag);
    }

    public static boolean isNVGOnFace(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getTag(stack);
        if (tag != null && tag.contains("NvgOnFace")) {
            return tag.getBoolean("NvgOnFace");
        }
        return false;
    }

    public static void switchConfigUp(ItemStack item) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(item);
        GenericSpecialGogglesItem itemInstance = (GenericSpecialGogglesItem)item.getItem();
        if (itemInstance.configs == null || itemInstance.configs.length == 0) {
            tag.putInt("configIndex", 0);
            ItemNBTUtil.setTag(item, tag);
            return;
        }
        if (tag.contains("configIndex")) {
            int configIndex = tag.getInt("configIndex");
            if ((configIndex = Math.max(0, Math.min(configIndex, itemInstance.configs.length - 1))) < itemInstance.configs.length - 1) {
                tag.putInt("configIndex", configIndex + 1);
            }
            ItemNBTUtil.setTag(item, tag);
            return;
        }
        int safeDefaultIndex = Math.min(defaultConfigIndex, itemInstance.configs.length - 1);
        tag.putInt("configIndex", safeDefaultIndex);
        ItemNBTUtil.setTag(item, tag);
    }

    public static void switchConfigDown(ItemStack item) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(item);
        GenericSpecialGogglesItem itemInstance = (GenericSpecialGogglesItem)item.getItem();
        if (itemInstance.configs == null || itemInstance.configs.length == 0) {
            tag.putInt("configIndex", 0);
            ItemNBTUtil.setTag(item, tag);
            return;
        }
        if (tag.contains("configIndex")) {
            int configIndex = tag.getInt("configIndex");
            if ((configIndex = Math.max(0, Math.min(configIndex, itemInstance.configs.length - 1))) > 0) {
                tag.putInt("configIndex", configIndex - 1);
            }
            ItemNBTUtil.setTag(item, tag);
            return;
        }
        int safeDefaultIndex = Math.min(defaultConfigIndex, itemInstance.configs.length - 1);
        tag.putInt("configIndex", safeDefaultIndex);
        ItemNBTUtil.setTag(item, tag);
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, 1, state -> {
            Entity entity = (Entity)state.getData(DataTickets.ENTITY);
            if (entity == null || !entity.level().isClientSide()) {
                return PlayState.STOP;
            }
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if (CuriosUtil.hasNVGEquipped(player)) {
                    ItemStack stack = CuriosUtil.getFaceWearItem(player);
                    if (stack.getItem() instanceof GenericSpecialGogglesItem) {
                        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
                        if (tag.contains("NvgOnFace")) {
                            if (tag.getBoolean("NvgOnFace")) {
                                if (!state.isCurrentAnimation(ANIM_CLOSE)) {
                                    state.setAnimation(ANIM_CLOSE);
                                }
                            } else if (!state.isCurrentAnimation(ANIM_OPEN)) {
                                state.setAnimation(ANIM_OPEN);
                            }
                        } else {
                            state.setAnimation(ANIM_IDLE);
                        }
                    }
                } else {
                    state.setAnimation(ANIM_IDLE);
                }
            }
            return PlayState.CONTINUE;
        })});
    }

    private void calculateBrightnessRange() {
        if (this.configs == null || this.configs.length == 0) {
            return;
        }
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (NVGConfig config : this.configs) {
            float brightness = config.getBrightness();
            if (brightness < min) {
                min = brightness;
            }
            if (!(brightness > max)) continue;
            max = brightness;
        }
        for (NVGConfig config : this.configs) {
            config.setMinGain(min);
            config.setMaxGain(max);
        }
    }

    public void curioTick(SlotContext slotContext, ItemStack stack) {
        Player player;
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        if (entity.level().isClientSide()) {
            this.updateNVGMode(stack);
        }
        if (entity instanceof Player && !(player = (Player)entity).level().isClientSide()) {
            TagKey<Item> requiredTag;
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            TagKey<Item> tagKey = requiredTag = this.getGoggleType() == GoggleType.VISOR ? HAS_VISOR_MOUNT_TAG : HAS_HEAD_MOUNT_TAG;
            if (helmet.isEmpty() || !helmet.is(requiredTag)) {
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
        ItemStack headItem = player.getItemBySlot(EquipmentSlot.HEAD);
        TagKey<Item> requiredTag = this.getGoggleType() == GoggleType.VISOR ? HAS_VISOR_MOUNT_TAG : HAS_HEAD_MOUNT_TAG;
        return !headItem.isEmpty() && headItem.is(requiredTag);
    }

    public static boolean hasConfigIndexChanged(Player player, ItemStack stack) {
        UUID playerId;
        int lastIndex;
        if (!(stack.getItem() instanceof GenericSpecialGogglesItem)) {
            return false;
        }
        CompoundTag tag = ItemNBTUtil.getTag(stack);
        if (tag == null || !tag.contains("configIndex")) {
            return false;
        }
        int currentIndex = tag.getInt("configIndex");
        if (currentIndex != (lastIndex = lastConfigIndexMap.getOrDefault(playerId = player.getUUID(), -1).intValue())) {
            lastConfigIndexMap.put(playerId, currentIndex);
            return true;
        }
        return false;
    }

    public static void switchAutoGain(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
        if (tag.contains("AutoGainEnabled")) {
            boolean currentStatus = tag.getBoolean("AutoGainEnabled");
            tag.putBoolean("AutoGainEnabled", !currentStatus);
        } else {
            tag.putBoolean("AutoGainEnabled", true);
        }
        ItemNBTUtil.setTag(stack, tag);
    }

    public static boolean isAutoGainEnabled(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getTag(stack);
        if (tag != null && tag.contains("AutoGainEnabled")) {
            return tag.getBoolean("AutoGainEnabled");
        }
        return false;
    }

    public static void switchCotiMode(ItemStack stack) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
        if (tag.contains("Coti")) {
            boolean current = tag.getBoolean("Coti");
            tag.putBoolean("Coti", !current);
        } else {
            tag.putBoolean("Coti", true);
        }
        ItemNBTUtil.setTag(stack, tag);
    }

    public static boolean isCotiEnabled(ItemStack stack) {
        return GenericSpecialGogglesItem.hasCoti(stack) && ItemNBTUtil.getTag(stack) != null && ItemNBTUtil.getTag(stack).getBoolean("Coti");
    }

    public static enum GoggleType {
        NIGHT_VISION,
        THERMAL,
        VISOR;

    }

    public static class NVGConfig {
        private Float brightness = Float.valueOf(1.0f);
        private Float redValue = Float.valueOf(1.0f);
        private Float greenValue = Float.valueOf(1.0f);
        private Float blueValue = Float.valueOf(1.0f);
        private ResourceLocation overlay = null;
        private Float minGain = Float.valueOf(0.1f);
        private Float maxGain = Float.valueOf(1.0f);
        private Float noiseMultiplier = Float.valueOf(1.0f);
        private Float autoGainSpeed = Float.valueOf(0.05f);
        private Float autoGainOffset = Float.valueOf(0.0f);
        private Float autoGatingOffset = Float.valueOf(0.1f);
        private Float autoGatingSpeed = Float.valueOf(0.1f);

        public NVGConfig(float pBrightness, float pRed, float pGreen, float pBlue) {
            this.brightness = Float.valueOf(pBrightness);
            this.redValue = Float.valueOf(pRed);
            this.greenValue = Float.valueOf(pGreen);
            this.blueValue = Float.valueOf(pBlue);
        }

        public NVGConfig(float pBrightness, float pRed, float pGreen, float pBlue, String pOverlay) {
            this.brightness = Float.valueOf(pBrightness);
            this.redValue = Float.valueOf(pRed);
            this.greenValue = Float.valueOf(pGreen);
            this.blueValue = Float.valueOf(pBlue);
            this.overlay = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)pOverlay);
        }

        public NVGConfig(float pBrightness, float pRed, float pGreen, float pBlue, String pOverlay, float pNoiseMultiplier) {
            this.brightness = Float.valueOf(pBrightness);
            this.redValue = Float.valueOf(pRed);
            this.greenValue = Float.valueOf(pGreen);
            this.blueValue = Float.valueOf(pBlue);
            this.overlay = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)pOverlay);
            this.noiseMultiplier = Float.valueOf(pNoiseMultiplier);
        }

        public NVGConfig(float pBrightness, float pRed, float pGreen, float pBlue, String pOverlay, float pNoiseMultiplier, float pAutoGainSpeed) {
            this.brightness = Float.valueOf(pBrightness);
            this.redValue = Float.valueOf(pRed);
            this.greenValue = Float.valueOf(pGreen);
            this.blueValue = Float.valueOf(pBlue);
            this.overlay = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)pOverlay);
            this.noiseMultiplier = Float.valueOf(pNoiseMultiplier);
            this.autoGainSpeed = Float.valueOf(pAutoGainSpeed);
        }

        public NVGConfig(float pBrightness, float pRed, float pGreen, float pBlue, String pOverlay, float pNoiseMultiplier, float pAutoGainSpeed, float pAutoGatingOffset, float pAutoGatingSpeed) {
            this.brightness = Float.valueOf(pBrightness);
            this.redValue = Float.valueOf(pRed);
            this.greenValue = Float.valueOf(pGreen);
            this.blueValue = Float.valueOf(pBlue);
            this.overlay = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)pOverlay);
            this.noiseMultiplier = Float.valueOf(pNoiseMultiplier);
            this.autoGainSpeed = Float.valueOf(pAutoGainSpeed);
            this.autoGatingOffset = Float.valueOf(pAutoGatingOffset);
            this.autoGatingSpeed = Float.valueOf(pAutoGatingSpeed);
        }

        public NVGConfig(float pBrightness, float pRed, float pGreen, float pBlue, String pOverlay, float pNoiseMultiplier, float pAutoGainSpeed, float pAutoGainOffset, float pAutoGatingOffset, float pAutoGatingSpeed) {
            this.brightness = Float.valueOf(pBrightness);
            this.redValue = Float.valueOf(pRed);
            this.greenValue = Float.valueOf(pGreen);
            this.blueValue = Float.valueOf(pBlue);
            this.overlay = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)pOverlay);
            this.noiseMultiplier = Float.valueOf(pNoiseMultiplier);
            this.autoGainSpeed = Float.valueOf(pAutoGainSpeed);
            this.autoGainOffset = Float.valueOf(pAutoGainOffset);
            this.autoGatingOffset = Float.valueOf(pAutoGatingOffset);
            this.autoGatingSpeed = Float.valueOf(pAutoGatingSpeed);
        }

        public NVGConfig(String pOverlay) {
            this.overlay = ResourceLocation.fromNamespaceAndPath((String)"mm", (String)pOverlay);
        }

        public float getBrightness() {
            return this.brightness.floatValue();
        }

        public float getRedValue() {
            return this.redValue.floatValue();
        }

        public float getGreenValue() {
            return this.greenValue.floatValue();
        }

        public float getBlueValue() {
            return this.blueValue.floatValue();
        }

        public ResourceLocation getOverlay() {
            return this.overlay;
        }

        public void setMinGain(float min) {
            this.minGain = Float.valueOf(min);
        }

        public void setMaxGain(float max) {
            this.maxGain = Float.valueOf(max);
        }

        public float getMinGain() {
            return this.minGain.floatValue() - this.autoGainOffset.floatValue();
        }

        public float getMaxGain() {
            return this.maxGain.floatValue() + this.autoGatingOffset.floatValue();
        }

        public float getNoiseMultiplier() {
            return this.noiseMultiplier.floatValue();
        }

        public float getAutoGainSpeed() {
            return this.autoGainSpeed.floatValue();
        }

        public float getAutoGatingOffset() {
            return this.autoGatingOffset.floatValue();
        }

        public float getAutoGatingSpeed() {
            return this.autoGatingSpeed.floatValue();
        }
    }
}

