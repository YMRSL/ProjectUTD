import { createHash } from "node:crypto";
import { readFileSync } from "node:fs";
import JSZip from "jszip";
import { describe, expect, it } from "vitest";
import { buildCandidatePackage } from "../src/domain/candidateCore";
import {
  applyDraftSlices,
  applyLocalDraft,
  draftIdentityFor,
  draftSlicesDigest,
  draftStorageKey,
  extractDraftSlices,
  loadLocalDraft,
  MAX_LOCAL_DRAFT_BYTES,
  saveLocalDraft,
  type StorageLike
} from "../src/domain/draftStorage";
import { assertWorkbenchProject, exportBlockTransformsJson } from "../src/domain/exporters";
import {
  addBlockTransform,
  updateBlockTransform,
  updateItemPresentation,
  updateManagedLoot,
  updateManagedRecipe
} from "../src/domain/mutations";
import { sampleProject } from "../src/data/sample";

class MemoryStorage implements StorageLike {
  readonly values = new Map<string, string>();
  getItem(key: string) { return this.values.get(key) ?? null; }
  setItem(key: string, value: string) { this.values.set(key, value); }
  removeItem(key: string) { this.values.delete(key); }
}

describe("local draft v2 safety", () => {
  it("digests, restores, and clears every editable UI slice by exact identity", () => {
    const baseline = structuredClone(sampleProject);
    const recipe = baseline.recipes.find((entry) => entry.editable)!;
    const loot = baseline.lootPolicies[0];
    const item = baseline.items.find((entry) => entry.managed && entry.ownership === "utd")!;
    const baselineDigest = draftSlicesDigest(baseline);

    const recipeEdited = updateManagedRecipe(baseline, recipe.id, { station: "安全测试站", level: 4 });
    expect(draftSlicesDigest(recipeEdited)).not.toBe(baselineDigest);
    const lootEdited = updateManagedLoot(recipeEdited, loot.identityKey, {
      lootEnabled: !loot.lootEnabled,
      commonTags: ["safety_test"]
    });
    expect(draftSlicesDigest(lootEdited)).not.toBe(draftSlicesDigest(recipeEdited));
    const presentationEdited = updateItemPresentation(lootEdited, item.itemKey, {
      enabled: true,
      nameZhCn: "安全草稿名"
    });
    expect(draftSlicesDigest(presentationEdited)).not.toBe(draftSlicesDigest(lootEdited));
    const fullyEdited = addBlockTransform(presentationEdited, item.itemKey);
    expect(draftSlicesDigest(fullyEdited)).not.toBe(draftSlicesDigest(presentationEdited));

    const storage = new MemoryStorage();
    const identity = draftIdentityFor(baseline);
    saveLocalDraft(storage, identity, fullyEdited, "2026-07-12T13:00:00.000Z");
    const document = loadLocalDraft(storage, identity)!;
    const restored = applyLocalDraft(baseline, document);
    expect(restored.recipes.find((entry) => entry.id === recipe.id)).toMatchObject({ station: "安全测试站", level: 4 });
    expect(restored.lootPolicies.find((entry) => entry.identityKey === loot.identityKey)).toMatchObject({
      lootEnabled: !loot.lootEnabled,
      commonTags: ["safety_test"]
    });
    expect(restored.presentations[0].nameZhCn).toBe("安全草稿名");
    expect(restored.blockTransforms).toHaveLength(1);

    const cleared = applyDraftSlices(restored, extractDraftSlices(baseline));
    expect(draftSlicesDigest(cleared)).toBe(baselineDigest);
    expect(cleared.recipes.find((entry) => entry.id === recipe.id)?.station).toBe(recipe.station);
    expect(cleared.lootPolicies.find((entry) => entry.identityKey === loot.identityKey)?.commonTags).toEqual(loot.commonTags);
  });

  it("rejects fuzzy or partial recipe/Loot replay", () => {
    const slices = extractDraftSlices(sampleProject);
    expect(() => applyDraftSlices(sampleProject, {
      ...slices,
      recipes: slices.recipes.slice(1)
    })).toThrow(/配方 id集合.*拒绝模糊回放/);
    expect(() => applyDraftSlices(sampleProject, {
      ...slices,
      lootPolicies: [{ ...slices.lootPolicies[0], identityKey: "missing:identity" }, ...slices.lootPolicies.slice(1)]
    })).toThrow(/Loot identityKey集合.*拒绝模糊回放/);
  });

  it("checks 2 MB before writing and preserves the previous recoverable draft", () => {
    const storage = new MemoryStorage();
    const identity = draftIdentityFor(sampleProject);
    saveLocalDraft(storage, identity, sampleProject, "2026-07-12T13:00:00.000Z");
    const key = draftStorageKey(identity);
    const previous = storage.getItem(key);
    const item = sampleProject.items.find((entry) => entry.managed && entry.ownership === "utd")!;
    const oversized = updateItemPresentation(sampleProject, item.itemKey, {
      enabled: true,
      descriptionZhCn: "超".repeat(MAX_LOCAL_DRAFT_BYTES)
    });
    expect(() => saveLocalDraft(storage, identity, oversized)).toThrow(/超过 2 MB.*旧草稿已保留/);
    expect(storage.getItem(key)).toBe(previous);
    expect(loadLocalDraft(storage, identity)).not.toBeNull();
  });

  it("keeps the real 890/960/798 project projection comfortably below 2 MB", () => {
    const project: unknown = JSON.parse(readFileSync(new URL("../public/data/workbench.json", import.meta.url), "utf8"));
    assertWorkbenchProject(project);
    expect([project.items.length, project.recipes.length, project.lootPolicies.length]).toEqual([890, 960, 798]);
    const storage = new MemoryStorage();
    const identity = draftIdentityFor(project);
    const document = saveLocalDraft(storage, identity, project, "2026-07-12T13:00:00.000Z");
    const raw = storage.getItem(draftStorageKey(identity))!;
    expect(document.recipe_edits).toHaveLength(960);
    expect(document.loot_edits).toHaveLength(798);
    expect(new TextEncoder().encode(raw).byteLength).toBeLessThan(MAX_LOCAL_DRAFT_BYTES);
    expect(new TextEncoder().encode(raw).byteLength).toBeLessThan(400_000);
  });
});

