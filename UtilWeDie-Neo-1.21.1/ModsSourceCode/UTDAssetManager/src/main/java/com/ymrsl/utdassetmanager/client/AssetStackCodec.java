package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.core.AssetIdentity;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import java.time.Instant;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class AssetStackCodec {
    private AssetStackCodec() {
    }

    public static AssetRecord capture(ItemStack source) {
        Minecraft minecraft = Minecraft.getInstance();
        if (source == null || source.isEmpty() || minecraft.level == null) {
            return null;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(source.getItem());
        if (id == null) {
            return null;
        }
        ItemStack stack = source.copyWithCount(1);
        Tag encoded = stack.save(minecraft.level.registryAccess());
        if (!(encoded instanceof CompoundTag full)) {
            return null;
        }
        CompoundTag components = full.contains("components", Tag.TAG_COMPOUND)
                ? full.getCompound("components")
                : new CompoundTag();
        String componentsSnbt = components.toString();
        String componentsCanonical = CanonicalTagEncoder.encode(components);
        String registryId = id.toString();
        String now = Instant.now().toString();
        String variantDiscriminator = AssetIdentity.variantDiscriminator(componentsSnbt);
        String identityComponentsCanonical = AssetIdentity.identityComponentsCanonical(
                componentsCanonical, variantDiscriminator);

        AssetRecord record = new AssetRecord();
        record.registryId = registryId;
        record.modId = id.getNamespace();
        record.componentsSnbt = componentsSnbt;
        record.componentsCanonical = componentsCanonical;
        record.identityComponentsCanonical = identityComponentsCanonical;
        record.assetKey = AssetIdentity.assetKey(registryId, identityComponentsCanonical);
        record.variantKey = AssetIdentity.variantKey(identityComponentsCanonical);
        record.variantKind = AssetIdentity.variantKind(registryId, componentsSnbt);
        record.variantDiscriminator = variantDiscriminator;
        record.itemStackSnbt = full.toString();
        record.translationKey = stack.getDescriptionId();
        record.displayNameZhCn = stack.getHoverName().getString();
        record.capturedLocale = minecraft.getLanguageManager().getSelected();
        record.humanSelected = true;
        record.selectedAt = now;
        record.updatedAt = now;
        return record;
    }

    public static ItemStack restore(AssetRecord record) {
        Minecraft minecraft = Minecraft.getInstance();
        if (record == null || minecraft.level == null) {
            return ItemStack.EMPTY;
        }
        String itemStackSnbt = record.itemStackSnbt == null ? "" : record.itemStackSnbt.trim();
        if (!itemStackSnbt.isBlank() && !"{}".equals(itemStackSnbt)) {
            try {
                CompoundTag full = TagParser.parseTag(itemStackSnbt);
                ItemStack restored = ItemStack.parse(minecraft.level.registryAccess(), full).orElse(ItemStack.EMPTY);
                if (!restored.isEmpty()) {
                    return restored;
                }
            } catch (Exception ignored) {
                // A status-manifest directory row may carry no exact stack
                // snapshot. Fall through to a safe read-only preview.
            }
        }
        try {
            ItemStack base = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(record.registryId)));
            if (base.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (supportsVariantPreview(record)) {
                applyFoodPreview(base, record);
            }
            return base;
        } catch (Exception invalidId) {
            return ItemStack.EMPTY;
        }
    }

    public static boolean supportsVariantPreview(AssetRecord record) {
        return record != null
                && "firstpersonfoodeating:pack_food".equals(record.registryId)
                && foodId(record).contains(":");
    }

    private static void applyFoodPreview(ItemStack stack, AssetRecord record) {
        CompoundTag customData = parseCustomData(record.componentsSnbt);
        String foodId = foodId(record);
        customData.putString("food_id", foodId);
        CompoundTag profile = customData.contains("firstpersonfoodeating_profile", Tag.TAG_COMPOUND)
                ? customData.getCompound("firstpersonfoodeating_profile")
                : new CompoundTag();
        profile.putString("food_id", foodId);
        customData.put("firstpersonfoodeating_profile", profile);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));
    }

    private static CompoundTag parseCustomData(String componentsSnbt) {
        String snbt = componentsSnbt == null ? "" : componentsSnbt.trim();
        if (snbt.isBlank() || "{}".equals(snbt)) {
            return new CompoundTag();
        }
        try {
            CompoundTag parsed = TagParser.parseTag(snbt);
            if (parsed.contains("minecraft:custom_data", Tag.TAG_COMPOUND)) {
                return parsed.getCompound("minecraft:custom_data").copy();
            }
            return parsed.copy();
        } catch (Exception ignored) {
            return new CompoundTag();
        }
    }

    private static String foodId(AssetRecord record) {
        String discriminator = record == null || record.variantDiscriminator == null
                ? ""
                : record.variantDiscriminator.trim();
        String prefix = "food_id=";
        return discriminator.startsWith(prefix) ? discriminator.substring(prefix.length()).trim() : "";
    }

    public static String localizedBaseName(String registryId) {
        try {
            ResourceLocation id = ResourceLocation.parse(registryId);
            Item item = BuiltInRegistries.ITEM.get(id);
            if (item == Items.AIR && !"minecraft:air".equals(id.toString())) {
                return "";
            }
            return item.getDescription().getString();
        } catch (Exception invalidId) {
            return "";
        }
    }
}
