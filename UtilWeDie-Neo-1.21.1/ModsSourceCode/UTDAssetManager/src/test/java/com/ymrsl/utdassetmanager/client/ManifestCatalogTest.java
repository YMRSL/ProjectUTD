package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.model.AssetRecord;
import com.ymrsl.utdassetmanager.model.ManifestEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ManifestCatalogTest {
    @Test
    void createsReadOnlyDirectorySnapshotsFromManifestRows() {
        ManifestEntry plain = entry("minecraft:stick", "minecraft:stick", "plain", "");
        plain.clientNameZhCn = "木棍";
        plain.translationKey = "item.minecraft.stick";
        plain.humanSelected = true;

        ManifestEntry lootVariant = entry(
                "loot:firstpersonfoodeating:pack_food::food_id=test:meal",
                "firstpersonfoodeating:pack_food", "loot_variant", "food_id=test:meal");
        lootVariant.variantKey = "1234567890abcdef";
        lootVariant.clientNameZhCn = "Pack Food";
        lootVariant.componentsSnbt = "{food_id:\"test:meal\"}";
        lootVariant.componentsCanonical = "firstpersonfoodeating:pack_food{food_id=test:meal}";
        lootVariant.identityComponentsCanonical = "food_id=test:meal";

        ManifestCatalog catalog = ManifestCatalog.from(List.of(plain, lootVariant));
        List<AssetRecord> firstSnapshot = catalog.directorySnapshot();

        assertEquals(2, catalog.size());
        assertEquals("", firstSnapshot.getFirst().capturedLocale);
        assertEquals("{}", firstSnapshot.getFirst().itemStackSnbt);
        assertEquals("{}", firstSnapshot.getFirst().componentsSnbt);
        assertEquals("木棍", firstSnapshot.getFirst().displayNameZhCn);
        assertTrue(firstSnapshot.getFirst().humanSelected);
        assertEquals("{food_id:\"test:meal\"}", firstSnapshot.get(1).componentsSnbt);

        firstSnapshot.getFirst().displayNameZhCn = "changed";
        assertEquals("木棍", catalog.directorySnapshot().getFirst().displayNameZhCn);
    }

    @Test
    void resolvesExactPlainItemAndFrozenVariantIdentities() {
        ManifestEntry item = entry("tacz:ammo", "tacz:ammo", "item", "");
        ManifestEntry variant = entry(
                "recipe:tacz:ammo::AmmoId=tacz_unidict:pistol",
                "tacz:ammo", "recipe_variant", "AmmoId=tacz_unidict:pistol");
        variant.variantKey = "53589ee213cb9ebf";
        ManifestCatalog catalog = ManifestCatalog.from(List.of(item, variant));

        assertEquals(item.assetKey, catalog.resolve(item.assetKey, null).entry().assetKey);

        AssetRecord localPlain = record("asset_local_plain", "tacz:ammo", "plain", "");
        assertEquals(item.assetKey, catalog.resolve(localPlain.assetKey, localPlain).entry().assetKey);
        assertEquals(item.assetKey, catalog.canonicalRowKey(localPlain));

        AssetRecord localVariant = record(
                "asset_local_variant", "tacz:ammo", "tacz_component", "AmmoId=tacz_unidict:pistol");
        assertEquals(variant.assetKey, catalog.resolve(localVariant.assetKey, localVariant).entry().assetKey);
        assertEquals(variant.assetKey, catalog.canonicalRowKey(localVariant));
    }

    @Test
    void recoversFrozenDiscriminatorFromComponentsButRejectsUnknownComponents() {
        ManifestEntry item = entry("tacz:ammo", "tacz:ammo", "item", "");
        ManifestEntry variant = entry(
                "recipe:tacz:ammo::AmmoId=tacz_unidict:pistol",
                "tacz:ammo", "recipe_variant", "AmmoId=tacz_unidict:pistol");
        ManifestCatalog catalog = ManifestCatalog.from(List.of(item, variant));

        AssetRecord recovered = record("asset_component", "tacz:ammo", "tacz_component", "");
        recovered.componentsSnbt = "{AmmoId:\"tacz_unidict:pistol\"}";
        assertEquals(variant.assetKey, catalog.resolve(recovered.assetKey, recovered).entry().assetKey);

        AssetRecord unknown = record("asset_unknown", "tacz:ammo", "tacz_component", "");
        unknown.componentsSnbt = "{Damage:1}";
        assertNull(catalog.resolve(unknown.assetKey, unknown).entry());
        assertEquals("asset_unknown", catalog.canonicalRowKey(unknown));
    }

    @Test
    void refusesAmbiguousPlainAndVariantFallbacks() {
        ManifestEntry plainA = entry("plain_a", "example:shared", "plain", "");
        ManifestEntry plainB = entry("item_b", "example:shared", "item", "");
        ManifestEntry variantA = entry("variant_a", "example:variant", "recipe_variant", "mode=a");
        ManifestEntry variantB = entry("variant_b", "example:variant", "loot_variant", "mode=a");
        ManifestCatalog catalog = ManifestCatalog.from(List.of(plainA, plainB, variantA, variantB));

        AssetRecord plain = record("local_plain", "example:shared", "plain", "");
        ManifestCatalog.Resolution plainResolution = catalog.resolve(plain.assetKey, plain);
        assertNull(plainResolution.entry());
        assertTrue(plainResolution.issue().contains("manifest_fallback_ambiguous"));
        assertEquals("local_plain", catalog.canonicalRowKey(plain));

        AssetRecord variant = record("local_variant", "example:variant", "component", "mode=a");
        ManifestCatalog.Resolution variantResolution = catalog.resolve(variant.assetKey, variant);
        assertNull(variantResolution.entry());
        assertTrue(variantResolution.issue().contains("manifest_fallback_ambiguous"));
        assertEquals("local_variant", catalog.canonicalRowKey(variant));
    }

    @Test
    void rejectsDuplicateExactAssetKeys() {
        ManifestEntry first = entry("duplicate", "example:first", "plain", "");
        ManifestEntry second = entry("duplicate", "example:second", "plain", "");

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> ManifestCatalog.from(List.of(first, second)));

        assertTrue(error.getMessage().contains("duplicate asset_key"));
    }

    private static ManifestEntry entry(String assetKey, String registryId, String kind, String discriminator) {
        ManifestEntry entry = new ManifestEntry();
        entry.assetKey = assetKey;
        entry.registryId = registryId;
        entry.identityKind = kind;
        entry.variantDiscriminator = discriminator;
        return entry;
    }

    private static AssetRecord record(String assetKey, String registryId, String kind, String discriminator) {
        AssetRecord record = new AssetRecord();
        record.assetKey = assetKey;
        record.registryId = registryId;
        record.variantKind = kind;
        record.variantDiscriminator = discriminator;
        return record;
    }
}
