import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import { customDataPath, defaultItemProperty } from "../src/domain/itemProperties";
import { refreshProject } from "../src/domain/mutations";
import type { FoodProperty, ItemPropertyOverride, JsonObject, TaczGunProperty, WorkbenchProject } from "../src/domain/schema";

export interface RuntimePropertyScanSummary {
  rarity: number;
  blockz: number;
  tacz: number;
  food: number;
  unresolvedGuns: string[];
}

export async function enrichPropertiesFromRuntime(
  project: WorkbenchProject,
  instanceRoot: string
): Promise<{ project: WorkbenchProject; summary: RuntimePropertyScanSummary }> {
  const next = structuredClone(project);
  const rarity = await scanRarityBase(path.join(instanceRoot, "config", "raritycore"));
  const rarityVariants = await scanRarityVariants(path.join(instanceRoot, "config", "raritycore", "item_data_matches"));
  const blockzRoot = await readJsonObject(path.join(instanceRoot, "config", "blockz", "grid_items.json"));
  const blockzItems = objectOr(blockzRoot.items);
  const guns = await scanTaczGuns(path.join(instanceRoot, "tacz"));
  const foodOverrides = await scanFoodOverrides(path.join(instanceRoot, "config", "firstpersonfoodeating", "utd_food_overrides.json"));
  const existing = new Map(next.itemProperties.map((entry) => [entry.itemKey, entry]));
  const summary: RuntimePropertyScanSummary = { rarity: 0, blockz: 0, tacz: 0, food: 0, unresolvedGuns: [] };

  for (const item of next.items.filter((entry) => entry.managed && entry.ownership === "utd")) {
    const row = existing.get(item.itemKey) ?? defaultItemProperty(item);
    let observed = existing.has(item.itemKey);
    const rarityValue = finiteNumber(rarityVariants.get(`${item.registryId}\u0000${item.variantDiscriminator}`) ?? rarity[item.registryId]);
    if (rarityValue !== null && Number.isInteger(rarityValue) && rarityValue >= 1 && rarityValue <= 7) {
      if (!row.enabled || !row.rarity) row.rarity = { value: rarityValue };
      summary.rarity += 1;
      observed = true;
    }
    const blockz = findBlockZVariant(blockzRoot, item.registryId, item.variantDiscriminator)
      ?? objectOr(blockzItems[item.registryId]);
    const width = finiteNumber(blockz.width);
    const height = finiteNumber(blockz.height);
    if (width !== null && height !== null) {
      if (!row.enabled || !row.blockz) {
        const baseBlockz = objectOr(blockzItems[item.registryId]);
        row.blockz = {
          width,
          height,
          capacityWidth: finiteNumber(baseBlockz.cap_width),
          capacityHeight: finiteNumber(baseBlockz.cap_height)
        };
      }
      summary.blockz += 1;
      observed = true;
    }
    const gunId = discriminator(item.variantDiscriminator, "GunId");
    if (gunId) {
      const gun = guns.get(gunId);
      if (gun) {
        row.tacz = row.enabled ? mergeTacz(row.tacz, gun) : gun;
        summary.tacz += 1;
        observed = true;
      } else {
        summary.unresolvedGuns.push(gunId);
      }
    }
    const foodId = discriminator(item.variantDiscriminator, "food_id");
    const food = foodOverrides.get(foodId || item.registryId);
    if (food && (!row.enabled || !row.food)) row.food = food;
    if (row.food) {
      summary.food += 1;
      observed = true;
    }
    if (observed) existing.set(item.itemKey, row);
  }
  next.itemProperties = [...existing.values()].sort((left, right) => left.itemKey.localeCompare(right.itemKey, "en"));
  next.manifest.counts.itemProperties = next.itemProperties.length;
  return { project: refreshProject(next, new Set()), summary };
}

