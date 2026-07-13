import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { buildWorkbenchProject } from "../src/domain/build";
import {
  assertWorkbenchProject,
  exportBlockTransformsJson,
  exportLangOverlaysJson,
  exportPresentationJson,
  exportProjectJson,
  exportRecipeKjs,
  toExcelExportInterface,
  toStatusManifest
} from "../src/domain/exporters";
import { variantDiscriminator } from "../src/domain/identity";
import { normalizeSnapshot } from "../src/domain/normalize";
import { parseAssignmentPayload } from "../src/domain/parsers";
import { applyBlockTransformDocument, applyPresentationDocument } from "../src/domain/projectCompat";
import { catalogIdentityLabel, matchesCatalogQuery } from "../src/domain/catalogIdentity";
import { addBlockTransform, updateBlockTransform, updateItemPresentation } from "../src/domain/mutations";
import { presentationForItem } from "../src/domain/presentation";
import { applyItemCategoryDocument } from "../src/domain/categories";
import { mergeSnapshotEvidence } from "../src/domain/snapshotMerge";

const fixture = (name: string) => JSON.parse(readFileSync(fileURLToPath(new URL(`./fixtures/${name}`, import.meta.url)), "utf8"));

describe("item icons and spreadsheet categories", () => {
  it("preserves safe PNG data URLs and applies exact variant categories before registry categories", () => {
    const png = "data:image/png;base64,iVBORw0KGgo=";
    const base = buildWorkbenchProject({
      snapshot: { items: [{
        asset_key: "asset:tacz-ak47",
        registry_id: "tacz:modern_kinetic_gun",
        variant_discriminator: "GunId=tacz:ak47",
        client_name_zh_cn: "AK-47",
        icon_data_url: png
      }] },
      recipeData: { shaped: [], shapeless: [], custom: [] }
    });
    const project = applyItemCategoryDocument(base, {
      schema_version: "utd-item-categories/v1",
      categories: [
        { key: "generic", label_zh_cn: "通用", order: 1 },
        { key: "gun", label_zh_cn: "枪械", order: 2 }
      ],
      assignments: [
        { category_key: "generic", registry_id: "tacz:modern_kinetic_gun", level: 0 },
        { category_key: "gun", variant_discriminator: "GunId=tacz:ak47", level: 3 }
      ]
    });
    const item = project.items[0];
    expect(item.iconDataUrl).toBe(png);
    expect(item.categoryKey).toBe("gun");
    expect(item.categoryLabelZhCn).toBe("枪械");
    expect(item.categoryLevel).toBe(3);
    expect(matchesCatalogQuery(item, "枪械")).toBe(true);
    expect(toStatusManifest(project).items[0].category_key).toBe("gun");
  });

  it("drops non-PNG and oversized icon payloads", () => {
    const normalized = normalizeSnapshot({ items: [{
      registry_id: "minecraft:stick",
      client_name_zh_cn: "木棍",
      icon_data_url: "data:text/html;base64,PHNjcmlwdD4="
    }] });
    expect(normalized.items[0].iconDataUrl).toBe("");
  });

  it("merges game icons by semantic identity without shrinking the canonical directory", () => {
    const base = buildWorkbenchProject({
      snapshot: { items: [
        { asset_key: "canonical-stick", registry_id: "minecraft:stick", client_name_zh_cn: "木棍" },
        { asset_key: "canonical-stone", registry_id: "minecraft:stone", client_name_zh_cn: "石头" }
      ] },
      recipeData: { shaped: [], shapeless: [], custom: [] }
    });
    const icon = "data:image/png;base64,iVBORw0KGgo=";
    const merged = mergeSnapshotEvidence(base, { items: [{
      asset_key: "runtime-different-key",
      registry_id: "minecraft:stick",
      client_name_zh_cn: "木棍",
      icon_data_url: icon
    }] });
    expect(merged.matched).toBe(1);
    expect(merged.icons).toBe(1);
    expect(merged.project.items).toHaveLength(2);
    expect(merged.project.items.find((item) => item.registryId === "minecraft:stick")?.iconDataUrl).toBe(icon);
  });
});

const shaped = (
  id: string,
  output: string,
  input: { item?: string; tag?: string },
  outputName = output,
  outputIdentity?: string
) => ({
  id,
  pattern: ["A"],
  key: { A: input },
  output: { id: output, count: 1 },
  utd: {
    sheet: "tests",
    row: 1,
    station: "测试台",
    stationKey: "crafting",
    stationScope: "test",
    form: "有序",
    outputName,
    output,
    outputKeys: outputIdentity ? [output, outputIdentity] : [output],
    level: 1,
    count: 1
  }
});

