package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public final class FoodTextureResolver {
    private FoodTextureResolver() {
    }

    public static ResourceLocation resolveDisplayTexture(FoodDisplayDefinition display) {
        if (display == null) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/unknown_pack.png");
        }
        ResourceLocation raw = display.resolveTextureId();
        return resolveByCandidates(raw);
    }

    public static ResourceLocation resolveThirdTexture(FoodDisplayDefinition display) {
        if (display == null) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/unknown_pack.png");
        }
        ResourceLocation raw = display.resolveThirdTextureId();
        return resolveByCandidates(raw);
    }

    private static ResourceLocation resolveByCandidates(ResourceLocation raw) {
        List<ResourceLocation> candidates = collectCandidates(raw);
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        for (ResourceLocation candidate : candidates) {
            if (resourceManager.getResource(candidate).isPresent()) {
                return candidate;
            }
        }
        return candidates.isEmpty() ? raw : candidates.get(0);
    }

    private static List<ResourceLocation> collectCandidates(ResourceLocation raw) {
        Set<ResourceLocation> candidates = new LinkedHashSet<>();
        String namespace = raw.getNamespace();
        String path = raw.getPath();

        candidates.add(normalize(namespace, path));

        String withoutPrefix = path.startsWith("textures/") ? path.substring("textures/".length()) : path;
        String withoutExt = withoutPrefix.endsWith(".png")
                ? withoutPrefix.substring(0, withoutPrefix.length() - 4)
                : withoutPrefix;
        String leaf = withoutExt.substring(withoutExt.lastIndexOf('/') + 1);

        if (withoutExt.startsWith("item/")) {
            String sub = withoutExt.substring("item/".length());
            candidates.add(normalize(namespace, "food/" + sub));
            candidates.add(normalize(namespace, "item/" + leaf));
            candidates.add(normalize(namespace, "food/" + leaf));
        } else if (withoutExt.startsWith("food/")) {
            String sub = withoutExt.substring("food/".length());
            candidates.add(normalize(namespace, "item/" + sub));
            candidates.add(normalize(namespace, "item/" + leaf));
            candidates.add(normalize(namespace, "food/" + leaf));
        } else {
            candidates.add(normalize(namespace, "item/" + withoutExt));
            candidates.add(normalize(namespace, "food/" + withoutExt));
            candidates.add(normalize(namespace, "item/" + leaf));
            candidates.add(normalize(namespace, "food/" + leaf));
        }
        return new ArrayList<>(candidates);
    }

    private static ResourceLocation normalize(String namespace, String rawPath) {
        String path = rawPath.replace('\\', '/');
        if (!path.startsWith("textures/")) {
            path = "textures/" + path;
        }
        if (!path.endsWith(".png")) {
            path = path + ".png";
        }
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
