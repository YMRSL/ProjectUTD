package io.github.ymrsl.firstpersonfoodeating.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

public final class FoodStackData {
    private static final String ROOT_KEY = "firstpersonfoodeating_profile";
    private static final String FOOD_ID_KEY = "food_id";
    private static final String USE_DURATION_KEY = "use_duration_ticks";
    private static final String NUTRITION_KEY = "nutrition";
    private static final String SATURATION_KEY = "saturation";
    private static final String MAX_STACK_SIZE_KEY = "max_stack_size";
    private static final String EFFECTS_KEY = "effects";
    private static final String CUSTOM_EFFECTS_KEY = "custom_effects";
    private static final String THIRST_KEY = "thirst";
    private static final String FLAVOR_MESSAGES_KEY = "flavor_messages";
    private static final String USE_SELECTOR_KEY = "use_selector";
    private static final String DURABILITY_USE_KEY = "durability_use";
    private static final String ACTIVE_USE_PLAN_KEY = "active_use_plan";

    private static final String EFFECT_ID_KEY = "id";
    private static final String EFFECT_DURATION_KEY = "duration_ticks";
    private static final String EFFECT_AMPLIFIER_KEY = "amplifier";
    private static final String EFFECT_CHANCE_KEY = "chance";
    private static final String EFFECT_AMBIENT_KEY = "ambient";
    private static final String EFFECT_SHOW_PARTICLES_KEY = "show_particles";
    private static final String EFFECT_SHOW_ICON_KEY = "show_icon";
    private static final String CUSTOM_EFFECT_TYPE_KEY = "type";
    private static final String CUSTOM_EFFECT_VALUE_KEY = "value";
    private static final String CUSTOM_EFFECT_DURATION_KEY = "duration_ticks";
    private static final String CUSTOM_EFFECT_INTERVAL_KEY = "interval_ticks";
    private static final String CUSTOM_EFFECT_CHANCE_KEY = "chance";

    private static final String THIRST_DELTA_KEY = "delta";
    private static final String THIRST_WATER_DELTA_KEY = "water_delta";
    private static final String THIRST_MODE_KEY = "mode";
    private static final String THIRST_COMPAT_KEEP_KEY = "compat_keep";

    private static final String FLAVOR_MODE_KEY = "mode";
    private static final String FLAVOR_PICK_KEY = "pick";
    private static final String FLAVOR_COOLDOWN_KEY = "cooldown_ticks";
    private static final String FLAVOR_GROUPS_KEY = "groups";
    private static final String FLAVOR_GROUP_WEIGHT_KEY = "weight";
    private static final String FLAVOR_SEGMENTS_KEY = "segments";
    private static final String FLAVOR_SEGMENT_AT_TICKS_KEY = "at_ticks";
    private static final String FLAVOR_SEGMENT_TEXT_KEY = "text";
    private static final String FLAVOR_SEGMENT_LANG_KEY = "lang_key";

    private static final String SELECTOR_MODE_KEY = "mode";
    private static final String SELECTOR_DEFAULT_CLIP_KEY = "default_clip";
    private static final String SELECTOR_RULES_KEY = "rules";
    private static final String SELECTOR_RULE_MIN_KEY = "min_amount";
    private static final String SELECTOR_RULE_MAX_KEY = "max_amount";
    private static final String SELECTOR_RULE_CLIP_KEY = "clip";
    private static final String SELECTOR_RULE_DURATION_KEY = "duration_ticks";

    private static final String DURABILITY_ENABLED_KEY = "enabled";
    private static final String DURABILITY_RESOURCE_KEY = "resource";
    private static final String DURABILITY_MAX_KEY = "max";
    private static final String DURABILITY_CURRENT_KEY = "current";
    private static final String DURABILITY_PER_POINT_KEY = "per_point";

    private static final String ACTIVE_USE_CLIP_KEY = "clip";
    private static final String ACTIVE_USE_DURATION_KEY = "duration_ticks";
    private static final String ACTIVE_USE_AMOUNT_KEY = "amount";
    private static final String ACTIVE_USE_RESOURCE_KEY = "resource";

    private FoodStackData() {
    }

    public static void applyProfile(
            ItemStack stack,
            ResourceLocation foodId,
            int useDurationTicks,
            int nutrition,
            float saturation,
            int maxStackSize
    ) {
        if (stack.isEmpty() || foodId == null) {
            return;
        }
        mutateRoot(stack, root -> {
            root.putString(FOOD_ID_KEY, foodId.toString());
            root.putInt(USE_DURATION_KEY, Mth.clamp(useDurationTicks, 1, 72_000));
            root.putInt(NUTRITION_KEY, Math.max(nutrition, 0));
            root.putFloat(SATURATION_KEY, Math.max(saturation, 0.0f));
            root.putInt(MAX_STACK_SIZE_KEY, Mth.clamp(maxStackSize, 1, 64));
        });
    }

