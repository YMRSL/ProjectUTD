import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { buildWorkbenchProject } from "../src/domain/build";
import { exportRecipeKjs, toExcelExportInterface, toStatusManifest } from "../src/domain/exporters";
import { variantDiscriminator } from "../src/domain/identity";
import { normalizeSnapshot } from "../src/domain/normalize";
import { parseAssignmentPayload } from "../src/domain/parsers";
import { catalogIdentityLabel, matchesCatalogQuery } from "../src/domain/catalogIdentity";

const fixture = (name: string) => JSON.parse(readFileSync(fileURLToPath(new URL(`./fixtures/${name}`, import.meta.url)), "utf8"));

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
});

describe("deployment contract", () => {
  it("publishes the short Mod status filename as a primary artifact", () => {
    const cliSource = readFileSync(fileURLToPath(new URL("../cli/index.ts", import.meta.url)), "utf8");
    expect(cliSource).toContain('"status_manifest.json": exportStatusJson(project)');
  });
});
