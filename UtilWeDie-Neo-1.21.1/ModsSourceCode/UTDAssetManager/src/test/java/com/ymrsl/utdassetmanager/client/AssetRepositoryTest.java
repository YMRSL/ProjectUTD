package com.ymrsl.utdassetmanager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AssetRepositoryTest {
    @TempDir
    Path directory;

    @Test
    void keepsLastKnownGoodManifestAndForceRecoversAtTheSameObservedMtime() throws Exception {
        Path manifest = directory.resolve("status_manifest.json");
        Files.writeString(manifest, manifest("minecraft:stick", "minecraft:stick", "木棍", true));
        long firstMtime = Files.getLastModifiedTime(manifest).toMillis();

        AssetRepository repository = new AssetRepository(directory);
        assertEquals(1, repository.manifestDirectorySize());
        assertEquals(firstMtime, repository.manifestRevision());

        long brokenMtime = firstMtime + 2_000L;
        Files.writeString(manifest, "{broken");
        Files.setLastModifiedTime(manifest, FileTime.fromMillis(brokenMtime));
        repository.forceReloadManifest();

        assertEquals(1, repository.manifestDirectorySize());
        assertEquals(firstMtime, repository.manifestRevision());
        assertTrue(repository.manifestErrorMessage().contains("last_known_good"));

        Files.writeString(manifest, manifest("minecraft:stone", "minecraft:stone", "石头", false));
        Files.setLastModifiedTime(manifest, FileTime.fromMillis(brokenMtime));
        repository.forceReloadManifest();

        assertEquals(brokenMtime, repository.manifestRevision());
        assertEquals("minecraft:stone", repository.allManifestDirectory().getFirst().assetKey);
        assertEquals("", repository.manifestErrorMessage());

        Files.delete(manifest);
        repository.forceReloadManifest();
        assertEquals(0, repository.manifestDirectorySize());
        assertEquals(-1L, repository.manifestRevision());
    }

    @Test
    void separatesLocalAndHistoricalSelectionAndNeverExportsDirectoryRows() throws Exception {
        Files.writeString(
                directory.resolve("status_manifest.json"),
                manifest("minecraft:stick", "minecraft:stick", "木棍", true));
        AssetRepository repository = new AssetRepository(directory);
        AssetRecord directoryRecord = repository.allManifestDirectory().getFirst();

        AssetRecord local = new AssetRecord();
        local.assetKey = "asset_local_stick";
        local.registryId = "minecraft:stick";
        local.variantKind = "plain";
        local.displayNameZhCn = "木棍";
        local.capturedLocale = "zh_cn";
        local.itemStackSnbt = "{id:\"minecraft:stick\",count:1}";

        assertFalse(repository.statusFor(local).humanSelected());
        assertTrue(repository.projectStatusFor(directoryRecord).humanSelected());
        assertTrue(repository.manifestHumanSelected(local));
        assertFalse(repository.isSelectedIdentity(directoryRecord));

        repository.select(local);
        assertTrue(repository.statusFor(local).humanSelected());
        assertTrue(repository.isSelectedIdentity(directoryRecord));

        assertThrows(IllegalStateException.class, () -> repository.select(directoryRecord));
        assertThrows(IllegalStateException.class, () -> repository.selectAll(List.of(directoryRecord)));
        assertEquals(1, repository.allSelected().size());

        JsonObject exported = JsonParser.parseString(Files.readString(repository.exportSnapshot())).getAsJsonObject();
        assertEquals(1, exported.getAsJsonArray("items").size());
        assertEquals(
                "asset_local_stick",
                exported.getAsJsonArray("items").get(0).getAsJsonObject().get("asset_key").getAsString());
    }

    private static String manifest(String assetKey, String registryId, String name, boolean humanSelected) {
        return """
                {
                  "schema_version": "utd-asset-status/v1",
                  "items": [{
                    "asset_key": "%s",
                    "registry_id": "%s",
                    "identity_kind": "item",
                    "client_name_zh_cn": "%s",
                    "human_selected": %s,
                    "catalogued": true,
                    "sync_state": "synced",
                    "issues": []
                  }]
                }
                """.formatted(assetKey, registryId, name, humanSelected);
    }
}