const recipeData = {
  generatedAt: "2026-07-11T01:00:00Z",
  removeTypes: [],
  removeRecipeIds: [],
  removeOutputs: [],
  shaped: [
    shaped("kubejs:utd_test/baseball", "zombiekit:baseball_bat", { item: "minecraft:oak_slab" }, "实木棒球棍"),
    shaped("kubejs:utd_test/old_machine", "legacy:old_machine", { item: "minecraft:iron_ingot" }),
    shaped(
      "kubejs:utd_test/tagged",
      "firstpersonfoodeating:pack_food",
      { tag: "kubejs:utd_test/food_stock" },
      "高能量棒 A",
      "firstpersonfoodeating:pack_food{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}"
    )
  ],
  shapeless: [],
  custom: [
    {
      id: "kubejs:utd_test/ak47",
      json: {
        type: "tacz:gun_smith_table_crafting",
        materials: [{ item: { item: "minecraft:iron_ingot" }, count: 2 }],
        result: { type: "gun", id: "tacz:ak47" }
      },
      utd: {
        sheet: "guns",
        row: 2,
        station: "TaCZ配件工作台",
        stationKey: "gunsmith",
        stationScope: "gunsmith",
        form: "枪械",
        outputName: "AK-47",
        output: "tacz:ak47",
        outputKeys: ["tacz:ak47"],
        level: 3,
        count: 1
      }
    }
  ]
};

const lootRegistry = [
  {
    id: "firstpersonfoodeating:pack_food{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}",
    lootItemId: "firstpersonfoodeating:pack_food",
    lootNbt: "{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}",
    lootEnabled: true,
    level: 1,
    count: 1
  },
  {
    id: "firstpersonfoodeating:pack_food{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_b\"}}",
    lootItemId: "firstpersonfoodeating:pack_food",
    lootNbt: "{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_b\"}}",
    lootEnabled: false,
    level: 4,
    count: 1
  },
  {
    id: "tacz:modern_kinetic_gun{GunId:\"tacz:ak47\"}",
    lootItemId: "tacz:modern_kinetic_gun",
    lootNbt: "{GunId:\"tacz:ak47\"}",
    lootEnabled: false,
    level: 3,
    count: 1
  },
  {
    id: "tacz:modern_kinetic_gun{GunId:\"tacz:m4a1\"}",
    lootItemId: "tacz:modern_kinetic_gun",
    lootNbt: "{GunId:\"tacz:m4a1\"}",
    lootEnabled: true,
    level: 5,
    count: 1
  },
  {
    id: "legacy:loot_only",
    lootEnabled: true,
    level: 2,
    count: 1
  }
];

describe("snapshot normalization", () => {
  it("accepts the flat Mod snapshot and preserves canonical/SNBT strings", () => {
    const normalized = normalizeSnapshot(fixture("mod-export-flat.json"));
    expect(normalized.generatedAt).toBe("2026-07-11T09:30:00+08:00");
    expect(normalized.projectId).toBe("utd_asset_manager");
    expect(normalized.items).toHaveLength(5);
    expect(normalized.items[0].itemKey).toBe("asset:fpe-energy-a");
    expect(normalized.items[0].canonicalComponents).toEqual({
      encoding: "utd-nbt-canonical-v1",
      canonical: "firstpersonfoodeating_profile.food_id=firstpersonfoodeating:i_bang_a",
      snbt: "{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_bang_a\"}}"
    });
    expect(normalized.items[0].variantDiscriminator).toBe("food_id=firstpersonfoodeating:i_bang_a");
    expect(normalized.items[0].identityComponentsCanonical).toBe("food_id=firstpersonfoodeating:i_bang_a");
    expect(normalized.items[2].variantDiscriminator).toBe("GunId=tacz:ak47");
    expect(normalized.items[4].variantKey).toBeNull();
  });

  it("keeps compatibility with the old asset/status envelope", () => {
    const [item] = normalizeSnapshot(fixture("mod-export-envelope.json")).items;
    expect(item.itemKey).toBe("legacy:wrapped_asset");
    expect(item.deployedHash).toBe("legacy-deployed");
    expect(item.variantDiscriminator).toBe("GunId=tacz:legacy_test");
  });
});

describe("variant discriminator", () => {
  it("uses the exact Mod priority and returns a single value", () => {
    expect(variantDiscriminator("{food_id:'fpe:a',AttachmentId:'tacz:scope',AmmoId:'tacz:ammo',GunId:'tacz:ak47'}"))
      .toBe("GunId=tacz:ak47");
  });

  it("makes shared carrier variants distinguishable and searchable", () => {
    const project = buildWorkbenchProject({
      snapshot: fixture("mod-export-flat.json"),
      recipeData,
      lootRegistry
    });
    const ak = project.items.find((item) => item.itemKey === "asset:tacz-ak47")!;
    const label = catalogIdentityLabel(ak);
    expect(label).toEqual({ primary: "ak47", context: "GunId · tacz", exact: "GunId=tacz:ak47" });
    expect(matchesCatalogQuery(ak, "ak47")).toBe(true);
    expect(matchesCatalogQuery(ak, "GunId=tacz:ak47")).toBe(true);
    expect(matchesCatalogQuery(ak, "asset:tacz-ak47")).toBe(true);
    expect(matchesCatalogQuery(ak, "tacz:m4a1")).toBe(false);
  });
});

