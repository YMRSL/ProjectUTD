package com.ymrsl.utdassetmanager.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AssetIdentity {
    private static final Pattern REGISTRY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern GUN_ID = Pattern.compile("(?:^|[,{])\\s*GunId\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern AMMO_ID = Pattern.compile("(?:^|[,{])\\s*AmmoId\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern ATTACHMENT_ID = Pattern.compile("(?:^|[,{])\\s*AttachmentId\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern FOOD_ID = Pattern.compile("(?:^|[,{])\\s*food_id\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private AssetIdentity() {
    }

    public static String normalizeRegistryId(String registryId) {
        String normalized = registryId == null ? "" : registryId.trim().toLowerCase(Locale.ROOT);
        if (!REGISTRY_ID.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid registry id: " + registryId);
        }
        return normalized;
    }

    public static String normalizeComponents(String componentsSnbt) {
        String normalized = componentsSnbt == null ? "{}" : componentsSnbt.trim();
        return normalized.isEmpty() ? "{}" : normalized;
    }

    public static String assetKey(String registryId, String componentsSnbt) {
        String canonical = normalizeRegistryId(registryId) + "\n" + normalizeComponents(componentsSnbt);
        return "asset_" + sha256(canonical);
    }

    public static String variantKey(String componentsCanonical) {
        return "variant_" + sha256(normalizeComponents(componentsCanonical));
    }

    public static String identityComponentsCanonical(String observedCanonical, String variantDiscriminator) {
        String discriminator = variantDiscriminator == null ? "" : variantDiscriminator.trim();
        if (discriminator.isEmpty()) {
            return normalizeComponents(observedCanonical);
        }
        return "D{" + discriminator.length() + ":" + discriminator + "}";
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    public static String variantKind(String registryId, String componentsSnbt) {
        String id = normalizeRegistryId(registryId);
        String components = normalizeComponents(componentsSnbt);
        if (id.startsWith("tacz:") || components.contains("GunId") || components.contains("AmmoId")
                || components.contains("AttachmentId")) {
            return "tacz_component";
        }
        if (id.startsWith("firstpersonfoodeating:") || components.contains("firstpersonfoodeating_profile")) {
            return "fpe_component";
        }
        return "{}".equals(components) ? "plain" : "component";
    }

    public static String variantDiscriminator(String componentsSnbt) {
        String components = normalizeComponents(componentsSnbt);
        String value = firstGroup(GUN_ID, components);
        if (!value.isBlank()) return "GunId=" + value;
        value = firstGroup(AMMO_ID, components);
        if (!value.isBlank()) return "AmmoId=" + value;
        value = firstGroup(ATTACHMENT_ID, components);
        if (!value.isBlank()) return "AttachmentId=" + value;
        value = firstGroup(FOOD_ID, components);
        return value.isBlank() ? "" : "food_id=" + value;
    }

    public static String[] previewComponent(String variantDiscriminator) {
        String discriminator = variantDiscriminator == null ? "" : variantDiscriminator.trim();
        int separator = discriminator.indexOf('=');
        if (separator <= 0 || separator == discriminator.length() - 1) {
            return null;
        }
        String key = discriminator.substring(0, separator).trim();
        String value = discriminator.substring(separator + 1).trim();
        if (value.isBlank()) {
            return null;
        }
        return switch (key) {
            case "GunId", "AmmoId", "AttachmentId", "food_id" -> new String[]{key, value};
            default -> null;
        };
    }

    private static String firstGroup(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }
}
