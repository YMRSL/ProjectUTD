import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import { defaultItemProperty } from "../src/domain/itemProperties";
import type { ItemPropertyOverride, JsonObject, TaczGunProperty, WorkbenchProject } from "../src/domain/schema";

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
  const rarity = await readJsonObject(path.join(instanceRoot, "config", "raritycore", "FinalRarity.json"));
  const blockzRoot = await readJsonObject(path.join(instanceRoot, "config", "blockz", "grid_items.json"));
  const blockzItems = objectOr(blockzRoot.items);
  const guns = await scanTaczGuns(path.join(instanceRoot, "tacz"));
  const existing = new Map(next.itemProperties.map((entry) => [entry.itemKey, entry]));
  const summary: RuntimePropertyScanSummary = { rarity: 0, blockz: 0, tacz: 0, food: 0, unresolvedGuns: [] };

  for (const item of next.items.filter((entry) => entry.managed && entry.ownership === "utd")) {
    const row = existing.get(item.itemKey) ?? defaultItemProperty(item);
    let observed = existing.has(item.itemKey);
    const rarityValue = finiteNumber(rarity[item.registryId]);
    if (rarityValue !== null && Number.isInteger(rarityValue) && rarityValue >= 1 && rarityValue <= 7) {
      row.rarity ??= { value: rarityValue };
      summary.rarity += 1;
      observed = true;
    }
    const blockz = objectOr(blockzItems[item.registryId]);
    const width = finiteNumber(blockz.width);
    const height = finiteNumber(blockz.height);
    if (width !== null && height !== null) {
      row.blockz ??= {
        width,
        height,
        capacityWidth: finiteNumber(blockz.cap_width),
        capacityHeight: finiteNumber(blockz.cap_height)
      };
      summary.blockz += 1;
      observed = true;
    }
    const gunId = discriminator(item.variantDiscriminator, "GunId");
    if (gunId) {
      const gun = guns.get(gunId);
      if (gun) {
        row.tacz = mergeTacz(row.tacz, gun);
        summary.tacz += 1;
        observed = true;
      } else {
        summary.unresolvedGuns.push(gunId);
      }
    }
    if (row.food) {
      summary.food += 1;
      observed = true;
    }
    if (observed) existing.set(item.itemKey, row);
  }
  next.itemProperties = [...existing.values()].sort((left, right) => left.itemKey.localeCompare(right.itemKey, "en"));
  next.manifest.counts.itemProperties = next.itemProperties.length;
  return { project: next, summary };
}

async function scanTaczGuns(taczRoot: string): Promise<Map<string, TaczGunProperty>> {
  const result = new Map<string, TaczGunProperty>();
  let packs: string[];
  try {
    packs = (await readdir(taczRoot, { withFileTypes: true }))
      .filter((entry) => entry.isDirectory() && entry.name !== "utd_workbench_pack")
      .map((entry) => entry.name)
      .sort((left, right) => left.localeCompare(right, "en"));
  } catch {
    return result;
  }
  for (const pack of packs) {
    const packRoot = path.join(taczRoot, pack);
    const files = await walkJson(packRoot);
    for (const indexPath of files.filter((file) => normalized(file).includes("/index/guns/"))) {
      const relative = normalized(path.relative(packRoot, indexPath));
      const match = /^data\/([^/]+)\/index\/guns\/(.+)\.json$/i.exec(relative);
      if (!match) continue;
      const gunId = `${match[1]}:${match[2]}`;
      const index = await readJsonObject(indexPath);
      const dataId = typeof index.data === "string" ? index.data : "";
      const [dataNamespace, dataPath] = splitResource(dataId);
      if (!dataNamespace || !dataPath) continue;
      const dataFile = path.join(packRoot, "data", dataNamespace, "data", "guns", ...dataPath.split("/")) + ".json";
      const data = await readJsonObject(dataFile);
      if (!Object.keys(data).length) continue;
      result.set(gunId, gunFromData(gunId, pack, dataNamespace, dataId, data));
    }
  }
  return result;
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
