package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.core.AssetIdentity;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import java.time.Instant;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        try {
            CompoundTag full = TagParser.parseTag(record.itemStackSnbt);
            ItemStack restored = ItemStack.parse(minecraft.level.registryAccess(), full).orElse(ItemStack.EMPTY);
            if (!restored.isEmpty()) {
                return restored;
            }
        } catch (Exception ignored) {
            // A status-manifest directory row intentionally has no exact stack
            // snapshot. Fall through to a base registry icon when possible.
        }
        try {
            ItemStack base = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(record.registryId)));
            return base.isEmpty() ? ItemStack.EMPTY : base;
        } catch (Exception invalidId) {
            return ItemStack.EMPTY;
        }
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