describe("catalog and filtered graph", () => {
  const project = buildWorkbenchProject({
    snapshot: fixture("mod-export-flat.json"),
    recipeData,
    lootRegistry
  });

  it("keeps release time separate from source timestamps", () => {
    const generatedAt = "2026-07-11T19:50:00+08:00";
    const release = buildWorkbenchProject({
      snapshot: fixture("mod-export-flat.json"),
      recipeData,
      lootRegistry,
      generatedAt
    });
    expect(release.manifest.generatedAt).toBe(generatedAt);
    expect(release.manifest.source.recipes.generatedAt).toBe(recipeData.generatedAt);
  });

  it("catalogs every recipe item ref and Loot identity", () => {
    expect(project.items.some((item) => item.itemKey === "legacy:old_machine" && item.recipeOutput)).toBe(true);
    expect(project.items.some((item) => item.itemKey === "minecraft:iron_ingot" && item.recipeInput)).toBe(true);
    expect(project.items.some((item) => item.itemKey === "legacy:loot_only" && item.lootEnabled)).toBe(true);
    expect(project.recipes).toHaveLength(4);
    expect(project.lootPolicies).toHaveLength(5);
  });

  it("keeps the visible graph at roots plus one-hop leaves and does not expand tags", () => {
    expect(project.graph.nodes.some((node) => node.ref === "legacy:old_machine")).toBe(false);
    expect(project.graph.nodes.some((node) => node.id === "tag:kubejs:utd_test/food_stock")).toBe(true);
    expect(project.items.some((item) => item.registryId === "kubejs:utd_test/food_stock")).toBe(false);
  });

  it("matches FPE and TaCZ variants exactly without base-id leakage", () => {
    const fpeA = project.items.find((item) => item.itemKey === "asset:fpe-energy-a")!;
    const fpeB = project.items.find((item) => item.itemKey === "asset:fpe-energy-b")!;
    const ak = project.items.find((item) => item.itemKey === "asset:tacz-ak47")!;
    const m4 = project.items.find((item) => item.itemKey === "asset:tacz-m4")!;
    expect([fpeA.lootEnabled, fpeA.lootLevel]).toEqual([true, 1]);
    expect([fpeB.lootEnabled, fpeB.lootLevel]).toEqual([false, null]);
    expect([ak.lootEnabled, ak.lootLevel]).toEqual([false, null]);
    expect([m4.lootEnabled, m4.lootLevel]).toEqual([true, 5]);
    expect([fpeA.recipeOutputCount, fpeB.recipeOutputCount]).toEqual([1, 0]);
    expect([ak.recipeOutputCount, m4.recipeOutputCount]).toEqual([1, 0]);
  });

  it("emits the frozen status field contract and auditable Excel strings", () => {
    const status = toStatusManifest(project);
    const item = status.items.find((entry) => entry.asset_key === "asset:fpe-energy-a")!;
    expect(item).toMatchObject({
      asset_key: "asset:fpe-energy-a",
      catalogued: true,
      human_selected: true,
      recipe_input_count: 0,
      recipe_output_count: 1,
      loot_enabled: true,
      loot_level: 1,
      sync_state: "stale",
      stale: true,
      variant_discriminator: "food_id=firstpersonfoodeating:i_bang_a"
    });
    const excel = toExcelExportInterface(project);
    const row = excel.sheets.find((sheet) => sheet.name === "Items")!.rows.find((entry) => entry.asset_key === "asset:fpe-energy-a")!;
    expect(String(row.canonical_components)).toContain("components_snbt".replace("components_", ""));
    expect(String(row.canonical_components)).toContain("i_bang_a");
  });

  it("round-trips generated KJS through the balanced assignment parser", () => {
    const kjs = exportRecipeKjs(project);
    const parsed = parseAssignmentPayload(kjs, "utd.recipeData = ") as { shaped: unknown[]; custom: unknown[] };
    expect(parsed.shaped).toHaveLength(3);
    expect(parsed.custom).toHaveLength(1);
  });
});

