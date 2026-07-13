import { describe, expect, it } from "vitest";
import { buildWorkbenchProject } from "../src/domain/build";
import { cleanIdentityCatalog } from "../src/domain/identityCleanup";

describe("identity cleanup", () => {
  it("maps input-only TaCZ logical IDs onto the carrier variant", () => {
    const project = buildWorkbenchProject({
      snapshot: { items: [] },
      recipeData: {
        shaped: [],
        shapeless: [],
        custom: [{
          id: "kubejs:test/recycle_m107",
          json: {
            type: "create:crushing",
            ingredients: [{ type: "forge:partial_nbt", item: "tacz:modern_kinetic_gun", nbt: { GunId: "tacz:m107" } }],
            results: [{ item: "minecraft:iron_nugget" }]
          },
          utd: { sheet: "gun_recycling", stationScope: "recycling", outputName: "test" }
        }]
      },
      lootRegistry: [{ id: "tacz:m107", lootEnabled: false, level: 4, count: 1 }]
    });
    const policy = project.lootPolicies[0];
    expect(policy.registryId).toBe("tacz:modern_kinetic_gun");
    expect(policy.variantDiscriminator).toBe("GunId=tacz:m107");
    expect(project.items.filter((item) => item.registryId === "tacz:m107")).toHaveLength(0);
    expect(project.items.find((item) => item.variantDiscriminator === "GunId=tacz:m107")?.managed).toBe(true);
  });

  it("removes confirmed retired identities and every recipe that references them", () => {
    const project = buildWorkbenchProject({
      snapshot: { items: [{ asset_key: "old:item", registry_id: "old:item", client_name_zh_cn: "旧物品" }] },
      recipeData: {
        shaped: [{
          id: "kubejs:test/old",
          pattern: ["A"],
          key: { A: { item: "minecraft:stick" } },
          output: { id: "old:item", count: 1 },
          utd: { sheet: "test", stationScope: "test", outputName: "旧物品" }
        }],
        shapeless: [],
        custom: []
      },
      lootRegistry: [{ id: "old:item", lootEnabled: true, level: 1, count: 1 }]
    });
    const cleaned = cleanIdentityCatalog(project, ["old:item"]);
    expect(cleaned.project.items.some((item) => item.registryId === "old:item")).toBe(false);
    expect(cleaned.project.recipes).toHaveLength(0);
    expect(cleaned.project.lootPolicies).toHaveLength(0);
    expect(cleaned.summary.removedRecipes).toBe(1);
  });
});