    public static void setEffects(ItemStack stack, List<FoodEffect> effects) {
        if (stack.isEmpty()) {
            return;
        }
        mutateRoot(stack, root -> {
            if (effects == null || effects.isEmpty()) {
                root.remove(EFFECTS_KEY);
                return;
            }
            ListTag serialized = new ListTag();
            for (FoodEffect effect : effects) {
                if (effect == null || effect.effectId() == null) {
                    continue;
                }
                CompoundTag effectTag = new CompoundTag();
                effectTag.putString(EFFECT_ID_KEY, effect.effectId().toString());
                effectTag.putInt(EFFECT_DURATION_KEY, Mth.clamp(effect.durationTicks(), 1, 72_000));
                effectTag.putInt(EFFECT_AMPLIFIER_KEY, Math.max(effect.amplifier(), 0));
                effectTag.putFloat(EFFECT_CHANCE_KEY, Mth.clamp(effect.chance(), 0.0f, 1.0f));
                effectTag.putBoolean(EFFECT_AMBIENT_KEY, effect.ambient());
                effectTag.putBoolean(EFFECT_SHOW_PARTICLES_KEY, effect.showParticles());
                effectTag.putBoolean(EFFECT_SHOW_ICON_KEY, effect.showIcon());
                serialized.add(effectTag);
            }
            if (serialized.isEmpty()) {
                root.remove(EFFECTS_KEY);
            } else {
                root.put(EFFECTS_KEY, serialized);
            }
        });
    }