describe("cycle detection", () => {
  it("reports a managed root cycle", () => {
    const snapshot = {
      items: [
        { asset_key: "demo:a", registry_id: "demo:a", client_name_zh_cn: "A", translation_key: "item.demo.a" },
        { asset_key: "demo:b", registry_id: "demo:b", client_name_zh_cn: "B", translation_key: "item.demo.b" }
      ]
    };
    const recipes = {
      shaped: [
        shaped("kubejs:utd_cycle/a", "demo:a", { item: "demo:b" }),
        shaped("kubejs:utd_cycle/b", "demo:b", { item: "demo:a" })
      ],
      shapeless: [],
      custom: []
    };
    const project = buildWorkbenchProject({ snapshot, recipeData: recipes });
    expect(project.graph.cycles).toEqual([["demo:a", "demo:b"]]);
    expect(project.items.filter((item) => item.humanSelected).every((item) => item.sync === "error")).toBe(true);
  });

  it("does not treat recycling return edges as production cycles", () => {
    const snapshot = {
      items: [
        { asset_key: "demo:a", registry_id: "demo:a", client_name_zh_cn: "A" },
        { asset_key: "demo:b", registry_id: "demo:b", client_name_zh_cn: "B" }
      ]
    };
    const forward = shaped("kubejs:utd_cycle/forward", "demo:a", { item: "demo:b" });
    const recycling = shaped("kubejs:utd_cycle/recycling", "demo:b", { item: "demo:a" });
    recycling.utd.stationScope = "recycling";
    const project = buildWorkbenchProject({
      snapshot,
      recipeData: { shaped: [forward, recycling], shapeless: [], custom: [] }
    });
    expect(project.graph.cycles).toEqual([]);
    expect(project.issues.some((issue) => issue.code === "recipe_cycle")).toBe(false);
  });

  it("does not treat a shared carrier component upgrade as a self-cycle", () => {
    const project = buildWorkbenchProject({
      snapshot: { items: [{ asset_key: "demo:carrier", registry_id: "demo:carrier", client_name_zh_cn: "共用载体" }] },
      recipeData: {
        shaped: [],
        shapeless: [],
        custom: [{
          id: "kubejs:utd_cycle/component_upgrade",
          json: {
            type: "demo:upgrade",
            ingredients: [{ item: "demo:carrier", nbt: { BlockId: "demo:old" } }],
            result: { item: "demo:carrier", nbt: { BlockId: "demo:new" } }
          },
          utd: {
            station: "升级台",
            stationKey: "upgrade",
            stationScope: "upgrade",
            sheet: "workstations",
            outputName: "新工作台",
            output: "demo:carrier{BlockId:\"demo:new\"}",
            outputKeys: ["demo:carrier"],
            count: 1
          }
        }]
      }
    });
    expect(project.graph.cycles).toEqual([]);
    expect(project.issues.some((issue) => issue.code === "recipe_cycle")).toBe(false);
  });
});

describe("actionable issue policy", () => {
  it("accepts a captured display name when the translation key was not exported", () => {
    const project = buildWorkbenchProject({
      snapshot: { items: [{ asset_key: "demo:named", registry_id: "demo:named", client_name_zh_cn: "已捕获名称" }] },
      recipeData: { shaped: [], shapeless: [], custom: [] }
    });
    expect(project.issues.some((issue) => issue.code === "translation_key_missing")).toBe(false);
  });

  it("does not report a categorized workstation as an orphan asset", () => {
    const base = buildWorkbenchProject({
      snapshot: { items: [{ asset_key: "demo:station", registry_id: "demo:station", client_name_zh_cn: "测试工作台" }] },
      recipeData: { shaped: [], shapeless: [], custom: [] }
    });
    const project = applyItemCategoryDocument(base, {
      schema_version: "utd-item-categories/v1",
      categories: [{ key: "workstation", label_zh_cn: "工作台", order: 1 }],
      assignments: [{ category_key: "workstation", registry_id: "demo:station", level: 1 }]
    });
    expect(project.issues.some((issue) => issue.code === "managed_item_orphan")).toBe(false);
  });
});

describe("deployment contract", () => {
  it("publishes the short Mod status filename as a primary artifact", () => {
    const cliSource = readFileSync(fileURLToPath(new URL("../cli/index.ts", import.meta.url)), "utf8");
    expect(cliSource).toContain('"status_manifest.json": exportStatusJson(project)');
  });
});

