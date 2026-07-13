import { describe, expect, it } from "vitest";
import {
  formatBlockStateInput,
  parseBlockStateInput,
  parseCopyPropertiesInput,
  validateBlockTransformIdInput
} from "../src/domain/authoringInputs";
import { candidateCoreFiles } from "../src/domain/candidateCore";
import {
  applyLocalDraft,
  draftIdentityFor,
  draftStorageKey,
  loadLocalDraft,
  saveLocalDraft,
  type StorageLike
} from "../src/domain/draftStorage";
import { addBlockTransform, updateBlockTransform, updateItemPresentation, updateItemProperty } from "../src/domain/mutations";
import { sampleProject } from "../src/data/sample";

class MemoryStorage implements StorageLike {
  readonly values = new Map<string, string>();
  getItem(key: string) { return this.values.get(key) ?? null; }
  setItem(key: string, value: string) { this.values.set(key, value); }
  removeItem(key: string) { this.values.delete(key); }
}

describe("block transform authoring inputs", () => {
  it("accepts only a flat string-valued block-state object", () => {
    expect(parseBlockStateInput('{"facing":"north","lit":"false"}')).toEqual({
      ok: true,
      value: { facing: "north", lit: "false" }
    });
    expect(parseBlockStateInput("[]")).toMatchObject({ ok: false });
    expect(parseBlockStateInput('{"age":2}')).toMatchObject({ ok: false });
    expect(parseBlockStateInput('{"facing":')).toMatchObject({ ok: false });
    expect(formatBlockStateInput({ facing: "north" })).toContain('"facing": "north"');
  });

  it("parses comma, Chinese-comma, and newline copy-property lists without hiding duplicates", () => {
    expect(parseCopyPropertiesInput("facing, waterlogged，axis\nfacing"))
      .toEqual(["facing", "waterlogged", "axis", "facing"]);
  });

  it("validates rule-id format and Java-normalized duplicates before a rename is committed", () => {
    const itemKey = sampleProject.items.find((item) => item.ownership === "utd" && item.managed)!.itemKey;
    const drafted = addBlockTransform(sampleProject, itemKey);
    const rule = drafted.blockTransforms[0];
    expect(validateBlockTransformIdInput("bad id!", rule, [])).toMatch(/规则 ID/);
    expect(validateBlockTransformIdInput("UTD:block_transform/Duplicate", rule, ["utd:block_transform/duplicate"]))
      .toMatch(/重复/);
    expect(validateBlockTransformIdInput("utd:block_transform/formal_rule", rule, [])).toBe("");
    const renamed = updateBlockTransform(drafted, rule.id, { id: "utd:block_transform/formal_rule" });
    const editedAfterRename = updateBlockTransform(renamed, "utd:block_transform/formal_rule", {
      clickedBlock: "minecraft:stone"
    });
    expect(editedAfterRename.blockTransforms[0]).toMatchObject({
      id: "utd:block_transform/formal_rule",
      clickedBlock: "minecraft:stone"
    });
  });
});

describe("lightweight browser drafts", () => {
  it("stores authored slices and lightweight recipe/Loot projections under projectId + catalog hash", () => {
    const storage = new MemoryStorage();
    const itemKey = sampleProject.items.find((item) => item.ownership === "utd" && item.managed)!.itemKey;
    let changed = updateItemPresentation(sampleProject, itemKey, { enabled: true, nameZhCn: "本地草稿名" });
    changed = addBlockTransform(changed, itemKey);
    const identity = draftIdentityFor(sampleProject);
    saveLocalDraft(storage, identity, changed, "2026-07-12T12:00:00.000Z");

    const raw = storage.getItem(draftStorageKey(identity))!;
    const document = JSON.parse(raw);
    expect(document).toMatchObject({
      project_id: identity.projectId,
      catalog_hash: identity.catalogHash,
      saved_at: "2026-07-12T12:00:00.000Z"
    });
    expect(document.presentations).toHaveLength(1);
    expect(document.block_transforms).toHaveLength(1);
    expect(document.recipe_edits).toHaveLength(sampleProject.recipes.filter((recipe) => recipe.editable).length);
    expect(document.loot_edits).toHaveLength(sampleProject.lootPolicies.length);
    expect(document.recipe_edits[0].raw).toBeUndefined();
    expect(document.loot_edits[0].raw).toBeUndefined();
    expect(document.items).toBeUndefined();
    expect(document.recipes).toBeUndefined();
    expect(raw.length).toBeLessThan(20_000);

    const loaded = loadLocalDraft(storage, identity)!;
    const restored = applyLocalDraft(sampleProject, loaded);
    expect(restored.presentations[0].nameZhCn).toBe("本地草稿名");
    expect(restored.blockTransforms).toHaveLength(1);
    expect(restored.items).toHaveLength(sampleProject.items.length);
    expect(restored.recipes).toHaveLength(sampleProject.recipes.length);
  });

  it("isolates another catalog hash and rejects malformed stored structures", () => {
    const storage = new MemoryStorage();
    const identity = draftIdentityFor(sampleProject);
    saveLocalDraft(storage, identity, sampleProject);
    expect(loadLocalDraft(storage, { ...identity, catalogHash: "another-catalog" })).toBeNull();
    storage.setItem(draftStorageKey(identity), JSON.stringify({
      schema_version: "utd-asset-workbench-local-draft/v2",
      project_id: identity.projectId,
      catalog_hash: identity.catalogHash,
      saved_at: "now",
      presentations: [],
      block_transforms: [{ enabled: true }],
      recipe_edits: [],
      loot_edits: []
    }));
    expect(() => loadLocalDraft(storage, identity)).toThrow(/结构无效/);
  });
});

