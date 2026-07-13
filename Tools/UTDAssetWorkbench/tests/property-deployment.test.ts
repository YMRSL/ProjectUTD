import { mkdtempSync, mkdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { describe, expect, it } from "vitest";
import { sampleProject } from "../src/data/sample";
import { updateItemProperty } from "../src/domain/mutations";
import { itemPropertyIntegrationFiles } from "../src/domain/itemProperties";
import { buildCandidatePackage } from "../src/domain/candidateCore";
import {
  deployPropertyCandidate,
  loadPropertyCandidate,
  planPropertyDeployment,
  rollbackPropertyDeployment
} from "../cli/propertyDeployment";

describe("property deployment", () => {
  it("refuses an empty property deployment instead of reporting false success", async () => {
    const instance = createInstance();
    await expect(deployPropertyCandidate(
      { project: sampleProject, sha256: "0".repeat(64), source: "test" },
      instance,
      { dryRun: true }
    )).rejects.toThrow(/enabled=0/);
    expect(existsSync(path.join(instance, "config"))).toBe(false);
  });

  it("verifies and loads the browser candidate ZIP before deployment", async () => {
    const directory = mkdtempSync(path.join(tmpdir(), "utd-property-candidate-"));
    const candidate = await buildCandidatePackage(sampleProject, "2026-07-13T11:00:00.000Z");
    const candidatePath = path.join(directory, candidate.filename);
    writeFileSync(candidatePath, candidate.bytes);
    const loaded = await loadPropertyCandidate(candidatePath);
    expect(loaded.project.manifest.projectId).toBe(sampleProject.manifest.projectId);
    expect(loaded.project.manifest.catalogHash).toBe(sampleProject.manifest.catalogHash);
    expect(loaded.sha256).toMatch(/^[a-f0-9]{64}$/);
  });

  it("deploys reviewed integrations without replacing unrelated BlockZ data and rolls back exactly", async () => {
    const instance = createInstance();
    const blockzPath = path.join(instance, "config", "blockz", "grid_items.json");
    mkdirSync(path.dirname(blockzPath), { recursive: true });
    const originalBlockz = JSON.stringify({ items: { "minecraft:shield": { width: 2, height: 2 } }, nbt_items: [] }, null, 2) + "\n";
    writeFileSync(blockzPath, originalBlockz);

    const ordinary = sampleProject.items.find((entry) => entry.registryId === "zombiekit:baseball_bat")!;
    const food = sampleProject.items.find((entry) => entry.registryId === "firstpersonfoodeating:pack_food")!;
    let project = updateItemProperty(sampleProject, ordinary.itemKey, {
      enabled: true,
      rarity: { value: 4 },
      blockz: { width: 2, height: 3, capacityWidth: null, capacityHeight: null }
    });
    project = updateItemProperty(project, food.itemKey, {
      enabled: true,
      rarity: { value: 3 },
      blockz: { width: 1, height: 2, capacityWidth: null, capacityHeight: null },
      food: {
        foodId: "firstpersonfoodeating:i_bang_a",
        nutrition: 7,
        saturation: 0.6,
        thirstDelta: 2,
        waterDelta: 4,
        thirstMode: "always",
        effects: [{ id: "minecraft:speed", durationTicks: 200, amplifier: 0, chance: 1 }]
      }
    });

    const loaded = { project, sha256: "a".repeat(64), source: "test" };
    const deployed = await deployPropertyCandidate(loaded, instance, { deployedAt: "2026-07-13T12:00:00.000Z" });
    expect(deployed.plan.counts).toMatchObject({ enabled: 2, rarity: 2, blockz: 2, food: 1 });

    const blockz = JSON.parse(readFileSync(blockzPath, "utf8"));
    expect(blockz.items["minecraft:shield"]).toEqual({ width: 2, height: 2 });
    expect(blockz.items[ordinary.registryId]).toEqual({ width: 2, height: 3 });
    expect(blockz.nbt_items[0]).toMatchObject({
      id: "firstpersonfoodeating:pack_food",
      nbt_key: "firstpersonfoodeating_profile.food_id",
      nbt_value: "firstpersonfoodeating:i_bang_a",
      width: 1,
      height: 2
    });
    const foodRuntime = JSON.parse(readFileSync(path.join(instance, "config", "firstpersonfoodeating", "utd_food_overrides.json"), "utf8"));
    expect(foodRuntime.foods[0]).toMatchObject({ food_id: "firstpersonfoodeating:i_bang_a", nutrition: 7, water_delta: 4 });
    expect(existsSync(path.join(instance, "config", "raritycore", "FinalRarityConfig", "utd_asset_workbench.json"))).toBe(true);
    expect(existsSync(path.join(instance, "config", "raritycore", "item_data_matches", "utd_asset_workbench"))).toBe(true);

    const rollback = await rollbackPropertyDeployment(instance);
    expect(rollback.deploymentId).toContain("2026-07-13T12-00-00");
    expect(readFileSync(blockzPath, "utf8")).toBe(originalBlockz);
    expect(existsSync(path.join(instance, "config", "firstpersonfoodeating", "utd_food_overrides.json"))).toBe(false);
    expect(existsSync(path.join(instance, "config", "utd_asset_manager", "property_deployment.json"))).toBe(false);
  });

  it("keeps dry-run side-effect free and emits native nested variant paths", async () => {
    const instance = createInstance();
    const food = sampleProject.items.find((entry) => entry.registryId === "firstpersonfoodeating:pack_food")!;
    const project = updateItemProperty(sampleProject, food.itemKey, {
      enabled: true,
      rarity: { value: 5 },
      blockz: { width: 1, height: 3, capacityWidth: null, capacityHeight: null }
    });
    const files = itemPropertyIntegrationFiles(project);
    const rarity = JSON.parse(files.find((entry) => entry.filename.includes("item_data_matches"))!.content);
    const blockz = JSON.parse(files.find((entry) => entry.filename.includes("grid_items"))!.content);
    expect(rarity.conditions[0].path).toBe("components.minecraft:custom_data.firstpersonfoodeating_profile.food_id");
    expect(blockz.nbt_items[0].nbt_key).toBe("firstpersonfoodeating_profile.food_id");

    const plan = await planPropertyDeployment(project, instance);
    expect(plan.writes.length).toBeGreaterThan(3);
    await deployPropertyCandidate({ project, sha256: "b".repeat(64), source: "test" }, instance, { dryRun: true });
    expect(existsSync(path.join(instance, "config"))).toBe(false);
  });
});

function createInstance(): string {
  const root = mkdtempSync(path.join(tmpdir(), "utd-property-deploy-"));
  writeFileSync(path.join(root, "options.txt"), "lang:zh_cn\n");
  mkdirSync(path.join(root, "mods"));
  return root;
}