describe("presentation overrides and block transforms", () => {
  it("keeps presentation edits separate and derives FPE language keys", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const changed = updateItemPresentation(base, "asset:fpe-energy-a", {
      enabled: true,
      nameZhCn: "测试能量棒",
      descriptionZhCn: "第一行\n第二行",
      applyScope: "identity"
    });
    const observed = changed.items.find((item) => item.itemKey === "asset:fpe-energy-a")!;
    expect(observed.clientNameZhCn).not.toBe("测试能量棒");
    expect(changed.presentations[0]).toMatchObject({
      nameKey: "item.firstpersonfoodeating.i_bang_a",
      descriptionKey: "tooltip.firstpersonfoodeating.i_bang_a.desc",
      nameZhCn: "测试能量棒",
      descriptionZhCn: "第一行\n第二行"
    });
    const presentationDocument = JSON.parse(exportPresentationJson(changed));
    expect(presentationDocument.overrides).toBeUndefined();
    expect(presentationDocument.drafts).toHaveLength(1);
    expect(presentationDocument.drafts[0]).toMatchObject({
      asset_key: "asset:fpe-energy-a",
      registry_id: "firstpersonfoodeating:pack_food",
      variant_discriminator: "food_id=firstpersonfoodeating:i_bang_a",
      apply_scope: "identity",
      observed_name_zh_cn: "高能量棒 A",
      name_zh_cn: "测试能量棒",
      description_zh_cn: ["第一行", "第二行"],
      enabled: true
    });
    const overlays = JSON.parse(exportLangOverlaysJson(changed));
    expect(overlays.namespaces.firstpersonfoodeating).toMatchObject({
      "item.firstpersonfoodeating.i_bang_a": "测试能量棒",
      "tooltip.firstpersonfoodeating.i_bang_a.desc": "第一行\n第二行"
    });
  });

  it("forces every FPE food presentation to remain identity-scoped", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const changed = updateItemPresentation(base, "asset:fpe-energy-a", {
      enabled: true,
      nameZhCn: "不能扩散到全部食品",
      applyScope: "registry"
    });
    expect(changed.presentations).toHaveLength(1);
    expect(changed.presentations[0]).toMatchObject({
      itemKey: "asset:fpe-energy-a",
      applyScope: "identity",
      variantDiscriminator: "food_id=firstpersonfoodeating:i_bang_a"
    });

    const imported = applyPresentationDocument(base, {
      schema_version: "utd-item-presentation/v1",
      drafts: [{
        asset_key: "asset:fpe-energy-b",
        registry_id: "firstpersonfoodeating:pack_food",
        variant_discriminator: "food_id=firstpersonfoodeating:i_bang_b",
        apply_scope: "registry",
        name_zh_cn: "导入时也必须收紧",
        description_zh_cn: [],
        enabled: true
      }]
    });
    expect(imported.presentations[0].applyScope).toBe("identity");
  });

  it("resolves and updates one registry presentation across sibling variants without duplicates", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const first = updateItemPresentation(base, "asset:tacz-ak47", {
      enabled: true,
      nameZhCn: "共享枪械名称",
      applyScope: "registry"
    });
    const m4 = first.items.find((item) => item.itemKey === "asset:tacz-m4")!;
    expect(presentationForItem(first, m4)).toMatchObject({
      applyScope: "registry",
      registryId: "tacz:modern_kinetic_gun",
      nameZhCn: "共享枪械名称"
    });

    const second = updateItemPresentation(first, "asset:tacz-m4", { nameZhCn: "共享枪械名称二版" });
    const registryRows = second.presentations.filter((entry) =>
      entry.applyScope === "registry" && entry.registryId === "tacz:modern_kinetic_gun"
    );
    expect(registryRows).toHaveLength(1);
    expect(registryRows[0]).toMatchObject({ itemKey: "asset:tacz-m4", nameZhCn: "共享枪械名称二版" });
    expect(JSON.parse(exportPresentationJson(second)).drafts.filter((entry: { apply_scope: string }) =>
      entry.apply_scope === "registry"
    )).toHaveLength(1);
  });

  it("creates an editable but disabled block transform with safe defaults", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const drafted = addBlockTransform(base, "asset:fpe-energy-a");
    const rule = drafted.blockTransforms[0];
    expect(rule).toMatchObject({
      enabled: false,
      inputSource: "clicked_hand",
      hand: "main",
      blockEntityPolicy: "reject",
      requireSneaking: false,
      cancelInteraction: true
    });
    expect(rule.catalyst.variantDiscriminator).toBe("food_id=firstpersonfoodeating:i_bang_a");
    expect(rule.catalystComponentsSnbt).toBe("{}");
    const edited = updateBlockTransform(drafted, rule.id, {
      clickedBlock: "minecraft:dirt",
      resultBlock: "minecraft:grass_block",
      catalystCount: 2,
      requireSneaking: true,
      enabled: true
    });
    const exported = JSON.parse(exportBlockTransformsJson(edited));
    expect(exported.block_transforms).toBeUndefined();
    expect(exported.rules[0]).toMatchObject({
      priority: 100,
      target: { block: "minecraft:dirt", state: {}, blockEntityPolicy: "reject" },
      result: { block: "minecraft:grass_block", state: {}, copyProperties: [] },
      activation: { requireSneak: true, hand: "main", allowFakePlayer: false },
      catalyst: {
        registryId: "firstpersonfoodeating:pack_food",
        variantDiscriminator: "food_id=firstpersonfoodeating:i_bang_a",
        count: 2,
        source: "clicked_hand",
        consume: true
      },
      creative: { requireInput: true, consume: false }
    });
  });

  it("keeps incomplete disabled drafts in the project but omits them from Java export", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const drafted = addBlockTransform(base, "asset:fpe-energy-a");

    expect(drafted.blockTransforms).toHaveLength(1);
    expect(drafted.issues).toContainEqual(expect.objectContaining({
      code: "block_transform_draft_incomplete",
      severity: "warning",
      entityId: drafted.blockTransforms[0].id
    }));
    expect(JSON.parse(exportProjectJson(drafted)).blockTransforms).toHaveLength(1);
    expect(JSON.parse(exportBlockTransformsJson(drafted)).rules).toEqual([]);
  });

  it("blocks runtime export when an incomplete draft is enabled", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const drafted = addBlockTransform(base, "asset:fpe-energy-a");
    const enabled = updateBlockTransform(drafted, drafted.blockTransforms[0].id, { enabled: true });
    const codes = enabled.issues.map((issue) => issue.code);

    expect(codes).toContain("block_transform_invalid_target_id");
    expect(codes).toContain("block_transform_invalid_result_id");
    expect(codes).not.toContain("block_transform_draft_incomplete");
    expect(() => exportBlockTransformsJson(enabled)).toThrow(/方块替换规则存在 2 个阻断错误/);
  });

  it("uses Java rule-id normalization for validation, duplicate detection, and export", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const imported = applyBlockTransformDocument(base, {
      schema_version: "utd-block-transforms/v1",
      rules: [{
        id: "UTD:block_transform/Upper_Case",
        enabled: false,
        target: { block: "minecraft:stone" },
        catalyst: { registryId: "minecraft:stick" },
        result: { block: "minecraft:cobblestone" }
      }]
    });
    expect(JSON.parse(exportBlockTransformsJson(imported)).rules[0].id)
      .toBe("utd:block_transform/upper_case");

    const invalid = applyBlockTransformDocument(base, {
      schema_version: "utd-block-transforms/v1",
      rules: [{
        id: "bad id!",
        enabled: true,
        target: { block: "minecraft:stone" },
        catalyst: { registryId: "minecraft:stick" },
        result: { block: "minecraft:cobblestone" }
      }]
    });
    expect(invalid.issues.map((issue) => issue.code)).toContain("block_transform_invalid_rule_id");
    expect(() => exportBlockTransformsJson(invalid)).toThrow(/bad id!/);

    const duplicates = applyBlockTransformDocument(base, {
      schema_version: "utd-block-transforms/v1",
      rules: [
        {
          id: "Case_Duplicate",
          enabled: false,
          target: { block: "minecraft:stone" },
          catalyst: { registryId: "minecraft:stick" },
          result: { block: "minecraft:cobblestone" }
        },
        {
          id: "case_duplicate",
          enabled: false,
          target: { block: "minecraft:dirt" },
          catalyst: { registryId: "minecraft:stick" },
          result: { block: "minecraft:grass_block" }
        }
      ]
    });
    expect(duplicates.issues.filter((issue) => issue.code === "block_transform_duplicate_id"))
      .toHaveLength(2);
    expect(JSON.parse(exportBlockTransformsJson(duplicates)).rules).toEqual([]);
  });

  it("imports the nested Java rule contract and round-trips every runtime field", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const imported = applyBlockTransformDocument(base, {
      schema: "utd-block-transforms/v1",
      rules: [{
        id: "utd:block_transform/runtime_rule",
        enabled: true,
        priority: 120,
        target: { block: "minecraft:oak_log", state: { axis: "y" }, blockEntityPolicy: "reject" },
        catalyst: {
          registryId: "firstpersonfoodeating:pack_food",
          variantDiscriminator: "food_id=firstpersonfoodeating:i_bang_a",
          componentsSnbt: "{food_id:\"firstpersonfoodeating:i_bang_a\"}",
          count: 2,
          source: "inventory",
          consume: true
        },
        activation: { hand: "any", requireSneak: true, allowFakePlayer: false },
        result: { block: "minecraft:stripped_oak_log", state: { axis: "y" }, copyProperties: ["axis"] },
        creative: { requireInput: true, consume: true }
      }]
    });
    expect(imported.blockTransforms[0]).toMatchObject({
      priority: 120,
      clickedBlock: "minecraft:oak_log",
      targetState: { axis: "y" },
      resultBlock: "minecraft:stripped_oak_log",
      resultState: { axis: "y" },
      copyProperties: ["axis"],
      inputSource: "inventory",
      hand: "any",
      requireSneaking: true,
      creativeRequireInput: true,
      creativeConsume: true
    });
    const exported = JSON.parse(exportBlockTransformsJson(imported));
    expect(exported.rules[0]).toMatchObject({
      priority: 120,
      target: { block: "minecraft:oak_log", state: { axis: "y" } },
      catalyst: { source: "inventory", componentsSnbt: "{food_id:\"firstpersonfoodeating:i_bang_a\"}" },
      activation: { hand: "any", requireSneak: true },
      result: { block: "minecraft:stripped_oak_log", state: { axis: "y" }, copyProperties: ["axis"] },
      creative: { requireInput: true, consume: true }
    });
  });

  it("validates deployable ids, counts, duplicate ids, conflicts, and unsafe inventory activation", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const imported = applyBlockTransformDocument(base, {
      schema_version: "utd-block-transforms/v1",
      rules: [
        {
          id: "duplicate",
          enabled: true,
          priority: 10,
          target: { block: "stone" },
          catalyst: { registryId: "coal", count: 0, source: "inventory" },
          result: { block: "dirt" }
        },
        {
          id: "duplicate",
          enabled: true,
          priority: 10,
          target: { block: "stone" },
          catalyst: { registryId: "minecraft:coal", count: 1 },
          result: { block: "minecraft:dirt" }
        }
      ]
    });
    const codes = imported.issues.map((issue) => issue.code);
    expect(codes).toContain("block_transform_duplicate_id");
    expect(codes).toContain("block_transform_invalid_target_id");
    expect(codes).toContain("block_transform_invalid_result_id");
    expect(codes).toContain("block_transform_invalid_catalyst_id");
    expect(codes).toContain("block_transform_invalid_count");
    expect(codes).toContain("block_transform_inventory_without_sneak");
    expect(codes).toContain("block_transform_priority_conflict");
  });

  it("automatically requires sneaking when the UI switches a rule to inventory input", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const drafted = addBlockTransform(base, "asset:fpe-energy-a");
    const changed = updateBlockTransform(drafted, drafted.blockTransforms[0].id, {
      inputSource: "inventory",
      requireSneaking: false
    });
    expect(changed.blockTransforms[0]).toMatchObject({ inputSource: "inventory", requireSneaking: true });
  });

  it("hydrates additive fields when an older v1 project is loaded", () => {
    const legacy = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry }) as unknown as Record<string, unknown>;
    delete legacy.presentations;
    legacy.blockTransforms = [{
      id: "utd:block_transform/legacy",
      clickedBlock: "minecraft:dirt",
      resultBlock: "minecraft:grass_block",
      catalyst: { ref: "firstpersonfoodeating:pack_food", count: 1 }
    }];
    const manifest = legacy.manifest as { counts: Record<string, unknown> };
    delete manifest.counts.presentations;
    delete manifest.counts.blockTransforms;
    assertWorkbenchProject(legacy);
    expect(legacy.presentations).toEqual([]);
    expect(legacy.blockTransforms).toEqual([expect.objectContaining({
      inputSource: "clicked_hand",
      hand: "main",
      blockEntityPolicy: "reject"
    })]);
    expect((legacy.manifest as { counts: Record<string, unknown> }).counts).toMatchObject({ presentations: 0, blockTransforms: 1 });
  });

  it("imports Mod drafts by semantic identity when runtime and canonical asset keys differ", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const imported = applyPresentationDocument(base, {
      schema_version: "utd-item-presentation/v1",
      producer: "utd_asset_manager",
      updated_at: "2026-07-12T02:00:00Z",
      drafts: [{
        asset_key: "runtime-sha-key",
        registry_id: "firstpersonfoodeating:pack_food",
        variant_discriminator: "food_id=firstpersonfoodeating:i_bang_a",
        apply_scope: "identity",
        observed_name_zh_cn: "高能量棒 A",
        name_zh_cn: "现场改名",
        description_zh_cn: ["第一行", "第二行"],
        enabled: true,
        base_catalog_hash: "runtime-catalog-hash",
        updated_at: "2026-07-12T01:59:00Z"
      }]
    });
    expect(imported.presentations).toEqual([expect.objectContaining({
      itemKey: "asset:fpe-energy-a",
      registryId: "firstpersonfoodeating:pack_food",
      variantDiscriminator: "food_id=firstpersonfoodeating:i_bang_a",
      nameZhCn: "现场改名",
      descriptionZhCn: "第一行\n第二行",
      baseCatalogHash: "runtime-catalog-hash",
      updatedAt: "2026-07-12T01:59:00Z"
    })]);
    expect(JSON.parse(exportPresentationJson(imported)).drafts[0].asset_key).toBe("asset:fpe-energy-a");
  });

  it("prefers an exact asset key and rejects an ambiguous semantic fallback", () => {
    const base = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry });
    const duplicate = structuredClone(base.items.find((item) => item.itemKey === "asset:fpe-energy-a")!);
    duplicate.itemKey = "asset:fpe-energy-a-duplicate";
    base.items.push(duplicate);
    const commonDraft = {
      registry_id: "firstpersonfoodeating:pack_food",
      variant_discriminator: "food_id=firstpersonfoodeating:i_bang_a",
      apply_scope: "identity",
      description_zh_cn: "兼容单字符串介绍",
      enabled: true
    };
    const exact = applyPresentationDocument(base, {
      schema_version: "utd-item-presentation/v1",
      drafts: [{ ...commonDraft, asset_key: "asset:fpe-energy-a", name_zh_cn: "精确" }]
    });
    expect(exact.presentations[0]).toMatchObject({
      itemKey: "asset:fpe-energy-a",
      descriptionZhCn: "兼容单字符串介绍"
    });
    expect(() => applyPresentationDocument(base, {
      schema_version: "utd-item-presentation/v1",
      drafts: [{ ...commonDraft, asset_key: "unknown-runtime-key", name_zh_cn: "歧义" }]
    })).toThrow(/ambiguous.*asset:fpe-energy-a.*asset:fpe-energy-a-duplicate/i);
  });

  it("hydrates array and string descriptions in canonical v1 projects", () => {
    const project = buildWorkbenchProject({ snapshot: fixture("mod-export-flat.json"), recipeData, lootRegistry }) as unknown as Record<string, unknown>;
    project.presentations = [{
      item_key: "asset:fpe-energy-a",
      apply_scope: "identity",
      enabled: true,
      name_zh_cn: "兼容草稿",
      description_zh_cn: ["数组一", "数组二"]
    }];
    assertWorkbenchProject(project);
    expect((project as unknown as { presentations: Array<{ descriptionZhCn: string }> }).presentations[0].descriptionZhCn)
      .toBe("数组一\n数组二");
  });

  it("documents the optional CLI presentation import", () => {
    const cliSource = readFileSync(fileURLToPath(new URL("../cli/index.ts", import.meta.url)), "utf8");
    expect(cliSource).toContain("--presentations <presentation_drafts.json>");
    expect(cliSource).toContain("applyPresentationDocument(project, presentations.value)");
    expect(cliSource).toContain("--block-transforms <utd_block_transforms.json>");
    expect(cliSource).toContain("applyBlockTransformDocument(project, blockTransforms.value)");
  });
});