describe("item property publication guardrails", () => {
  it("auto-enables a property when a runtime field is edited", () => {
    const item = sampleProject.items.find((entry) => entry.managed && entry.ownership === "utd")!;
    const changed = updateItemProperty(sampleProject, item.itemKey, { rarity: { value: 4 } });
    expect(changed.itemProperties.find((entry) => entry.itemKey === item.itemKey)).toMatchObject({
      enabled: true,
      rarity: { value: 4 }
    });
  });

  it("keeps an explicit withdrawal disabled until another runtime edit", () => {
    const item = sampleProject.items.find((entry) => entry.managed && entry.ownership === "utd")!;
    const enabled = updateItemProperty(sampleProject, item.itemKey, { rarity: { value: 3 } });
    const disabled = updateItemProperty(enabled, item.itemKey, { enabled: false });
    expect(disabled.itemProperties.find((entry) => entry.itemKey === item.itemKey)?.enabled).toBe(false);
    const edited = updateItemProperty(disabled, item.itemKey, {
      blockz: { width: 2, height: 2, capacityWidth: null, capacityHeight: null }
    });
    expect(edited.itemProperties.find((entry) => entry.itemKey === item.itemKey)?.enabled).toBe(true);
  });
});

describe("candidate core download set", () => {
  it("includes the canonical project and all runtime-facing authored drafts", () => {
    const itemKey = sampleProject.items.find((item) => item.ownership === "utd" && item.managed)!.itemKey;
    let changed = addBlockTransform(sampleProject, itemKey);
    const id = changed.blockTransforms[0].id;
    changed = updateBlockTransform(changed, id, {
      clickedBlock: "minecraft:stone",
      targetState: { axis: "y" },
      resultBlock: "minecraft:cobblestone",
      resultState: { waterlogged: "false" },
      copyProperties: ["facing"],
      hand: "any",
      allowFakePlayer: true,
      consumeInput: false,
      creativeRequireInput: false,
      creativeConsume: false,
      enabled: true
    });
    const files = candidateCoreFiles(changed);
    expect(files.map((file) => file.filename)).toEqual([
      "workbench.json",
      "utd_block_transforms.json",
      "utd_item_presentations.json",
      "utd_item_properties.json"
    ]);
    const transforms = JSON.parse(files[1].content);
    expect(transforms.rules[0]).toMatchObject({
      target: { state: { axis: "y" } },
      activation: { hand: "any", allowFakePlayer: true },
      catalyst: { consume: false },
      result: { state: { waterlogged: "false" }, copyProperties: ["facing"] },
      creative: { requireInput: false, consume: false }
    });
  });

  it("emits concrete RarityCore and BlockZ adapter candidates", () => {
    const item = sampleProject.items.find((entry) => entry.managed && entry.ownership === "utd" && !entry.variantDiscriminator)!;
    const changed = updateItemProperty(sampleProject, item.itemKey, {
      enabled: true,
      rarity: { value: 4 },
      blockz: { width: 2, height: 3, capacityWidth: 5, capacityHeight: 4 }
    });
    const files = candidateCoreFiles(changed);
    const names = files.map((file) => file.filename);
    expect(names).toContain("integrations/raritycore/FinalRarityConfig/utd_asset_workbench.json");
    expect(names).toContain("integrations/blockz/grid_items.utd-overrides.json");
    const rarity = JSON.parse(files.find((file) => file.filename.includes("FinalRarity"))!.content);
    const blockz = JSON.parse(files.find((file) => file.filename.includes("grid_items"))!.content);
    expect(rarity[item.registryId]).toBe(4);
    expect(blockz.items[item.registryId]).toEqual({ width: 2, height: 3, cap_width: 5, cap_height: 4 });
  });
});
