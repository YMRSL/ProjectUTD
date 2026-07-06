package io.github.ymrsl.firstpersonfoodeating.client.animation;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.core.registries.BuiltInRegistries;

public final class AnimationProfileRegistry {
    private static final AnimationProfile DEFAULT_EDIBLE = AnimationProfile.of(
            "generic_edible", 2, 1,
            0.20f, 0.60f, 0.16f, 0.50f, 0.18f, 0.16f, 0.16f
    );

    private static final Map<String, AnimationProfile> BUILTIN_PROFILES = Map.ofEntries(
            Map.entry("bang", AnimationProfile.of("i_bang_d", 1, 1, 0.20f, 0.62f, 0.16f, 0.52f, 0.22f, 0.17f, 0.18f)),
            Map.entry("bengdai", AnimationProfile.of("i_bengdai_a", 4, 1, 0.22f, 0.70f, 0.17f, 0.60f, 0.20f, 0.18f, 0.18f)),
            Map.entry("dai", AnimationProfile.of("i_dai_a", 1, 1, 0.20f, 0.58f, 0.16f, 0.52f, 0.20f, 0.17f, 0.17f)),
            Map.entry("guan", AnimationProfile.of("i_guan_b", 2, 1, 0.20f, 0.60f, 0.16f, 0.54f, 0.21f, 0.17f, 0.17f)),
            Map.entry("guantou", AnimationProfile.of("i_guantou_f", 1, 1, 0.20f, 0.56f, 0.16f, 0.56f, 0.20f, 0.17f, 0.18f)),
            Map.entry("he", AnimationProfile.of("i_he_a", 1, 1, 0.20f, 0.56f, 0.16f, 0.48f, 0.20f, 0.17f, 0.17f)),
            Map.entry("jia", AnimationProfile.of("i_jia_a", 1, 4, 0.22f, 0.58f, 0.17f, 0.62f, 0.24f, 0.18f, 0.18f)),
            Map.entry("jijiubao", AnimationProfile.of("i_jijiubao_a", 1, 2, 0.22f, 0.68f, 0.18f, 0.58f, 0.22f, 0.18f, 0.18f)),
            Map.entry("ping", AnimationProfile.of("i_ping_b", 4, 1, 0.21f, 0.62f, 0.17f, 0.50f, 0.20f, 0.17f, 0.18f)),
            Map.entry("yaoping", AnimationProfile.of("i_yaoping_b", 4, 1, 0.21f, 0.64f, 0.17f, 0.54f, 0.20f, 0.18f, 0.18f)),
            Map.entry("zhenji", AnimationProfile.of("i_zhenji_a", 4, 1, 0.22f, 0.66f, 0.18f, 0.62f, 0.22f, 0.18f, 0.18f))
    );

    private AnimationProfileRegistry() {
    }

    public static boolean hasProfile(ItemStack stack) {
        return resolve(stack) != null;
    }

    public static AnimationProfile resolve(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return null;
        }

        // Only FirstPersonFoodEating's own default item pack gets a custom use animation.
        // Never spread it to other mods' items — they keep the vanilla animation.
        if (!FirstPersonFoodEatingMod.MOD_ID.equals(key.getNamespace())) {
            return null;
        }

        String path = key.getPath();
        AnimationProfile profile = BUILTIN_PROFILES.get(path);
        if (profile != null) {
            return profile;
        }

        AnimationProfile keywordProfile = byKeyword(path);
        if (keywordProfile != null) {
            return keywordProfile;
        }

        // Generic edible fallback applies ONLY to FirstPersonFoodEating's own items.
        // External mod food (e.g. survival_instinct item/generated models) doesn't fit
        // this hand pose, so let it fall through to the vanilla eating animation.
        if (FirstPersonFoodEatingMod.MOD_ID.equals(key.getNamespace())) {
            UseAnim useAnim = stack.getUseAnimation();
            if (useAnim == UseAnim.DRINK || useAnim == UseAnim.EAT
                    || stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                return DEFAULT_EDIBLE;
            }
        }
        return null;
    }

    private static AnimationProfile byKeyword(String path) {
        if (path.contains("bandage") || path.contains("bengdai")) {
            return BUILTIN_PROFILES.get("bengdai");
        }
        if (path.contains("first_aid") || path.contains("jijiubao")) {
            return BUILTIN_PROFILES.get("jijiubao");
        }
        if (path.contains("syringe") || path.contains("zhenji")) {
            return BUILTIN_PROFILES.get("zhenji");
        }
        if (path.contains("medicine") || path.contains("med") || path.contains("yaoping")) {
            return BUILTIN_PROFILES.get("yaoping");
        }
        if (path.contains("potion") || path.contains("bottle") || path.contains("ping")) {
            return BUILTIN_PROFILES.get("ping");
        }
        if (path.contains("can") || path.contains("guan")) {
            return BUILTIN_PROFILES.get("guan");
        }
        return null;
    }
}