describe("captured plain item reconciliation", () => {
  it("merges SHA-keyed plain captures with base recipe refs by unique registry id", () => {
    const ids = [
      "create:cogwheel",
      "create:large_cogwheel",
      "create:mechanical_mixer",
      "create:turntable"
    ];
    const snapshot = {
      items: ids.map((registryId, index) => ({
        asset_key: `sha256-captured-${index}`,
        variant_key: `sha256-plain-${index}`,
        registry_id: registryId,
        identity_kind: "plain",
        variant_discriminator: "",
        components_snbt: "{}",
        client_name_zh_cn: registryId
      }))
    };
    const recipes = {
      shaped: ids.map((registryId, index) => shaped(
        `kubejs:utd_test/plain_${index}`,
        registryId,
        { item: "minecraft:stone" }
      )),
      shapeless: [],
      custom: []
    };
    const project = buildWorkbenchProject({ snapshot, recipeData: recipes });
    for (const registryId of ids) {
      const rows = project.items.filter((item) =>
        item.registryId === registryId && item.variantDiscriminator === ""
      );
      expect(rows).toHaveLength(1);
      expect(rows[0]).toMatchObject({ identityKind: "plain", recipeOutput: true, recipeOutputCount: 1 });
    }
    const identities = project.items.map((item) => `${item.registryId}\u0000${item.variantDiscriminator}`);
    expect(new Set(identities).size).toBe(identities.length);
  });

  it("does not base-fallback a component-sensitive capture without a discriminator", () => {
    const project = buildWorkbenchProject({
      snapshot: { items: [{
        asset_key: "sha256-component-sensitive",
        variant_key: "sha256-component-variant",
        registry_id: "example:component_item",
        identity_kind: "components",
        variant_discriminator: "",
        components_snbt: "{Damage:1}"
      }] },
      recipeData: {
        shaped: [shaped("kubejs:utd_test/component_base", "example:component_item", { item: "minecraft:stone" })],
        shapeless: [],
        custom: []
      }
    });
    expect(project.items.filter((item) => item.registryId === "example:component_item")).toHaveLength(2);
  });
});