describe("candidate ZIP safety", () => {
  it("writes one ZIP with three core files and independently verifiable SHA-256 metadata", async () => {
    const candidate = await buildCandidatePackage(sampleProject, "2026-07-12T13:00:00.000Z");
    expect(candidate.filename).toBe("utd-assets-demo.candidate.zip");
    const zip = await JSZip.loadAsync(candidate.bytes);
    expect(Object.keys(zip.files).sort()).toEqual([
      "manifest.json",
      "utd_block_transforms.json",
      "utd_item_presentations.json",
      "workbench.json"
    ]);
    const manifest = JSON.parse(await zip.file("manifest.json")!.async("string"));
    expect(manifest).toEqual(candidate.manifest);
    expect(manifest.files.map((entry: { path: string }) => entry.path)).toEqual([
      "workbench.json",
      "utd_block_transforms.json",
      "utd_item_presentations.json"
    ]);
    for (const entry of manifest.files as Array<{ path: string; sha256: string; bytes: number }>) {
      const bytes = await zip.file(entry.path)!.async("uint8array");
      expect(bytes.byteLength).toBe(entry.bytes);
      expect(createHash("sha256").update(bytes).digest("hex")).toBe(entry.sha256);
    }
  });

  it("blocks both direct export and ZIP generation on same-target/same-priority conflicts", async () => {
    const catalysts = sampleProject.items.filter((entry) => entry.managed && entry.ownership === "utd").slice(0, 2);
    let project = addBlockTransform(sampleProject, catalysts[0].itemKey);
    project = addBlockTransform(project, catalysts[1].itemKey);
    for (const rule of [...project.blockTransforms]) {
      project = updateBlockTransform(project, rule.id, {
        enabled: true,
        priority: 100,
        clickedBlock: "minecraft:stone",
        resultBlock: rule.catalyst.ref === catalysts[0].registryId ? "minecraft:dirt" : "minecraft:grass_block"
      });
    }
    expect(() => exportBlockTransformsJson(project)).toThrow(/相同目标方块、目标状态和优先级/);
    await expect(buildCandidatePackage(project)).rejects.toThrow(/候选包未生成/);
  });
});