async function scanTaczGuns(taczRoot: string): Promise<Map<string, TaczGunProperty>> {
  const result = new Map<string, TaczGunProperty>();
  const dataFiles = new Map<string, { pack: string; namespace: string; data: JsonObject }>();
  const indices = new Map<string, { pack: string; dataId: string }>();
  let packs: string[];
  try {
    packs = (await readdir(taczRoot, { withFileTypes: true }))
      .filter((entry) => entry.isDirectory())
      .map((entry) => entry.name)
      .sort((left, right) => left.localeCompare(right, "en"));
  } catch {
    return result;
  }
  for (const pack of packs) {
    const packRoot = path.join(taczRoot, pack);
    const files = await walkJson(packRoot);
    for (const dataFile of files.filter((file) => normalized(file).includes("/data/guns/"))) {
      const relative = normalized(path.relative(packRoot, dataFile));
      const match = /^data\/([^/]+)\/data\/guns\/(.+)\.json$/i.exec(relative);
      if (!match) continue;
      const data = await readJsonObject(dataFile);
      if (Object.keys(data).length) dataFiles.set(`${match[1]}:${match[2]}`, { pack, namespace: match[1], data });
    }
    for (const indexPath of files.filter((file) => normalized(file).includes("/index/guns/"))) {
      const relative = normalized(path.relative(packRoot, indexPath));
      const match = /^data\/([^/]+)\/index\/guns\/(.+)\.json$/i.exec(relative);
      if (!match) continue;
      const gunId = `${match[1]}:${match[2]}`;
      const index = await readJsonObject(indexPath);
      const dataId = typeof index.data === "string" ? index.data : "";
      if (splitResource(dataId).every(Boolean)) indices.set(gunId, { pack, dataId });
    }
  }
  for (const [gunId, index] of indices) {
    const data = dataFiles.get(index.dataId);
    if (data) result.set(gunId, gunFromData(gunId, index.pack, data.namespace, index.dataId, data.data));
  }
  return result;
}

async function scanRarityBase(root: string): Promise<JsonObject> {
  const merged = await readJsonObject(path.join(root, "FinalRarity.json"));
  const folder = path.join(root, "FinalRarityConfig");
  for (const file of (await walkJson(folder)).sort((left, right) => left.localeCompare(right, "en"))) {
    Object.assign(merged, await readJsonObject(file));
  }
  return merged;
}

async function scanRarityVariants(root: string): Promise<Map<string, number>> {
  const result = new Map<string, number>();
  for (const file of (await walkJson(root)).sort((left, right) => left.localeCompare(right, "en"))) {
    const rule = await readJsonObject(file);
    const itemId = typeof rule.item_id === "string" ? rule.item_id : "";
    const rarity = finiteNumber(rule.rarity);
    const conditions = Array.isArray(rule.conditions) ? rule.conditions.filter(isObject) : [];
    const condition = conditions.find((entry) => entry.type === "equals" && typeof entry.path === "string");
    if (!itemId || rarity === null || !condition) continue;
    const prefix = "components.minecraft:custom_data.";
    const conditionPath = String(condition.path);
    if (!conditionPath.startsWith(prefix)) continue;
    const key = discriminatorKeyFromPath(conditionPath.slice(prefix.length));
    const value = typeof condition.value === "string" ? condition.value : "";
    if (key && value) result.set(`${itemId}\u0000${key}=${value}`, rarity);
  }
  return result;
}

async function scanFoodOverrides(file: string): Promise<Map<string, FoodProperty>> {
  const root = await readJsonObject(file);
  const result = new Map<string, FoodProperty>();
  const foods = Array.isArray(root.foods) ? root.foods.filter(isObject) : [];
  for (const row of foods) {
    const foodId = typeof row.food_id === "string" ? row.food_id : "";
    if (!foodId) continue;
    const effects = Array.isArray(row.effects) ? row.effects.filter(isObject).map((effect) => ({
      id: typeof effect.id === "string" ? effect.id : "",
      durationTicks: Math.trunc(numberOr(effect.duration_ticks, 0)),
      amplifier: Math.trunc(numberOr(effect.amplifier, 0)),
      chance: numberOr(effect.chance, 1)
    })).filter((effect) => effect.id) : [];
    result.set(foodId, {
      foodId,
      nutrition: Math.trunc(numberOr(row.nutrition, 0)),
      saturation: numberOr(row.saturation, 0),
      thirstDelta: Math.trunc(numberOr(row.thirst_delta, 0)),
      waterDelta: Math.trunc(numberOr(row.water_delta, 0)),
      thirstMode: row.thirst_mode === "only" ? "only" : "always",
      effects
    });
  }
  return result;
}

function findBlockZVariant(root: JsonObject, registryId: string, variantDiscriminator: string): JsonObject | null {
  const separator = variantDiscriminator.indexOf("=");
  if (separator <= 0) return null;
  const expectedKey = customDataPath(variantDiscriminator.slice(0, separator));
  const expectedValue = variantDiscriminator.slice(separator + 1);
  const rules = Array.isArray(root.nbt_items) ? root.nbt_items.filter(isObject) : [];
  return rules.find((entry) => entry.id === registryId && entry.nbt_key === expectedKey && entry.nbt_value === expectedValue) ?? null;
}

function discriminatorKeyFromPath(value: string): string {
  return value === "firstpersonfoodeating_profile.food_id" ? "food_id" : value;
}