describe("FPE partial-NBT recipe normalization", () => {
  it("enriches an existing recipe variant with the matching Loot legacy SNBT", () => {
    const identity = "firstpersonfoodeating:pack_food{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_merge_test\"}}";
    const fullNbt = "{firstpersonfoodeating_profile:{food_id:\"firstpersonfoodeating:i_merge_test\",nutrition:6},food_id:\"firstpersonfoodeating:i_merge_test\"}";
    const project = buildWorkbenchProject({
      snapshot: { items: [] },
      recipeData: {
        shaped: [shaped("kubejs:utd_test/fpe_merge", "firstpersonfoodeating:pack_food", { item: "minecraft:paper" }, "合并测试", identity)],
        shapeless: [],
        custom: []
      },
      lootRegistry: [{
        id: identity,
        lootItemId: "firstpersonfoodeating:pack_food",
        lootNbt: fullNbt,
        lootEnabled: true,
        level: 1
      }]
    });
    const variant = project.items.find((item) => item.variantDiscriminator === "food_id=firstpersonfoodeating:i_merge_test")!;
    expect(variant.identityKind).toBe("recipe_variant");
    expect(variant.componentsSnbt).toBe(fullNbt);
    expect(variant.canonicalComponents).toMatchObject({ legacy_nbt: fullNbt });
  });

  it("keeps 56 recycling ingredients as 56 exact variants and merges Loot SNBT", () => {
    const fpeRecipes = Array.from({ length: 56 }, (_, index) => {
      const foodId = `firstpersonfoodeating:i_recycle_${String(index + 1).padStart(2, "0")}`;
      const nbt = `{firstpersonfoodeating_profile:{food_id:"${foodId}"},food_id:"${foodId}"}`;
      return {
        id: `kubejs:utd_firstperson_recycling/test_${index + 1}`,
        json: {
          type: "create:cutting",
          ingredients: [{ type: "forge:partial_nbt", item: "firstpersonfoodeating:pack_food", nbt }],
          results: [{ item: "minecraft:paper" }]
        },
        utd: {
          station: "FPE 回收",
          stationKey: "recycling",
          stationScope: "recycling",
          form: "create:cutting",
          outputKeys: ["minecraft:paper"]
        }
      };
    });
    const firstNbt = fpeRecipes[0].json.ingredients[0].nbt;
    const project = buildWorkbenchProject({
      snapshot: { items: [] },
      recipeData: { shaped: [], shapeless: [], custom: fpeRecipes },
      lootRegistry: [{
        id: `firstpersonfoodeating:pack_food${firstNbt}`,
        lootItemId: "firstpersonfoodeating:pack_food",
        lootNbt: firstNbt,
        lootEnabled: true,
        level: 1
      }]
    });
    const variants = project.items.filter((item) => item.registryId === "firstpersonfoodeating:pack_food");
    expect(variants).toHaveLength(56);
    expect(new Set(variants.map((item) => item.variantDiscriminator)).size).toBe(56);
    expect(variants.every((item) => item.recipeInputCount === 1)).toBe(true);
    const first = variants.find((item) => item.variantDiscriminator === "food_id=firstpersonfoodeating:i_recycle_01")!;
    expect(first.componentsSnbt).toBe(firstNbt);
    expect(first.lootEnabled).toBe(true);
  });
});
