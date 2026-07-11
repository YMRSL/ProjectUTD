package com.ymrsl.utdassetmanager.client;

import com.google.gson.Gson;
import com.ymrsl.utdassetmanager.core.AssetStatus;
import com.ymrsl.utdassetmanager.core.SyncState;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import com.ymrsl.utdassetmanager.model.StatusManifest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssetManifestContractTest {
    private static final Gson GSON = new Gson();

    @Test
    void readsFrozenSnakeCaseStatusManifest() {
        StatusManifest manifest = GSON.fromJson("""
                {
                  "schema_version": 1,
                  "generated_at": "2026-07-11T00:00:00Z",
                  "items": [{
                    "asset_key": "asset_abc",
                    "registry_id": "minecraft:stick",
                    "identity_kind": "loot_variant",
                    "variant_discriminator": "profile=abc",
                    "client_name_zh_cn": "木棍",
                    "translation_key": "item.minecraft.stick",
                    "components_snbt": "{profile:\\\"abc\\\"}",
                    "components_canonical": "C{profile=abc}",
                    "identity_components_canonical": "profile=abc",
                    "human_selected": true,
                    "catalogued": true,
                    "recipe_input_count": 2,
                    "recipe_output_count": 1,
                    "loot_enabled": true,
                    "loot_level": 3,
                    "sync_state": "synced",
                    "catalog_hash": "catalog",
                    "deployed_hash": "catalog",
                    "stale": false,
                    "issues": []
                  }]
                }
                """, StatusManifest.class);

        assertEquals(1, manifest.entries.size());
        assertEquals("asset_abc", manifest.entries.getFirst().assetKey);
        assertEquals(2, manifest.entries.getFirst().recipeInputCount);
        assertEquals("synced", manifest.entries.getFirst().syncState);
        assertEquals("木棍", manifest.entries.getFirst().clientNameZhCn);
        assertEquals("item.minecraft.stick", manifest.entries.getFirst().translationKey);
        assertEquals("{profile:\"abc\"}", manifest.entries.getFirst().componentsSnbt);
        assertEquals("C{profile=abc}", manifest.entries.getFirst().componentsCanonical);
        assertEquals("profile=abc", manifest.entries.getFirst().identityComponentsCanonical);
        assertTrue(manifest.entries.getFirst().humanSelected);
    }

    @Test
    void retainsCamelCaseCompatibilityForEarlierExamples() {
        StatusManifest manifest = GSON.fromJson("""
                {"schemaVersion":1,"entries":[{
                  "assetKey":"asset_old","registryId":"minecraft:stone",
                  "clientNameZhCn":"石头","translationKey":"block.minecraft.stone",
                  "componentsSnbt":"{}","componentsCanonical":"C{}",
                  "identityComponentsCanonical":"C{}","humanSelected":true,
                  "recipeInputCount":4,"syncState":"pending"
                }]}
                """, StatusManifest.class);

        assertEquals("asset_old", manifest.entries.getFirst().assetKey);
        assertEquals(4, manifest.entries.getFirst().recipeInputCount);
        assertEquals("石头", manifest.entries.getFirst().clientNameZhCn);
        assertTrue(manifest.entries.getFirst().humanSelected);
    }

    @Test
    void exportsFlatSnakeCaseWorkbenchSnapshotRows() {
        AssetRecord record = new AssetRecord();
        record.assetKey = "asset_abc";
        record.variantKey = "variant_abc";
        record.registryId = "minecraft:stick";
        record.componentsSnbt = "{}";
        record.componentsCanonical = "C{}";
        record.identityComponentsCanonical = "C{}";
        record.displayNameZhCn = "木棍";
        record.translationKey = "item.minecraft.stick";
        AssetStatus status = new AssetStatus(
                true, true, 2, 1, true, 3, SyncState.SYNCED,
                "catalog", "catalog", false, List.of());

        Map<String, Object> exported = AssetRepository.exportRecord(record, status);

        assertEquals("asset_abc", exported.get("asset_key"));
        assertEquals("minecraft:stick", exported.get("registry_id"));
        assertEquals("C{}", exported.get("components_canonical"));
        assertEquals("C{}", exported.get("identity_components_canonical"));
        assertEquals("木棍", exported.get("client_name_zh_cn"));
        assertEquals("synced", exported.get("sync_state"));
        assertTrue(exported.containsKey("recipe_input_count"));
    }
}
