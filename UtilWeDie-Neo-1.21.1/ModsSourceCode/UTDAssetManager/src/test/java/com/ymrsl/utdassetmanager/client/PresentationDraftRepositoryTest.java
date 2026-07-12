package com.ymrsl.utdassetmanager.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ymrsl.utdassetmanager.model.PresentationApplyScope;
import com.ymrsl.utdassetmanager.model.PresentationDraft;
import com.ymrsl.utdassetmanager.model.PresentationDraftDocument;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PresentationDraftRepositoryTest {
    @TempDir
    Path directory;

    @Test
    void persistsExactIdentityAndRegistryScopesWithoutItemStackComponents() throws Exception {
        PresentationDraftRepository repository = new PresentationDraftRepository(directory);
        PresentationDraft registry = draft(
                "asset-source-stick", "minecraft:stick", "", PresentationApplyScope.REGISTRY, "木棍", "加工木棍");
        registry.descriptionZhCn = List.of("登记表第一行", "登记表第二行");
        repository.upsert(registry);

        PresentationDraft identity = draft(
                "asset-food-a",
                "firstpersonfoodeating:pack_food",
                "food_id=firstpersonfoodeating:i_bang_a",
                PresentationApplyScope.IDENTITY,
                "工地佬压缩卡路里棒",
                "能量压缩棒");
        repository.upsert(identity);

        assertTrue(repository.isWritable());
        assertEquals(2, repository.all().size());
        assertEquals("加工木棍", repository.findRegistry("minecraft:stick").nameZhCn);
        assertEquals(
                "能量压缩棒",
                repository.findIdentity(
                                "asset-food-a",
                                "firstpersonfoodeating:pack_food",
                                "food_id=firstpersonfoodeating:i_bang_a")
                        .nameZhCn);
        assertNull(repository.findIdentity(
                "asset-food-a",
                "firstpersonfoodeating:pack_food",
                "food_id=firstpersonfoodeating:i_bang_b"));
        assertEquals(
                "能量压缩棒",
                repository.resolveEnabled(
                                "asset-food-a",
                                "firstpersonfoodeating:pack_food",
                                "food_id=firstpersonfoodeating:i_bang_a")
                        .nameZhCn);
        assertEquals("加工木棍", repository.resolveEnabled("another-asset", "minecraft:stick", "variant").nameZhCn);

        Path live = directory.resolve(PresentationDraftRepository.FILE_NAME);
        assertTrue(Files.exists(live));
        assertTrue(Files.exists(directory.resolve(PresentationDraftRepository.FILE_NAME + ".bak")));
        String json = Files.readString(live);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertEquals(PresentationDraftDocument.SCHEMA, root.get("schema_version").getAsString());
        assertEquals(2, root.getAsJsonArray("drafts").size());
        assertTrue(json.contains("\"description_zh_cn\""));
        assertFalse(json.contains("item_stack"));
        assertFalse(json.contains("custom_name"));
        assertFalse(json.contains("lore"));

        PresentationDraft externalCopy = repository.findRegistry("minecraft:stick");
        externalCopy.descriptionZhCn.set(0, "调用方修改");
        assertEquals("登记表第一行", repository.findRegistry("minecraft:stick").descriptionZhCn.getFirst());
    }

    @Test
    void disabledIdentityFallsBackToEnabledRegistryAndRemoveChecksCompleteSourceIdentity() {
        PresentationDraftRepository repository = new PresentationDraftRepository(directory);
        PresentationDraft registry = draft(
                "asset-source", "example:item", "source-variant", PresentationApplyScope.REGISTRY, "原名", "通用名");
        repository.upsert(registry);

        PresentationDraft identity = draft(
                "asset-exact", "example:item", "exact-variant", PresentationApplyScope.IDENTITY, "原名", "精确名");
        identity.enabled = false;
        repository.upsert(identity);

        assertEquals(
                "通用名",
                repository.resolveEnabled("asset-exact", "example:item", "exact-variant").nameZhCn);

        PresentationDraft wrongRegistrySource = registry.copy();
        wrongRegistrySource.assetKey = "different-source";
        assertFalse(repository.remove(wrongRegistrySource));
        assertNotNull(repository.findRegistry("example:item"));
        assertTrue(repository.remove(registry));
        assertNull(repository.findRegistry("example:item"));
    }

    @Test
    void bridgesSyntheticAndCapturedAssetKeysByUniqueRegistryAndDiscriminator() throws Exception {
        PresentationDraftRepository repository = new PresentationDraftRepository(directory);
        String discriminator = "food_id=firstpersonfoodeating:i_bang_a";
        PresentationDraft captured = draft(
                "sha256-captured-key",
                "firstpersonfoodeating:pack_food",
                discriminator,
                PresentationApplyScope.IDENTITY,
                "工地佬压缩卡路里棒",
                "压缩能量棒");
        repository.upsert(captured);

        assertNull(repository.findIdentity(
                "recipe:synthetic-key", "firstpersonfoodeating:pack_food", discriminator));
        assertEquals(
                "sha256-captured-key",
                repository.findIdentitySemantic("firstpersonfoodeating:pack_food", discriminator).assetKey);
        assertEquals(
                "压缩能量棒",
                repository.resolveEnabled(
                                "recipe:synthetic-key", "firstpersonfoodeating:pack_food", discriminator)
                        .nameZhCn);

        PresentationDraft ambiguous = captured.copy();
        ambiguous.assetKey = "recipe:synthetic-key";
        assertThrows(IllegalArgumentException.class, () -> repository.upsert(ambiguous));
        assertEquals(1, repository.all().size());

        Path live = directory.resolve(PresentationDraftRepository.FILE_NAME);
        String oneDraft = Files.readString(live);
        JsonObject root = JsonParser.parseString(oneDraft).getAsJsonObject();
        JsonObject duplicate = root.getAsJsonArray("drafts").get(0).getAsJsonObject().deepCopy();
        duplicate.addProperty("asset_key", "loot:synthetic-key");
        root.getAsJsonArray("drafts").add(duplicate);
        Files.writeString(live, root.toString());
        Files.setLastModifiedTime(live, FileTime.fromMillis(repository.revision() + 2_000L));
        repository.forceReload();

        assertFalse(repository.isWritable());
        assertEquals(1, repository.all().size());
        assertTrue(repository.lastError().contains("ambiguous presentation target"));
    }

    @Test
    void malformedReplacementEntersReadOnlyAndKeepsLastKnownGoodUntilForcedRecovery() throws Exception {
        PresentationDraftRepository repository = new PresentationDraftRepository(directory);
        repository.upsert(draft(
                "asset-good", "minecraft:apple", "", PresentationApplyScope.IDENTITY, "苹果", "红苹果"));
        Path live = directory.resolve(PresentationDraftRepository.FILE_NAME);
        long goodRevision = repository.revision();

        long brokenRevision = goodRevision + 2_000L;
        Files.writeString(live, "{broken");
        Files.setLastModifiedTime(live, FileTime.fromMillis(brokenRevision));
        repository.forceReload();

        assertFalse(repository.isWritable());
        assertEquals(goodRevision, repository.revision());
        assertEquals("红苹果", repository.all().getFirst().nameZhCn);
        assertTrue(repository.lastError().contains("read_only_using_last_known_good"));
        assertThrows(IllegalStateException.class, () -> repository.upsert(draft(
                "asset-new", "minecraft:stone", "", PresentationApplyScope.IDENTITY, "石头", "方石")));
        assertEquals("{broken", Files.readString(live));

        Files.writeString(live, validDocument("asset-fixed", "minecraft:stone", "", "石头"));
        Files.setLastModifiedTime(live, FileTime.fromMillis(brokenRevision));
        repository.forceReload();

        assertTrue(repository.isWritable());
        assertEquals(brokenRevision, repository.revision());
        assertEquals("asset-fixed", repository.all().getFirst().assetKey);
        assertEquals("", repository.lastError());
    }

    @Test
    void initialCorruptionNeverGetsOverwrittenAndLastKnownGoodCanStillBeExported() throws Exception {
        Path live = directory.resolve(PresentationDraftRepository.FILE_NAME);
        Files.writeString(live, "[]");
        PresentationDraftRepository corrupt = new PresentationDraftRepository(directory);

        assertFalse(corrupt.isWritable());
        assertEquals(0, corrupt.all().size());
        assertThrows(IllegalStateException.class, () -> corrupt.upsert(draft(
                "asset", "minecraft:stone", "", PresentationApplyScope.IDENTITY, "石头", "石块")));
        assertEquals("[]", Files.readString(live));

        Files.writeString(live, validDocument("asset-export", "minecraft:diamond", "", "钻石"));
        corrupt.forceReload();
        Path exported = corrupt.exportSnapshot();
        JsonObject exportRoot = JsonParser.parseString(Files.readString(exported)).getAsJsonObject();
        JsonArray exportedDrafts = exportRoot.getAsJsonArray("drafts");
        assertEquals(PresentationDraftDocument.SCHEMA, exportRoot.get("schema_version").getAsString());
        assertEquals("asset-export", exportedDrafts.get(0).getAsJsonObject().get("asset_key").getAsString());
    }

    private static PresentationDraft draft(
            String assetKey,
            String registryId,
            String discriminator,
            PresentationApplyScope scope,
            String observedName,
            String name) {
        PresentationDraft draft = new PresentationDraft();
        draft.assetKey = assetKey;
        draft.registryId = registryId;
        draft.variantDiscriminator = discriminator;
        draft.applyScope = scope;
        draft.observedNameZhCn = observedName;
        draft.nameZhCn = name;
        draft.descriptionZhCn = new java.util.ArrayList<>();
        draft.enabled = true;
        draft.baseCatalogHash = "catalog-test";
        return draft;
    }

    private static String validDocument(String assetKey, String registryId, String discriminator, String name) {
        return """
                {
                  "schema_version": "utd-item-presentation/v1",
                  "producer": "test",
                  "updated_at": "2026-07-12T00:00:00Z",
                  "drafts": [{
                    "asset_key": "%s",
                    "registry_id": "%s",
                    "variant_discriminator": "%s",
                    "apply_scope": "identity",
                    "observed_name_zh_cn": "%s",
                    "name_zh_cn": "%s",
                    "description_zh_cn": [],
                    "enabled": true,
                    "base_catalog_hash": "catalog-test",
                    "updated_at": "2026-07-12T00:00:00Z"
                  }]
                }
                """.formatted(assetKey, registryId, discriminator, name, name);
    }
}
