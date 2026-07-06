package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import com.google.gson.annotations.SerializedName;
import io.github.ymrsl.firstpersonfoodeating.item.FoodStackData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public final class FoodDisplayDefinition {
    @SerializedName("item")
    private ResourceLocation itemId;
    @SerializedName("animation")
    private ResourceLocation animationId;
    @SerializedName("state_machine")
    private ResourceLocation stateMachineId;
    @SerializedName("geo")
    private ResourceLocation geoId;
    @SerializedName("third_geo")
    private ResourceLocation thirdGeoId;
    @SerializedName("texture")
    private ResourceLocation textureId;
    @SerializedName("third_texture")
    private ResourceLocation thirdTextureId;
    @SerializedName("use_sound")
    private ResourceLocation useSoundId;
    @SerializedName("geo_visible_root")
    private String geoVisibleRoot;
    @SerializedName("geo_visible_roots")
    private List<String> geoVisibleRoots;
    @SerializedName("third_geo_visible_root")
    private String thirdGeoVisibleRoot;
    @SerializedName("third_geo_visible_roots")
    private List<String> thirdGeoVisibleRoots;
    @SerializedName("state_machine_param")
    private Map<String, Object> stateMachineParams;
    @SerializedName("use_duration_ticks")
    private Integer useDurationTicks;
    @SerializedName("nutrition")
    private Integer nutrition;
    @SerializedName("saturation")
    private Float saturation;
    @SerializedName("max_stack_size")
    private Integer maxStackSize;
    @SerializedName("use_selector")
    private UseSelectorDefinition useSelector;
    @SerializedName("durability_use")
    private DurabilityUseDefinition durabilityUse;

    @SerializedName("effects")
    private List<EffectDefinition> effects;
    @SerializedName("custom_effects")
    private List<CustomEffectDefinition> customEffects;
    @SerializedName("thirst")
    private ThirstDefinition thirst;
    @SerializedName("tooltip")
    private TooltipDefinition tooltip;
    @SerializedName("flavor_messages")
    private FlavorMessagesDefinition flavorMessages;
    @SerializedName("post_consume_messages")
    private FlavorMessagesDefinition postConsumeMessages;
    @SerializedName("message_mode")
    private String messageMode;

    public ResourceLocation getItemId() {
        return itemId;
    }

    public ResourceLocation getAnimationId() {
        return animationId;
    }

    public ResourceLocation getStateMachineId() {
        return stateMachineId;
    }

    public ResourceLocation getGeoId() {
        return geoId;
    }

    public ResourceLocation getThirdGeoId() {
        return thirdGeoId;
    }

    public ResourceLocation getTextureId() {
        return textureId;
    }

    public ResourceLocation getThirdTextureId() {
        return thirdTextureId;
    }

    public ResourceLocation getUseSoundId() {
        return useSoundId;
    }

    public Map<String, Object> getStateMachineParams() {
        return stateMachineParams;
    }

    public int resolveUseDurationTicks() {
        Integer value = firstNonNull(
                useDurationTicks,
                getIntStateParam("use_duration_ticks"),
                getIntStateParam("use_duration")
        );
        return Mth.clamp(value == null ? 81 : value, 1, 72_000);
    }

    public boolean hasExplicitUseDurationTicks() {
        return useDurationTicks != null
                || getIntStateParam("use_duration_ticks") != null
                || getIntStateParam("use_duration") != null;
    }

    public int resolveNutrition() {
        Integer value = firstNonNull(
                nutrition,
                getIntStateParam("nutrition")
        );
        return Math.max(value == null ? 2 : value, 0);
    }

    public float resolveSaturation() {
        Float value = firstNonNull(
                saturation,
                getFloatStateParam("saturation"),
                getFloatStateParam("saturation_mod")
        );
        return Math.max(value == null ? 0.1f : value, 0.0f);
    }

    public int resolveMaxStackSize() {
        Integer value = firstNonNull(
                maxStackSize,
                getIntStateParam("max_stack_size"),
                getIntStateParam("stack_size")
        );
        return Mth.clamp(value == null ? 16 : value, 1, 64);
    }

    public @Nullable FoodStackData.UseSelectorSpec resolveUseSelectorSpec(int fallbackDurationTicks) {
        if (useSelector == null || useSelector.rules == null || useSelector.rules.isEmpty()) {
            return null;
        }
        FoodStackData.UseSelectorMode mode = FoodStackData.UseSelectorMode.fromString(useSelector.mode);
        String defaultClip = isBlank(useSelector.defaultClip) ? "use" : useSelector.defaultClip.trim();
        List<FoodStackData.UseSelectorRule> rules = new ArrayList<>();
        for (UseSelectorRuleDefinition definition : useSelector.rules) {
            if (definition == null || isBlank(definition.clip)) {
                continue;
            }
            int min = Math.max(firstNonNull(definition.minAmount, definition.min, 0), 0);
            int max = Math.max(firstNonNull(definition.maxAmount, definition.max, Integer.MAX_VALUE), min);
            int duration = Mth.clamp(
                    firstNonNull(definition.durationTicks, definition.duration, fallbackDurationTicks),
                    1,
                    72_000
            );
            rules.add(new FoodStackData.UseSelectorRule(min, max, definition.clip.trim(), duration));
        }
        if (rules.isEmpty()) {
            return null;
        }
        return new FoodStackData.UseSelectorSpec(mode, defaultClip, rules);
    }

    public @Nullable FoodStackData.DurabilityUseSpec resolveDurabilityUseSpec() {
        if (durabilityUse == null || durabilityUse.enabled == null || !durabilityUse.enabled) {
            return null;
        }
        int max = Math.max(firstNonNull(durabilityUse.max, durabilityUse.maxDurability, 0), 1);
        int current = firstNonNull(durabilityUse.current, durabilityUse.currentDurability, max);
        int perPoint = Math.max(firstNonNull(durabilityUse.perPoint, durabilityUse.pointsPerUnit, 1), 1);
        FoodStackData.ResourceType resourceType = FoodStackData.ResourceType.fromString(durabilityUse.resource);
        return new FoodStackData.DurabilityUseSpec(true, resourceType, max, current, perPoint);
    }

    public String getGeoVisibleRoot() {
        return geoVisibleRoot;
    }

    public String getThirdGeoVisibleRoot() {
        return thirdGeoVisibleRoot;
    }

    public ResourceLocation resolveGeoId() {
        return geoId != null ? geoId : animationId;
    }

    public ResourceLocation resolveThirdGeoId() {
        if (thirdGeoId != null) {
            return thirdGeoId;
        }
        return resolveGeoId();
    }

    public ResourceLocation resolveTextureId() {
        ResourceLocation raw;
        if (textureId != null) {
            raw = textureId;
        } else if (animationId == null) {
            raw = ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        } else {
            raw = ResourceLocation.fromNamespaceAndPath(animationId.getNamespace(), "item/" + animationId.getPath());
        }
        return normalizeTexturePath(raw);
    }

    public ResourceLocation resolveThirdTextureId() {
        if (thirdTextureId != null) {
            return normalizeTexturePath(thirdTextureId);
        }
        return resolveTextureId();
    }

    public String resolveGeoVisibleRoot() {
        if (geoVisibleRoot == null || geoVisibleRoot.isBlank()) {
            return null;
        }
        return geoVisibleRoot;
    }

    public List<String> resolveGeoVisibleRoots() {
        return normalizeVisibleRoots(geoVisibleRoots, resolveGeoVisibleRoot());
    }

    public String resolveThirdGeoVisibleRoot() {
        if (thirdGeoVisibleRoot == null || thirdGeoVisibleRoot.isBlank()) {
            return null;
        }
        return thirdGeoVisibleRoot;
    }

    public List<String> resolveThirdGeoVisibleRoots() {
        return normalizeVisibleRoots(thirdGeoVisibleRoots, resolveThirdGeoVisibleRoot());
    }

    public List<FoodStackData.FoodEffect> resolveEffects() {
        if (effects == null || effects.isEmpty()) {
            return List.of();
        }
        List<FoodStackData.FoodEffect> result = new ArrayList<>();
        for (EffectDefinition definition : effects) {
            FoodStackData.FoodEffect converted = definition == null ? null : definition.toFoodEffect();
            if (converted != null) {
                result.add(converted);
            }
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    public List<FoodStackData.CustomEffect> resolveCustomEffects() {
        if (customEffects == null || customEffects.isEmpty()) {
            return List.of();
        }
        List<FoodStackData.CustomEffect> result = new ArrayList<>();
        for (CustomEffectDefinition definition : customEffects) {
            FoodStackData.CustomEffect converted = definition == null ? null : definition.toCustomEffect();
            if (converted != null && !isBlank(converted.type())) {
                result.add(converted);
            }
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    public @Nullable FoodStackData.ThirstSpec resolveThirstSpec() {
        return thirst == null ? null : thirst.toThirstSpec();
    }

    public boolean shouldTooltipShowNutrition() {
        if (tooltip == null || tooltip.showNutrition == null) {
            return true;
        }
        return tooltip.showNutrition;
    }

    public boolean shouldTooltipShowEffects() {
        if (tooltip == null || tooltip.showEffects == null) {
            return true;
        }
        return tooltip.showEffects;
    }

    public List<TooltipLine> resolveTooltipLines() {
        if (tooltip == null || tooltip.lines == null || tooltip.lines.isEmpty()) {
            return List.of();
        }
        List<TooltipLine> result = new ArrayList<>();
        for (TooltipLineDefinition line : tooltip.lines) {
            if (line == null) {
                continue;
            }
            TooltipLine converted = line.toTooltipLine();
            if (converted != null) {
                result.add(converted);
            }
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    public @Nullable FoodStackData.FlavorMessageSpec resolveFlavorMessageSpec() {
        FlavorMessagesDefinition definition = null;
        if (flavorMessages != null && !flavorMessages.isEmpty()) {
            definition = flavorMessages;
        } else if (postConsumeMessages != null && !postConsumeMessages.isEmpty()) {
            definition = postConsumeMessages;
        }
        if (definition == null) {
            return null;
        }
        return definition.toFlavorMessageSpec(messageMode);
    }

    public List<String> collectSchemaWarnings() {
        List<String> warnings = new ArrayList<>();
        if (effects != null) {
            for (int i = 0; i < effects.size(); i++) {
                EffectDefinition definition = effects.get(i);
                if (definition == null || definition.toFoodEffect() == null) {
                    warnings.add("effects[" + i + "] is invalid or missing id");
                }
            }
        }
        if (customEffects != null) {
            for (int i = 0; i < customEffects.size(); i++) {
                CustomEffectDefinition definition = customEffects.get(i);
                if (definition == null || definition.toCustomEffect() == null) {
                    warnings.add("custom_effects[" + i + "] is invalid or missing type");
                }
            }
        }
        if (thirst != null && thirst.toThirstSpec() == null) {
            warnings.add("thirst exists but has invalid or zero deltas");
        }
        if (tooltip != null && tooltip.lines != null) {
            for (int i = 0; i < tooltip.lines.size(); i++) {
                TooltipLineDefinition line = tooltip.lines.get(i);
                if (line == null || line.toTooltipLine() == null) {
                    warnings.add("tooltip.lines[" + i + "] has no text/lang_key");
                }
            }
        }
        if (useSelector != null && (useSelector.rules == null || useSelector.rules.isEmpty())) {
            warnings.add("use_selector exists but has no valid rules");
        }
        if (durabilityUse != null && durabilityUse.enabled != null && durabilityUse.enabled) {
            int max = firstNonNull(durabilityUse.max, durabilityUse.maxDurability, 0);
            if (max <= 0) {
                warnings.add("durability_use.enabled is true but max/max_durability <= 0");
            }
        }
        if ((flavorMessages != null && !flavorMessages.isEmpty())
                || (postConsumeMessages != null && !postConsumeMessages.isEmpty())) {
            if (resolveFlavorMessageSpec() == null) {
                warnings.add("flavor_messages/post_consume_messages exists but has no valid groups/segments");
            }
        }
        return warnings;
    }

    private static ResourceLocation normalizeTexturePath(ResourceLocation raw) {
        String path = raw.getPath();
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return ResourceLocation.fromNamespaceAndPath(raw.getNamespace(), path);
    }

    private static List<String> normalizeVisibleRoots(List<String> roots, String singleRoot) {
        List<String> result = new ArrayList<>();
        if (roots != null) {
            for (String root : roots) {
                if (root != null && !root.isBlank() && !result.contains(root)) {
                    result.add(root);
                }
            }
        }
        if (singleRoot != null && !singleRoot.isBlank() && !result.contains(singleRoot)) {
            result.add(singleRoot);
        }
        return result;
    }

    private Integer getIntStateParam(String key) {
        if (stateMachineParams == null || key == null || key.isBlank()) {
            return null;
        }
        Object value = stateMachineParams.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Float getFloatStateParam(String key) {
        if (stateMachineParams == null || key == null || key.isBlank()) {
            return null;
        }
        Object value = stateMachineParams.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        if (value instanceof String text) {
            try {
                return Float.parseFloat(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(@Nullable String value) {
        return value == null || value.isBlank();
    }

    private static final class TooltipDefinition {
        @SerializedName("show_nutrition")
        private Boolean showNutrition;
        @SerializedName("show_effects")
        private Boolean showEffects;
        @SerializedName("lines")
        private List<TooltipLineDefinition> lines;
    }

    private static final class TooltipLineDefinition {
        private String text;
        @SerializedName("lang_key")
        private String langKey;
        private String style;

        private @Nullable TooltipLine toTooltipLine() {
            String normalizedText = isBlank(text) ? null : text.trim();
            String normalizedLang = isBlank(langKey) ? null : langKey.trim();
            if (normalizedText == null && normalizedLang == null) {
                return null;
            }
            return new TooltipLine(normalizedText, normalizedLang, style);
        }
    }

    private static final class EffectDefinition {
        @SerializedName("id")
        private ResourceLocation id;
        @SerializedName("effect")
        private ResourceLocation effectAlias;
        @SerializedName("duration_ticks")
        private Integer durationTicks;
        @SerializedName("duration")
        private Integer duration;
        @SerializedName("duration_seconds")
        private Float durationSeconds;
        @SerializedName("amplifier")
        private Integer amplifier;
        @SerializedName("chance")
        private Float chance;
        @SerializedName("probability")
        private Float probability;
        @SerializedName("ambient")
        private Boolean ambient;
        @SerializedName("show_particles")
        private Boolean showParticles;
        @SerializedName("show_icon")
        private Boolean showIcon;

        private @Nullable FoodStackData.FoodEffect toFoodEffect() {
            ResourceLocation effectId = firstNonNull(id, effectAlias);
            if (effectId == null) {
                return null;
            }

            Integer rawDurationTicks = firstNonNull(durationTicks, duration);
            int resolvedDurationTicks;
            if (rawDurationTicks != null) {
                resolvedDurationTicks = Mth.clamp(rawDurationTicks, 1, 72_000);
            } else if (durationSeconds != null) {
                resolvedDurationTicks = Mth.clamp(Math.round(Math.max(durationSeconds, 0.05f) * 20.0f), 1, 72_000);
            } else {
                resolvedDurationTicks = 100;
            }

            int resolvedAmplifier = Math.max(amplifier == null ? 0 : amplifier, 0);
            float resolvedChance = Mth.clamp(firstNonNull(chance, probability, 1.0f), 0.0f, 1.0f);
            boolean resolvedAmbient = ambient != null && ambient;
            boolean resolvedShowParticles = showParticles == null || showParticles;
            boolean resolvedShowIcon = showIcon == null || showIcon;

            return new FoodStackData.FoodEffect(
                    effectId,
                    resolvedDurationTicks,
                    resolvedAmplifier,
                    resolvedChance,
                    resolvedAmbient,
                    resolvedShowParticles,
                    resolvedShowIcon
            );
        }
    }

    private static final class CustomEffectDefinition {
        private String type;
        private Float value;
        @SerializedName("duration_ticks")
        private Integer durationTicks;
        @SerializedName("duration")
        private Integer durationAlias;
        @SerializedName("duration_seconds")
        private Float durationSeconds;
        @SerializedName("interval_ticks")
        private Integer intervalTicks;
        @SerializedName("interval")
        private Integer intervalAlias;
        @SerializedName("interval_seconds")
        private Float intervalSeconds;
        @SerializedName("chance")
        private Float chance;
        @SerializedName("probability")
        private Float probability;

        private @Nullable FoodStackData.CustomEffect toCustomEffect() {
            if (isBlank(type)) {
                return null;
            }
            float resolvedValue = value == null ? 0.0f : value;
            int resolvedDurationTicks = 0;
            Integer rawDurationTicks = firstNonNull(durationTicks, durationAlias);
            if (rawDurationTicks != null) {
                resolvedDurationTicks = Mth.clamp(rawDurationTicks, 0, 72_000);
            } else if (durationSeconds != null) {
                resolvedDurationTicks = Mth.clamp(Math.round(Math.max(durationSeconds, 0.0f) * 20.0f), 0, 72_000);
            }
            int resolvedIntervalTicks = 0;
            Integer rawIntervalTicks = firstNonNull(intervalTicks, intervalAlias);
            if (rawIntervalTicks != null) {
                resolvedIntervalTicks = Mth.clamp(rawIntervalTicks, 0, 72_000);
            } else if (intervalSeconds != null) {
                resolvedIntervalTicks = Mth.clamp(Math.round(Math.max(intervalSeconds, 0.0f) * 20.0f), 0, 72_000);
            }
            float resolvedChance = Mth.clamp(firstNonNull(chance, probability, 1.0f), 0.0f, 1.0f);
            return new FoodStackData.CustomEffect(
                    type.trim().toLowerCase(Locale.ROOT),
                    resolvedValue,
                    resolvedDurationTicks,
                    resolvedIntervalTicks,
                    resolvedChance
            );
        }
    }

    private static final class ThirstDefinition {
        private Integer delta;
        private Integer value;
        private Integer amount;
        @SerializedName("thirst_delta")
        private Integer thirstDelta;
        @SerializedName("water_delta")
        private Integer waterDelta;
        private Integer water;
        private Integer hydration;
        @SerializedName("quenched_delta")
        private Integer quenchedDelta;
        private String mode;
        @SerializedName("compat_keep")
        private List<String> compatKeep;

        private @Nullable FoodStackData.ThirstSpec toThirstSpec() {
            int resolvedThirstDelta = firstNonNull(thirstDelta, delta, value, amount, 0);
            int resolvedWaterDelta = firstNonNull(waterDelta, water, hydration, quenchedDelta, 0);
            if (resolvedThirstDelta == 0 && resolvedWaterDelta == 0) {
                return null;
            }
            FoodStackData.ThirstMode resolvedMode = FoodStackData.ThirstMode.fromString(mode);
            List<FoodStackData.EffectChannel> keepChannels = new ArrayList<>();
            if (compatKeep != null) {
                for (String rawChannel : compatKeep) {
                    FoodStackData.EffectChannel channel = FoodStackData.EffectChannel.fromString(rawChannel);
                    if (channel != null && !keepChannels.contains(channel)) {
                        keepChannels.add(channel);
                    }
                }
            }
            return new FoodStackData.ThirstSpec(resolvedThirstDelta, resolvedWaterDelta, resolvedMode, keepChannels);
        }
    }

    private static final class UseSelectorDefinition {
        private String mode;
        @SerializedName("default_clip")
        private String defaultClip;
        private List<UseSelectorRuleDefinition> rules;
    }

    private static final class UseSelectorRuleDefinition {
        @SerializedName("min_amount")
        private Integer minAmount;
        @SerializedName("max_amount")
        private Integer maxAmount;
        private Integer min;
        private Integer max;
        private String clip;
        @SerializedName("duration_ticks")
        private Integer durationTicks;
        private Integer duration;
    }

    private static final class DurabilityUseDefinition {
        private Boolean enabled;
        private String resource;
        private Integer max;
        @SerializedName("max_durability")
        private Integer maxDurability;
        private Integer current;
        @SerializedName("current_durability")
        private Integer currentDurability;
        @SerializedName("per_point")
        private Integer perPoint;
        @SerializedName("points_per_unit")
        private Integer pointsPerUnit;
    }

    private static final class FlavorMessagesDefinition {
        private String mode;
        private String pick;
        @SerializedName("cooldown_ticks")
        private Integer cooldownTicks;
        private List<FlavorMessageGroupDefinition> groups;

        private boolean isEmpty() {
            return groups == null || groups.isEmpty();
        }

        private @Nullable FoodStackData.FlavorMessageSpec toFlavorMessageSpec(@Nullable String fallbackMode) {
            if (groups == null || groups.isEmpty()) {
                return null;
            }
            FoodStackData.MessageMode resolvedMode = FoodStackData.MessageMode.fromString(
                    isBlank(mode) ? fallbackMode : mode
            );
            String resolvedPick = isBlank(pick) ? "random_weighted" : pick.trim().toLowerCase(Locale.ROOT);
            int resolvedCooldownTicks = Mth.clamp(cooldownTicks == null ? 0 : cooldownTicks, 0, 72_000);

            List<FoodStackData.FlavorGroup> convertedGroups = new ArrayList<>();
            for (FlavorMessageGroupDefinition groupDefinition : groups) {
                if (groupDefinition == null) {
                    continue;
                }
                FoodStackData.FlavorGroup converted = groupDefinition.toFlavorGroup();
                if (converted != null && !converted.segments().isEmpty()) {
                    convertedGroups.add(converted);
                }
            }
            if (convertedGroups.isEmpty()) {
                return null;
            }
            return new FoodStackData.FlavorMessageSpec(
                    resolvedMode,
                    resolvedPick,
                    resolvedCooldownTicks,
                    convertedGroups
            );
        }
    }

    private static final class FlavorMessageGroupDefinition {
        private String id;
        private Integer weight;
        private List<FlavorMessageSegmentDefinition> segments;

        private @Nullable FoodStackData.FlavorGroup toFlavorGroup() {
            if (segments == null || segments.isEmpty()) {
                return null;
            }
            List<FoodStackData.FlavorSegment> convertedSegments = new ArrayList<>();
            for (FlavorMessageSegmentDefinition segmentDefinition : segments) {
                if (segmentDefinition == null) {
                    continue;
                }
                FoodStackData.FlavorSegment segment = segmentDefinition.toFlavorSegment();
                if (segment != null) {
                    convertedSegments.add(segment);
                }
            }
            if (convertedSegments.isEmpty()) {
                return null;
            }
            return new FoodStackData.FlavorGroup(Math.max(weight == null ? 1 : weight, 1), convertedSegments);
        }
    }

    private static final class FlavorMessageSegmentDefinition {
        @SerializedName("at_ticks")
        private Integer atTicks;
        @SerializedName("delay_ticks")
        private Integer delayTicks;
        private String text;
        @SerializedName("lang_key")
        private String langKey;

        private @Nullable FoodStackData.FlavorSegment toFlavorSegment() {
            String normalizedText = isBlank(text) ? null : text.trim();
            String normalizedLang = isBlank(langKey) ? null : langKey.trim();
            if (normalizedText == null && normalizedLang == null) {
                return null;
            }
            int tick = Math.max(firstNonNull(atTicks, delayTicks, 0), 0);
            return new FoodStackData.FlavorSegment(tick, normalizedText, normalizedLang);
        }
    }

    public record TooltipLine(@Nullable String text, @Nullable String langKey, @Nullable String style) {
        public Component toComponent() {
            String fallback = isBlank(text) ? "" : text;
            MutableComponent base = isBlank(langKey)
                    ? Component.literal(fallback)
                    : Component.translatableWithFallback(langKey, fallback);
            ChatFormatting format = parseFormat(style);
            if (format == null) {
                return base.withStyle(ChatFormatting.GRAY);
            }
            return base.withStyle(format);
        }

        private static @Nullable ChatFormatting parseFormat(@Nullable String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            try {
                return ChatFormatting.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