    public static List<FoodEffect> getEffects(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(EFFECTS_KEY, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag serialized = root.getList(EFFECTS_KEY, Tag.TAG_COMPOUND);
        if (serialized.isEmpty()) {
            return List.of();
        }
        List<FoodEffect> effects = new ArrayList<>();
        for (int i = 0; i < serialized.size(); i++) {
            CompoundTag effectTag = serialized.getCompound(i);
            if (!effectTag.contains(EFFECT_ID_KEY, Tag.TAG_STRING)) {
                continue;
            }
            ResourceLocation id = ResourceLocation.tryParse(effectTag.getString(EFFECT_ID_KEY));
            if (id == null) {
                continue;
            }
            int durationTicks = Mth.clamp(effectTag.getInt(EFFECT_DURATION_KEY), 1, 72_000);
            int amplifier = Math.max(effectTag.getInt(EFFECT_AMPLIFIER_KEY), 0);
            float chance = effectTag.contains(EFFECT_CHANCE_KEY, Tag.TAG_FLOAT)
                    ? Mth.clamp(effectTag.getFloat(EFFECT_CHANCE_KEY), 0.0f, 1.0f)
                    : 1.0f;
            boolean ambient = effectTag.getBoolean(EFFECT_AMBIENT_KEY);
            boolean showParticles = !effectTag.contains(EFFECT_SHOW_PARTICLES_KEY, Tag.TAG_BYTE)
                    || effectTag.getBoolean(EFFECT_SHOW_PARTICLES_KEY);
            boolean showIcon = !effectTag.contains(EFFECT_SHOW_ICON_KEY, Tag.TAG_BYTE)
                    || effectTag.getBoolean(EFFECT_SHOW_ICON_KEY);
            effects.add(new FoodEffect(id, durationTicks, amplifier, chance, ambient, showParticles, showIcon));
        }
        return effects.isEmpty() ? List.of() : List.copyOf(effects);
    }

    public static void setCustomEffects(ItemStack stack, List<CustomEffect> customEffects) {
        if (stack.isEmpty()) {
            return;
        }
        mutateRoot(stack, root -> {
            if (customEffects == null || customEffects.isEmpty()) {
                root.remove(CUSTOM_EFFECTS_KEY);
                return;
            }
            ListTag serialized = new ListTag();
            for (CustomEffect customEffect : customEffects) {
                if (customEffect == null || isBlank(customEffect.type())) {
                    continue;
                }
                CompoundTag effectTag = new CompoundTag();
                effectTag.putString(CUSTOM_EFFECT_TYPE_KEY, customEffect.type());
                effectTag.putFloat(CUSTOM_EFFECT_VALUE_KEY, customEffect.value());
                effectTag.putInt(CUSTOM_EFFECT_DURATION_KEY, Mth.clamp(customEffect.durationTicks(), 0, 72_000));
                effectTag.putInt(CUSTOM_EFFECT_INTERVAL_KEY, Mth.clamp(customEffect.intervalTicks(), 0, 72_000));
                effectTag.putFloat(CUSTOM_EFFECT_CHANCE_KEY, Mth.clamp(customEffect.chance(), 0.0f, 1.0f));
                serialized.add(effectTag);
            }
            if (serialized.isEmpty()) {
                root.remove(CUSTOM_EFFECTS_KEY);
                return;
            }
            root.put(CUSTOM_EFFECTS_KEY, serialized);
        });
    }

    public static List<CustomEffect> getCustomEffects(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(CUSTOM_EFFECTS_KEY, Tag.TAG_LIST)) {
            return List.of();
        }
        ListTag serialized = root.getList(CUSTOM_EFFECTS_KEY, Tag.TAG_COMPOUND);
        if (serialized.isEmpty()) {
            return List.of();
        }
        List<CustomEffect> result = new ArrayList<>();
        for (int i = 0; i < serialized.size(); i++) {
            CompoundTag effectTag = serialized.getCompound(i);
            if (!effectTag.contains(CUSTOM_EFFECT_TYPE_KEY, Tag.TAG_STRING)) {
                continue;
            }
            String type = effectTag.getString(CUSTOM_EFFECT_TYPE_KEY).trim();
            if (type.isEmpty()) {
                continue;
            }
            float value = effectTag.contains(CUSTOM_EFFECT_VALUE_KEY, Tag.TAG_FLOAT)
                    ? effectTag.getFloat(CUSTOM_EFFECT_VALUE_KEY)
                    : 0.0f;
            int durationTicks = effectTag.contains(CUSTOM_EFFECT_DURATION_KEY, Tag.TAG_INT)
                    ? Mth.clamp(effectTag.getInt(CUSTOM_EFFECT_DURATION_KEY), 0, 72_000)
                    : 0;
            int intervalTicks = effectTag.contains(CUSTOM_EFFECT_INTERVAL_KEY, Tag.TAG_INT)
                    ? Mth.clamp(effectTag.getInt(CUSTOM_EFFECT_INTERVAL_KEY), 0, 72_000)
                    : 0;
            float chance = effectTag.contains(CUSTOM_EFFECT_CHANCE_KEY, Tag.TAG_FLOAT)
                    ? Mth.clamp(effectTag.getFloat(CUSTOM_EFFECT_CHANCE_KEY), 0.0f, 1.0f)
                    : 1.0f;
            result.add(new CustomEffect(type, value, durationTicks, intervalTicks, chance));
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    public static void setThirstSpec(ItemStack stack, @Nullable ThirstSpec spec) {
        if (stack.isEmpty()) {
            return;
        }
        mutateRoot(stack, root -> {
            if (spec == null || (spec.thirstDelta() == 0 && spec.waterDelta() == 0)) {
                root.remove(THIRST_KEY);
                return;
            }
            CompoundTag thirstTag = new CompoundTag();
            thirstTag.putInt(THIRST_DELTA_KEY, Mth.clamp(spec.thirstDelta(), -100, 100));
            thirstTag.putInt(THIRST_WATER_DELTA_KEY, Mth.clamp(spec.waterDelta(), -100, 100));
            thirstTag.putString(THIRST_MODE_KEY, spec.mode().serializedName());
            ListTag keepListTag = new ListTag();
            for (EffectChannel channel : spec.compatKeepChannels()) {
                if (channel == null) {
                    continue;
                }
                keepListTag.add(net.minecraft.nbt.StringTag.valueOf(channel.serializedName()));
            }
            if (!keepListTag.isEmpty()) {
                thirstTag.put(THIRST_COMPAT_KEEP_KEY, keepListTag);
            }
            root.put(THIRST_KEY, thirstTag);
        });
    }

    public static Optional<ThirstSpec> getThirstSpec(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(THIRST_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag thirstTag = root.getCompound(THIRST_KEY);
        if (!thirstTag.contains(THIRST_DELTA_KEY, Tag.TAG_INT)
                && !thirstTag.contains(THIRST_WATER_DELTA_KEY, Tag.TAG_INT)) {
            return Optional.empty();
        }
        int thirstDelta = thirstTag.contains(THIRST_DELTA_KEY, Tag.TAG_INT)
                ? Mth.clamp(thirstTag.getInt(THIRST_DELTA_KEY), -100, 100)
                : 0;
        int waterDelta = thirstTag.contains(THIRST_WATER_DELTA_KEY, Tag.TAG_INT)
                ? Mth.clamp(thirstTag.getInt(THIRST_WATER_DELTA_KEY), -100, 100)
                : 0;
        if (thirstDelta == 0 && waterDelta == 0) {
            return Optional.empty();
        }
        ThirstMode mode = ThirstMode.fromString(thirstTag.getString(THIRST_MODE_KEY));
        List<EffectChannel> keepChannels = new ArrayList<>();
        if (thirstTag.contains(THIRST_COMPAT_KEEP_KEY, Tag.TAG_LIST)) {
            ListTag keepListTag = thirstTag.getList(THIRST_COMPAT_KEEP_KEY, Tag.TAG_STRING);
            for (int i = 0; i < keepListTag.size(); i++) {
                EffectChannel channel = EffectChannel.fromString(keepListTag.getString(i));
                if (channel != null && !keepChannels.contains(channel)) {
                    keepChannels.add(channel);
                }
            }
        }
        return Optional.of(new ThirstSpec(thirstDelta, waterDelta, mode, keepChannels));
    }

    public static boolean shouldApplyChannel(ItemStack stack, boolean thirstLinked, EffectChannel channel) {
        if (!thirstLinked || channel == null) {
            return true;
        }
        ThirstSpec spec = getThirstSpec(stack).orElse(null);
        if (spec == null) {
            return true;
        }
        return spec.shouldApply(channel);
    }

    public static void setFlavorMessages(ItemStack stack, @Nullable FlavorMessageSpec spec) {
        if (stack.isEmpty()) {
            return;
        }
        mutateRoot(stack, root -> {
            if (spec == null || spec.groups().isEmpty()) {
                root.remove(FLAVOR_MESSAGES_KEY);
                return;
            }
            CompoundTag flavorTag = new CompoundTag();
            flavorTag.putString(FLAVOR_MODE_KEY, spec.mode().serializedName());
            flavorTag.putString(FLAVOR_PICK_KEY, spec.pickMode());
            flavorTag.putInt(FLAVOR_COOLDOWN_KEY, Mth.clamp(spec.cooldownTicks(), 0, 72_000));

            ListTag groupsTag = new ListTag();
            for (FlavorGroup group : spec.groups()) {
                if (group == null || group.segments().isEmpty()) {
                    continue;
                }
                CompoundTag groupTag = new CompoundTag();
                groupTag.putInt(FLAVOR_GROUP_WEIGHT_KEY, Math.max(group.weight(), 1));

                ListTag segmentsTag = new ListTag();
                for (FlavorSegment segment : group.segments()) {
                    if (segment == null) {
                        continue;
                    }
                    String text = segment.text() == null ? "" : segment.text().trim();
                    String langKey = segment.langKey() == null ? "" : segment.langKey().trim();
                    if (text.isEmpty() && langKey.isEmpty()) {
                        continue;
                    }
                    CompoundTag segmentTag = new CompoundTag();
                    segmentTag.putInt(FLAVOR_SEGMENT_AT_TICKS_KEY, Math.max(segment.atTicks(), 0));
                    if (!text.isEmpty()) {
                        segmentTag.putString(FLAVOR_SEGMENT_TEXT_KEY, text);
                    }
                    if (!langKey.isEmpty()) {
                        segmentTag.putString(FLAVOR_SEGMENT_LANG_KEY, langKey);
                    }
                    segmentsTag.add(segmentTag);
                }
                if (segmentsTag.isEmpty()) {
                    continue;
                }
                groupTag.put(FLAVOR_SEGMENTS_KEY, segmentsTag);
                groupsTag.add(groupTag);
            }

            if (groupsTag.isEmpty()) {
                root.remove(FLAVOR_MESSAGES_KEY);
                return;
            }
            flavorTag.put(FLAVOR_GROUPS_KEY, groupsTag);
            root.put(FLAVOR_MESSAGES_KEY, flavorTag);
        });
    }

    public static Optional<FlavorMessageSpec> getFlavorMessages(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(FLAVOR_MESSAGES_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag flavorTag = root.getCompound(FLAVOR_MESSAGES_KEY);
        String modeRaw = flavorTag.getString(FLAVOR_MODE_KEY);
        MessageMode mode = MessageMode.fromString(modeRaw);
        String pickMode = flavorTag.contains(FLAVOR_PICK_KEY, Tag.TAG_STRING)
                ? flavorTag.getString(FLAVOR_PICK_KEY)
                : "random_weighted";
        int cooldownTicks = flavorTag.contains(FLAVOR_COOLDOWN_KEY, Tag.TAG_INT)
                ? Mth.clamp(flavorTag.getInt(FLAVOR_COOLDOWN_KEY), 0, 72_000)
                : 0;
        if (!flavorTag.contains(FLAVOR_GROUPS_KEY, Tag.TAG_LIST)) {
            return Optional.empty();
        }
        ListTag groupsTag = flavorTag.getList(FLAVOR_GROUPS_KEY, Tag.TAG_COMPOUND);
        List<FlavorGroup> groups = new ArrayList<>();
        for (int i = 0; i < groupsTag.size(); i++) {
            CompoundTag groupTag = groupsTag.getCompound(i);
            int weight = Math.max(groupTag.getInt(FLAVOR_GROUP_WEIGHT_KEY), 1);
            if (!groupTag.contains(FLAVOR_SEGMENTS_KEY, Tag.TAG_LIST)) {
                continue;
            }
            ListTag segmentsTag = groupTag.getList(FLAVOR_SEGMENTS_KEY, Tag.TAG_COMPOUND);
            List<FlavorSegment> segments = new ArrayList<>();
            for (int j = 0; j < segmentsTag.size(); j++) {
                CompoundTag segmentTag = segmentsTag.getCompound(j);
                int atTicks = Math.max(segmentTag.getInt(FLAVOR_SEGMENT_AT_TICKS_KEY), 0);
                String text = segmentTag.contains(FLAVOR_SEGMENT_TEXT_KEY, Tag.TAG_STRING)
                        ? segmentTag.getString(FLAVOR_SEGMENT_TEXT_KEY).trim()
                        : null;
                String langKey = segmentTag.contains(FLAVOR_SEGMENT_LANG_KEY, Tag.TAG_STRING)
                        ? segmentTag.getString(FLAVOR_SEGMENT_LANG_KEY).trim()
                        : null;
                if ((text == null || text.isEmpty()) && (langKey == null || langKey.isEmpty())) {
                    continue;
                }
                segments.add(new FlavorSegment(atTicks, isBlank(text) ? null : text, isBlank(langKey) ? null : langKey));
            }
            if (segments.isEmpty()) {
                continue;
            }
            segments.sort(Comparator.comparingInt(FlavorSegment::atTicks));
            groups.add(new FlavorGroup(weight, segments));
        }
        if (groups.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FlavorMessageSpec(mode, pickMode, cooldownTicks, groups));
    }

    public static Optional<FlavorMessagePlan> selectFlavorMessagePlan(ItemStack stack, RandomSource random) {
        return getFlavorMessages(stack).flatMap(spec -> selectFlavorMessagePlan(spec, random));
    }

    public static Optional<FlavorMessagePlan> selectFlavorMessagePlan(FlavorMessageSpec spec, RandomSource random) {
        if (spec == null || spec.groups().isEmpty()) {
            return Optional.empty();
        }
        List<FlavorGroup> available = new ArrayList<>();
        for (FlavorGroup group : spec.groups()) {
            if (group == null || group.segments().isEmpty()) {
                continue;
            }
            available.add(group);
        }
        if (available.isEmpty()) {
            return Optional.empty();
        }

        FlavorGroup selected;
        String pick = spec.pickMode().toLowerCase(Locale.ROOT);
        if ("first".equals(pick)) {
            selected = available.get(0);
        } else {
            int totalWeight = 0;
            for (FlavorGroup group : available) {
                totalWeight += Math.max(group.weight(), 1);
            }
            if (totalWeight <= 0) {
                selected = available.get(0);
            } else {
                int roll = random.nextInt(totalWeight);
                int acc = 0;
                selected = available.get(0);
                for (FlavorGroup group : available) {
                    acc += Math.max(group.weight(), 1);
                    if (roll < acc) {
                        selected = group;
                        break;
                    }
                }
            }
        }
        List<FlavorSegment> segments = new ArrayList<>(selected.segments());
        segments.sort(Comparator.comparingInt(FlavorSegment::atTicks));
        if (segments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new FlavorMessagePlan(spec.mode(), spec.cooldownTicks(), segments));
    }

    public static void setUseSelectorSpec(ItemStack stack, @Nullable UseSelectorSpec spec) {
        if (stack.isEmpty()) {
            return;
        }
        mutateRoot(stack, root -> {
            if (spec == null || spec.rules().isEmpty()) {
                root.remove(USE_SELECTOR_KEY);
                return;
            }
            CompoundTag selectorTag = new CompoundTag();
            selectorTag.putString(SELECTOR_MODE_KEY, spec.mode().serializedName());
            selectorTag.putString(SELECTOR_DEFAULT_CLIP_KEY, spec.defaultClip());

            ListTag rulesTag = new ListTag();
            for (UseSelectorRule rule : spec.rules()) {
                if (rule == null || isBlank(rule.clipName())) {
                    continue;
                }
                CompoundTag ruleTag = new CompoundTag();
                ruleTag.putInt(SELECTOR_RULE_MIN_KEY, Math.max(rule.minAmount(), 0));
                ruleTag.putInt(SELECTOR_RULE_MAX_KEY, Math.max(rule.maxAmount(), rule.minAmount()));
                ruleTag.putString(SELECTOR_RULE_CLIP_KEY, rule.clipName());
                ruleTag.putInt(SELECTOR_RULE_DURATION_KEY, Mth.clamp(rule.durationTicks(), 1, 72_000));
                rulesTag.add(ruleTag);
            }
            if (rulesTag.isEmpty()) {
                root.remove(USE_SELECTOR_KEY);
                return;
            }
            selectorTag.put(SELECTOR_RULES_KEY, rulesTag);
            root.put(USE_SELECTOR_KEY, selectorTag);
        });
    }

    public static Optional<UseSelectorSpec> getUseSelectorSpec(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(USE_SELECTOR_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag selectorTag = root.getCompound(USE_SELECTOR_KEY);
        UseSelectorMode mode = UseSelectorMode.fromString(selectorTag.getString(SELECTOR_MODE_KEY));
        String defaultClip = selectorTag.contains(SELECTOR_DEFAULT_CLIP_KEY, Tag.TAG_STRING)
                ? selectorTag.getString(SELECTOR_DEFAULT_CLIP_KEY).trim()
                : "use";
        if (defaultClip.isEmpty()) {
            defaultClip = "use";
        }
        if (!selectorTag.contains(SELECTOR_RULES_KEY, Tag.TAG_LIST)) {
            return Optional.empty();
        }
        ListTag rulesTag = selectorTag.getList(SELECTOR_RULES_KEY, Tag.TAG_COMPOUND);
        List<UseSelectorRule> rules = new ArrayList<>();
        for (int i = 0; i < rulesTag.size(); i++) {
            CompoundTag ruleTag = rulesTag.getCompound(i);
            if (!ruleTag.contains(SELECTOR_RULE_CLIP_KEY, Tag.TAG_STRING)) {
                continue;
            }
            String clip = ruleTag.getString(SELECTOR_RULE_CLIP_KEY).trim();
            if (clip.isEmpty()) {
                continue;
            }
            int minAmount = Math.max(ruleTag.getInt(SELECTOR_RULE_MIN_KEY), 0);
            int maxAmount = Math.max(ruleTag.getInt(SELECTOR_RULE_MAX_KEY), minAmount);
            int durationTicks = Mth.clamp(ruleTag.getInt(SELECTOR_RULE_DURATION_KEY), 1, 72_000);
            rules.add(new UseSelectorRule(minAmount, maxAmount, clip, durationTicks));
        }
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new UseSelectorSpec(mode, defaultClip, rules));
    }

    public static void setDurabilityUseSpec(ItemStack stack, @Nullable DurabilityUseSpec spec) {
        if (stack.isEmpty()) {
            return;
        }
        mutateRoot(stack, root -> {
            if (spec == null || !spec.enabled()) {
                root.remove(DURABILITY_USE_KEY);
                return;
            }
            CompoundTag durabilityTag = new CompoundTag();
            durabilityTag.putBoolean(DURABILITY_ENABLED_KEY, true);
            durabilityTag.putString(DURABILITY_RESOURCE_KEY, spec.resourceType().serializedName());
            durabilityTag.putInt(DURABILITY_MAX_KEY, Math.max(spec.maxDurability(), 1));
            durabilityTag.putInt(DURABILITY_CURRENT_KEY, Mth.clamp(spec.currentDurability(), 0, Math.max(spec.maxDurability(), 1)));
            durabilityTag.putInt(DURABILITY_PER_POINT_KEY, Math.max(spec.pointsPerUnit(), 1));
            root.put(DURABILITY_USE_KEY, durabilityTag);
        });
    }

    public static Optional<DurabilityUseSpec> getDurabilityUseSpec(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(DURABILITY_USE_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag durabilityTag = root.getCompound(DURABILITY_USE_KEY);
        if (!durabilityTag.getBoolean(DURABILITY_ENABLED_KEY)) {
            return Optional.empty();
        }
        ResourceType resourceType = ResourceType.fromString(durabilityTag.getString(DURABILITY_RESOURCE_KEY));
        int maxDurability = Math.max(durabilityTag.getInt(DURABILITY_MAX_KEY), 1);
        int currentDurability = Mth.clamp(durabilityTag.getInt(DURABILITY_CURRENT_KEY), 0, maxDurability);
        int pointsPerUnit = Math.max(durabilityTag.getInt(DURABILITY_PER_POINT_KEY), 1);
        return Optional.of(new DurabilityUseSpec(true, resourceType, maxDurability, currentDurability, pointsPerUnit));
    }

    public static int getDurabilityCurrent(ItemStack stack) {
        return getDurabilityUseSpec(stack).map(DurabilityUseSpec::currentDurability).orElse(0);
    }

    public static int getDurabilityMax(ItemStack stack) {
        return getDurabilityUseSpec(stack).map(DurabilityUseSpec::maxDurability).orElse(0);
    }

    public static boolean isDurabilityUseEnabled(ItemStack stack) {
        return getDurabilityUseSpec(stack).map(DurabilityUseSpec::enabled).orElse(false);
    }

    public static void setDurabilityCurrent(ItemStack stack, int current) {
        mutateRoot(stack, root -> {
            if (!root.contains(DURABILITY_USE_KEY, Tag.TAG_COMPOUND)) {
                return;
            }
            CompoundTag durabilityTag = root.getCompound(DURABILITY_USE_KEY);
            int maxDurability = Math.max(durabilityTag.getInt(DURABILITY_MAX_KEY), 1);
            durabilityTag.putInt(DURABILITY_CURRENT_KEY, Mth.clamp(current, 0, maxDurability));
            root.put(DURABILITY_USE_KEY, durabilityTag);
        });
    }

    public static void setActiveUsePlan(ItemStack stack, @Nullable ActiveUsePlan plan) {
        if (stack.isEmpty()) {
            return;
        }
        mutateRoot(stack, root -> {
            if (plan == null) {
                root.remove(ACTIVE_USE_PLAN_KEY);
                return;
            }
            CompoundTag activeTag = new CompoundTag();
            activeTag.putString(ACTIVE_USE_CLIP_KEY, plan.clipName());
            activeTag.putInt(ACTIVE_USE_DURATION_KEY, Mth.clamp(plan.durationTicks(), 1, 72_000));
            activeTag.putInt(ACTIVE_USE_AMOUNT_KEY, Math.max(plan.consumeAmount(), 0));
            activeTag.putString(ACTIVE_USE_RESOURCE_KEY, plan.resourceType().serializedName());
            root.put(ACTIVE_USE_PLAN_KEY, activeTag);
        });
    }

    public static Optional<ActiveUsePlan> getActiveUsePlan(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(ACTIVE_USE_PLAN_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag activeTag = root.getCompound(ACTIVE_USE_PLAN_KEY);
        if (!activeTag.contains(ACTIVE_USE_CLIP_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        String clip = activeTag.getString(ACTIVE_USE_CLIP_KEY).trim();
        if (clip.isEmpty()) {
            return Optional.empty();
        }
        int durationTicks = Mth.clamp(activeTag.getInt(ACTIVE_USE_DURATION_KEY), 1, 72_000);
        int consumeAmount = Math.max(activeTag.getInt(ACTIVE_USE_AMOUNT_KEY), 0);
        ResourceType resourceType = ResourceType.fromString(activeTag.getString(ACTIVE_USE_RESOURCE_KEY));
        return Optional.of(new ActiveUsePlan(clip, durationTicks, consumeAmount, resourceType));
    }

    public static Optional<String> getActiveUseClipName(ItemStack stack) {
        return getActiveUsePlan(stack).map(ActiveUsePlan::clipName);
    }

    public static void clearActiveUsePlan(ItemStack stack) {
        if (getRoot(stack) == null) {
            return;
        }
        mutateRoot(stack, root -> root.remove(ACTIVE_USE_PLAN_KEY));
    }

    public static Optional<ResourceLocation> getFoodId(ItemStack stack) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(FOOD_ID_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        ResourceLocation id = ResourceLocation.tryParse(root.getString(FOOD_ID_KEY));
        return Optional.ofNullable(id);
    }

    public static ResourceLocation resolveFoodId(ItemStack stack) {
        ResourceLocation tagged = getFoodId(stack).orElse(null);
        if (tagged != null) {
            return tagged;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId == null ? ResourceLocation.withDefaultNamespace("air") : itemId;
    }

    public static int getUseDurationTicks(ItemStack stack, int fallback) {
        ActiveUsePlan activePlan = getActiveUsePlan(stack).orElse(null);
        if (activePlan != null) {
            return Mth.clamp(activePlan.durationTicks(), 1, 72_000);
        }
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(USE_DURATION_KEY, Tag.TAG_INT)) {
            return Math.max(fallback, 1);
        }
        return Mth.clamp(root.getInt(USE_DURATION_KEY), 1, 72_000);
    }

    public static int getNutrition(ItemStack stack, int fallback) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(NUTRITION_KEY, Tag.TAG_INT)) {
            return Math.max(fallback, 0);
        }
        return Math.max(root.getInt(NUTRITION_KEY), 0);
    }

    public static float getSaturation(ItemStack stack, float fallback) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(SATURATION_KEY, Tag.TAG_FLOAT)) {
            return Math.max(fallback, 0.0f);
        }
        return Math.max(root.getFloat(SATURATION_KEY), 0.0f);
    }

    public static int getMaxStackSize(ItemStack stack, int fallback) {
        CompoundTag root = getRoot(stack);
        if (root == null || !root.contains(MAX_STACK_SIZE_KEY, Tag.TAG_INT)) {
            return Mth.clamp(fallback, 1, 64);
        }
        return Mth.clamp(root.getInt(MAX_STACK_SIZE_KEY), 1, 64);
    }

    public static boolean matches(
            ItemStack stack,
            @Nullable String expectedItemId,
            @Nullable String expectedFoodId
    ) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) {
            return false;
        }
        if (expectedItemId != null && !expectedItemId.isBlank() && !expectedItemId.equals(itemId.toString())) {
            return false;
        }
        if (expectedFoodId == null || expectedFoodId.isBlank()) {
            return true;
        }
        return expectedFoodId.equals(resolveFoodId(stack).toString());
    }

    // ---------------------------------------------------------------------------------------------
    // 1.21 storage: ItemStack no longer exposes a mutable NBT tag (getTag/getOrCreateTag are gone).
    // We persist the whole legacy profile CompoundTag inside the CUSTOM_DATA data component. Because
    // CustomData is immutable, every mutation must be a read-modify-write; mutateRoot() encapsulates
    // that so the rest of this class keeps using the original CompoundTag-based serialization logic.
    // ---------------------------------------------------------------------------------------------

    private static CompoundTag readCustomTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    private static void writeCustomTag(ItemStack stack, CompoundTag tag) {
        if (tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /**
     * Mutates the profile root compound in place and writes the result back into CUSTOM_DATA.
     */
    private static void mutateRoot(ItemStack stack, Consumer<CompoundTag> mutator) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag full = readCustomTag(stack);
        CompoundTag root = full.contains(ROOT_KEY, Tag.TAG_COMPOUND)
                ? full.getCompound(ROOT_KEY)
                : new CompoundTag();
        mutator.accept(root);
        if (root.isEmpty()) {
            full.remove(ROOT_KEY);
        } else {
            full.put(ROOT_KEY, root);
        }
        writeCustomTag(stack, full);
    }

    private static @Nullable CompoundTag getRoot(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = readCustomTag(stack);
        if (!tag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return tag.getCompound(ROOT_KEY);
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    public enum MessageMode {
        ACTIONBAR("actionbar"),
        CHAT("chat");

        private final String serializedName;

        MessageMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static MessageMode fromString(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return ACTIONBAR;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if ("chat".equals(normalized)) {
                return CHAT;
            }
            return ACTIONBAR;
        }
    }

    public record FoodEffect(
            ResourceLocation effectId,
            int durationTicks,
            int amplifier,
            float chance,
            boolean ambient,
            boolean showParticles,
            boolean showIcon
    ) {
        public FoodEffect {
            durationTicks = Mth.clamp(durationTicks, 1, 72_000);
            amplifier = Math.max(amplifier, 0);
            chance = Mth.clamp(chance, 0.0f, 1.0f);
        }
    }

    public record FlavorSegment(int atTicks, @Nullable String text, @Nullable String langKey) {
        public FlavorSegment {
            atTicks = Math.max(atTicks, 0);
            text = isBlank(text) ? null : text;
            langKey = isBlank(langKey) ? null : langKey;
        }
    }

    public record FlavorGroup(int weight, List<FlavorSegment> segments) {
        public FlavorGroup {
            weight = Math.max(weight, 1);
            segments = segments == null ? List.of() : List.copyOf(segments);
        }
    }

    public record FlavorMessageSpec(
            MessageMode mode,
            String pickMode,
            int cooldownTicks,
            List<FlavorGroup> groups
    ) {
        public FlavorMessageSpec {
            mode = mode == null ? MessageMode.ACTIONBAR : mode;
            pickMode = (pickMode == null || pickMode.isBlank()) ? "random_weighted" : pickMode.trim().toLowerCase(Locale.ROOT);
            cooldownTicks = Mth.clamp(cooldownTicks, 0, 72_000);
            groups = groups == null ? List.of() : List.copyOf(groups);
        }
    }

    public record FlavorMessagePlan(
            MessageMode mode,
            int cooldownTicks,
            List<FlavorSegment> segments
    ) {
        public FlavorMessagePlan {
            mode = mode == null ? MessageMode.ACTIONBAR : mode;
            cooldownTicks = Mth.clamp(cooldownTicks, 0, 72_000);
            segments = segments == null ? List.of() : List.copyOf(segments);
        }
    }

    public enum ThirstMode {
        ALWAYS("always"),
        ONLY("only"),
        COMPAT("compat");

        private final String serializedName;

        ThirstMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static ThirstMode fromString(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return ALWAYS;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if ("only".equals(normalized) || "thirst_only".equals(normalized)) {
                return ONLY;
            }
            if ("compat".equals(normalized) || "linked_compat".equals(normalized)) {
                return COMPAT;
            }
            return ALWAYS;
        }
    }

    public enum EffectChannel {
        NUTRITION("nutrition"),
        SATURATION("saturation"),
        EFFECTS("effects"),
        CUSTOM_EFFECTS("custom_effects");

        private final String serializedName;

        EffectChannel(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static @Nullable EffectChannel fromString(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            for (EffectChannel channel : values()) {
                if (channel.serializedName.equals(normalized)) {
                    return channel;
                }
            }
            return null;
        }
    }

    public record ThirstSpec(
            int thirstDelta,
            int waterDelta,
            ThirstMode mode,
            List<EffectChannel> compatKeepChannels
    ) {
        public ThirstSpec {
            thirstDelta = Mth.clamp(thirstDelta, -100, 100);
            waterDelta = Mth.clamp(waterDelta, -100, 100);
            mode = mode == null ? ThirstMode.ALWAYS : mode;
            compatKeepChannels = compatKeepChannels == null ? List.of() : List.copyOf(compatKeepChannels);
        }

        public boolean shouldApply(EffectChannel channel) {
            if (channel == null) {
                return true;
            }
            return switch (mode) {
                case ALWAYS -> true;
                case ONLY -> false;
                case COMPAT -> compatKeepChannels.contains(channel);
            };
        }
    }

    public record CustomEffect(
            String type,
            float value,
            int durationTicks,
            int intervalTicks,
            float chance
    ) {
        public CustomEffect {
            type = isBlank(type) ? "" : type.trim().toLowerCase(Locale.ROOT);
            durationTicks = Mth.clamp(durationTicks, 0, 72_000);
            intervalTicks = Mth.clamp(intervalTicks, 0, 72_000);
            chance = Mth.clamp(chance, 0.0f, 1.0f);
        }
    }

    public enum UseSelectorMode {
        FIXED("fixed"),
        HP("hp"),
        ARMOR("armor");

        private final String serializedName;

        UseSelectorMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static UseSelectorMode fromString(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return FIXED;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if ("hp".equals(normalized) || "health".equals(normalized)) {
                return HP;
            }
            if ("armor".equals(normalized) || "repair".equals(normalized)) {
                return ARMOR;
            }
            return FIXED;
        }
    }

    public enum ResourceType {
        NONE("none"),
        HP("hp"),
        ARMOR("armor");

        private final String serializedName;

        ResourceType(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static ResourceType fromString(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return NONE;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if ("hp".equals(normalized) || "health".equals(normalized)) {
                return HP;
            }
            if ("armor".equals(normalized) || "repair".equals(normalized)) {
                return ARMOR;
            }
            return NONE;
        }
    }

    public record UseSelectorRule(
            int minAmount,
            int maxAmount,
            String clipName,
            int durationTicks
    ) {
        public UseSelectorRule {
            minAmount = Math.max(minAmount, 0);
            maxAmount = Math.max(maxAmount, minAmount);
            clipName = isBlank(clipName) ? "use" : clipName.trim();
            durationTicks = Mth.clamp(durationTicks, 1, 72_000);
        }
    }

    public record UseSelectorSpec(
            UseSelectorMode mode,
            String defaultClip,
            List<UseSelectorRule> rules
    ) {
        public UseSelectorSpec {
            mode = mode == null ? UseSelectorMode.FIXED : mode;
            defaultClip = isBlank(defaultClip) ? "use" : defaultClip.trim();
            rules = rules == null ? List.of() : List.copyOf(rules);
        }
    }

    public record DurabilityUseSpec(
            boolean enabled,
            ResourceType resourceType,
            int maxDurability,
            int currentDurability,
            int pointsPerUnit
    ) {
        public DurabilityUseSpec {
            enabled = enabled;
            resourceType = resourceType == null ? ResourceType.NONE : resourceType;
            maxDurability = Math.max(maxDurability, 1);
            currentDurability = Mth.clamp(currentDurability, 0, maxDurability);
            pointsPerUnit = Math.max(pointsPerUnit, 1);
        }
    }

    public record ActiveUsePlan(
            String clipName,
            int durationTicks,
            int consumeAmount,
            ResourceType resourceType
    ) {
        public ActiveUsePlan {
            clipName = isBlank(clipName) ? "use" : clipName.trim();
            durationTicks = Mth.clamp(durationTicks, 1, 72_000);
            consumeAmount = Math.max(consumeAmount, 0);
            resourceType = resourceType == null ? ResourceType.NONE : resourceType;
        }
    }
}