function gunFromData(gunId: string, pack: string, namespace: string, dataId: string, data: JsonObject): TaczGunProperty {
  const bullet = objectOr(data.bullet);
  const extra = objectOr(bullet.extra_damage);
  const reload = objectOr(data.reload);
  const feed = objectOr(reload.feed);
  const cooldown = objectOr(reload.cooldown);
  const inaccuracy = objectOr(data.inaccuracy);
  return {
    gunId,
    sourcePack: pack,
    sourceNamespace: namespace,
    sourceDataId: dataId,
    sourceData: structuredClone(data),
    damage: numberOr(bullet.damage, 0),
    ammoAmount: Math.trunc(numberOr(data.ammo_amount, 1)),
    rpm: Math.trunc(numberOr(data.rpm, 600)),
    reloadTacticalFeed: numberOr(feed.tactical, 0),
    reloadTacticalCooldown: numberOr(cooldown.tactical, 0),
    reloadEmptyFeed: numberOr(feed.empty, 0),
    reloadEmptyCooldown: numberOr(cooldown.empty, 0),
    inaccuracyStand: numberOr(inaccuracy.stand, 0),
    inaccuracyMove: numberOr(inaccuracy.move, 0),
    inaccuracySneak: numberOr(inaccuracy.sneak, 0),
    inaccuracyLie: numberOr(inaccuracy.lie, 0),
    inaccuracyAim: numberOr(inaccuracy.aim, 0),
    armorIgnore: numberOr(extra.armor_ignore, 0),
    pierce: Math.trunc(numberOr(bullet.pierce, 0)),
    bulletSpeed: numberOr(bullet.speed, 0),
    gravity: numberOr(bullet.gravity, 0)
  };
}

function mergeTacz(current: TaczGunProperty | null, observed: TaczGunProperty): TaczGunProperty {
  if (!current || !current.sourcePack) return observed;
  return {
    ...observed,
    ...current,
    sourcePack: observed.sourcePack,
    sourceNamespace: observed.sourceNamespace,
    sourceDataId: observed.sourceDataId,
    sourceData: observed.sourceData
  };
}

async function readJsonObject(file: string): Promise<JsonObject> {
  try {
    const text = await readFile(file, "utf8");
    return objectOr(JSON.parse(stripJsonComments(text).replace(/,\s*([}\]])/g, "$1")));
  } catch {
    return {};
  }
}

async function walkJson(root: string): Promise<string[]> {
  const result: string[] = [];
  const pending = [root];
  while (pending.length) {
    const current = pending.pop()!;
    let entries;
    try {
      entries = await readdir(current, { withFileTypes: true });
    } catch {
      continue;
    }
    for (const entry of entries) {
      const absolute = path.join(current, entry.name);
      if (entry.isDirectory()) pending.push(absolute);
      else if (entry.isFile() && entry.name.toLowerCase().endsWith(".json")) result.push(absolute);
    }
  }
  return result;
}

function objectOr(value: unknown): JsonObject {
  return value !== null && typeof value === "object" && !Array.isArray(value) ? value as JsonObject : {};
}

function isObject(value: unknown): value is JsonObject {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function finiteNumber(value: unknown): number | null {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function numberOr(value: unknown, fallback: number): number {
  return finiteNumber(value) ?? fallback;
}

function discriminator(value: string, key: string): string {
  return value.startsWith(`${key}=`) ? value.slice(key.length + 1) : "";
}

function splitResource(value: string): [string, string] {
  const separator = value.indexOf(":");
  return separator > 0 ? [value.slice(0, separator), value.slice(separator + 1)] : ["", ""];
}

function normalized(value: string): string {
  return value.replaceAll("\\", "/");
}

function stripJsonComments(text: string): string {
  let result = "";
  let inString = false;
  let escaped = false;
  for (let index = 0; index < text.length; index += 1) {
    const current = text[index];
    const next = text[index + 1];
    if (inString) {
      result += current;
      if (escaped) escaped = false;
      else if (current === "\\") escaped = true;
      else if (current === '"') inString = false;
      continue;
    }
    if (current === '"') {
      inString = true;
      result += current;
      continue;
    }
    if (current === "/" && next === "/") {
      while (index < text.length && text[index] !== "\n") index += 1;
      result += "\n";
      continue;
    }
    if (current === "/" && next === "*") {
      index += 2;
      while (index < text.length - 1 && !(text[index] === "*" && text[index + 1] === "/")) index += 1;
      index += 1;
      continue;
    }
    result += current;
  }
  return result;
}
