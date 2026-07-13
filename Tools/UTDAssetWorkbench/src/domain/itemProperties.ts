import {
  ITEM_PROPERTY_SCHEMA,
  type CanonicalItem,
  type FoodEffectProperty,
  type ItemPropertyOverride,
  type JsonObject,
  type TaczGunProperty,
  type ValidationIssue,
  type WorkbenchProject
} from "./schema";
import { stableStringify } from "./stable";

const RESOURCE_ID = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;

export function propertyForItem(project: WorkbenchProject, item: CanonicalItem): ItemPropertyOverride {
  return project.itemProperties.find((entry) => entry.itemKey === item.itemKey) ?? defaultItemProperty(item);
}

export function defaultItemProperty(item: CanonicalItem): ItemPropertyOverride {
  const foodId = discriminatorValue(item.variantDiscriminator, "food_id");
  const gunId = discriminatorValue(item.variantDiscriminator, "GunId");
  return {
    itemKey: item.itemKey,
    registryId: item.registryId,
    variantDiscriminator: item.variantDiscriminator,
    enabled: false,
    rarity: null,
    blockz: null,
    tacz: gunId ? defaultTacz(gunId) : null,
    food: foodId ? {
      foodId,
      nutrition: numberFromSnbt(item.componentsSnbt, "nutrition", 0),
      saturation: numberFromSnbt(item.componentsSnbt, "saturation", 0),
      thirstDelta: numberFromSnbt(item.componentsSnbt, "delta", 0, "thirst"),
      waterDelta: numberFromSnbt(item.componentsSnbt, "water_delta", 0),
      thirstMode: stringFromSnbt(item.componentsSnbt, "mode") === "only" ? "only" : "always",
      effects: effectsFromSnbt(item.componentsSnbt)
    } : null,
    baseCatalogHash: item.catalogHash,
    updatedAt: new Date().toISOString()
  };
}

export function validateItemProperties(rows: ItemPropertyOverride[]): ValidationIssue[] {
  const issues: ValidationIssue[] = [];
  const ids = new Set<string>();
  for (const row of rows) {
    const add = (code: string, message: string) => issues.push({
      code,
      severity: "error",
      message,
      entityType: "item",
      entityId: row.itemKey
    });
    if (ids.has(row.itemKey)) add("item_property_duplicate", "Duplicate item property identity.");
    ids.add(row.itemKey);
    if (!row.enabled) continue;
    if (!row.rarity && !row.blockz && !row.tacz && !row.food) add("item_property_empty", "Enabled property row has no integration values.");
    if (row.rarity && (!Number.isInteger(row.rarity.value) || row.rarity.value < 1 || row.rarity.value > 7)) {
      add("item_property_rarity_range", "RarityCore rarity must be an integer from 1 to 7.");
    }
    if (row.blockz) {
      if (!positiveInteger(row.blockz.width, 20) || !positiveInteger(row.blockz.height, 20)) {
        add("item_property_blockz_size", "BlockZ width and height must be integers from 1 to 20.");
      }
      const capacityValues = [row.blockz.capacityWidth, row.blockz.capacityHeight];
      if (capacityValues.some((value) => value !== null) && !capacityValues.every((value) => value !== null && positiveInteger(value, 30))) {
        add("item_property_blockz_capacity", "BlockZ capacity width and height must both be blank or integers from 1 to 30.");
      }
      if (row.variantDiscriminator && capacityValues.some((value) => value !== null)) {
        add("item_property_blockz_variant_capacity", "BlockZ backpack capacity is registry-wide; variant rows may only override occupied width and height.");
      }
    }
    if (row.tacz) validateTacz(row.tacz, add);
    if (row.food) {
      if (!RESOURCE_ID.test(row.food.foodId)) add("item_property_food_id", "Food id must use namespace:path.");
      if (!integerIn(row.food.nutrition, 0, 100)) add("item_property_food_nutrition", "Nutrition must be an integer from 0 to 100.");
      if (!finiteIn(row.food.saturation, 0, 100)) add("item_property_food_saturation", "Saturation must be from 0 to 100.");
      if (!integerIn(row.food.thirstDelta, -100, 100) || !integerIn(row.food.waterDelta, -100, 100)) {
        add("item_property_food_thirst", "Thirst and water deltas must be integers from -100 to 100.");
      }
      for (const effect of row.food.effects) validateEffect(effect, add);
    }
  }
  return issues;
}

export function toItemPropertyDocument(project: WorkbenchProject): JsonObject {
  const errors = validateItemProperties(project.itemProperties).filter((issue) => issue.severity === "error");
  if (errors.length) throw new Error(`物品属性存在 ${errors.length} 个阻断错误：${errors.slice(0, 4).map((entry) => entry.message).join("；")}`);
  return {
    schema_version: ITEM_PROPERTY_SCHEMA,
    project_id: project.manifest.projectId,
    generated_at: project.manifest.generatedAt,
    properties: project.itemProperties.map((entry) => ({
      asset_key: entry.itemKey,
      registry_id: entry.registryId,
      variant_discriminator: entry.variantDiscriminator,
      enabled: entry.enabled,
      rarity: entry.rarity ? { value: entry.rarity.value } : null,
      blockz: entry.blockz ? {
        width: entry.blockz.width,
        height: entry.blockz.height,
        capacity_width: entry.blockz.capacityWidth,
        capacity_height: entry.blockz.capacityHeight
      } : null,
      tacz: entry.tacz ? {
        gun_id: entry.tacz.gunId,
        source_pack: entry.tacz.sourcePack,
        source_namespace: entry.tacz.sourceNamespace,
        source_data_id: entry.tacz.sourceDataId,
        source_data: entry.tacz.sourceData,
        damage: entry.tacz.damage,
        ammo_amount: entry.tacz.ammoAmount,
        rpm: entry.tacz.rpm,
        reload: {
          tactical_feed: entry.tacz.reloadTacticalFeed,
          tactical_cooldown: entry.tacz.reloadTacticalCooldown,
          empty_feed: entry.tacz.reloadEmptyFeed,
          empty_cooldown: entry.tacz.reloadEmptyCooldown
        },
        inaccuracy: {
          stand: entry.tacz.inaccuracyStand,
          move: entry.tacz.inaccuracyMove,
          sneak: entry.tacz.inaccuracySneak,
          lie: entry.tacz.inaccuracyLie,
          aim: entry.tacz.inaccuracyAim
        },
        armor_ignore: entry.tacz.armorIgnore,
        pierce: entry.tacz.pierce,
        bullet_speed: entry.tacz.bulletSpeed,
        gravity: entry.tacz.gravity
      } : null,
      food: entry.food ? {
        food_id: entry.food.foodId,
        nutrition: entry.food.nutrition,
        saturation: entry.food.saturation,
        thirst_delta: entry.food.thirstDelta,
        water_delta: entry.food.waterDelta,
        thirst_mode: entry.food.thirstMode,
        effects: entry.food.effects.map((effect) => ({
          id: effect.id,
          duration_ticks: effect.durationTicks,
          amplifier: effect.amplifier,
          chance: effect.chance
        }))
      } : null,
      base_catalog_hash: entry.baseCatalogHash,
      updated_at: entry.updatedAt
    }))
  };
}

export function exportItemPropertiesJson(project: WorkbenchProject): string {
  return stableStringify(toItemPropertyDocument(project), 2) + "\n";
}

export function toBlockZGridDocument(project: WorkbenchProject): JsonObject {
  const items: JsonObject = {};
  const nbtItems: JsonObject[] = [];
  for (const entry of project.itemProperties.filter((row) => row.enabled && row.blockz)) {
    const blockz = entry.blockz!;
    if (entry.variantDiscriminator) {
      const [key, value] = splitDiscriminator(entry.variantDiscriminator);
      if (!key || !value) continue;
      nbtItems.push({
        id: entry.registryId,
        nbt_key: customDataPath(key),
        nbt_value: value,
        width: blockz.width,
        height: blockz.height
      });
      continue;
    }
    items[entry.registryId] = {
      width: blockz.width,
      height: blockz.height,
      ...(blockz.capacityWidth !== null && blockz.capacityHeight !== null
        ? { cap_width: blockz.capacityWidth, cap_height: blockz.capacityHeight }
        : {})
    };
  }
  return {
    items,
    ...(nbtItems.length ? { nbt_items: nbtItems } : {})
  };
}

export interface ItemPropertyIntegrationFile {
  filename: string;
  content: string;
}

/** Concrete adapter candidates; deployment merges these beside third-party sources. */
export function itemPropertyIntegrationFiles(project: WorkbenchProject): ItemPropertyIntegrationFile[] {
  const enabled = project.itemProperties.filter((entry) => entry.enabled);
  const files: ItemPropertyIntegrationFile[] = [];
  const baseRarity: JsonObject = {};
  for (const entry of enabled.filter((row) => row.rarity)) {
    if (!entry.variantDiscriminator) {
      baseRarity[entry.registryId] = entry.rarity!.value;
      continue;
    }
    const [key, value] = splitDiscriminator(entry.variantDiscriminator);
    if (!key || !value) continue;
    const path = `components.minecraft:custom_data.${customDataPath(key)}`;
    files.push(jsonFile(`integrations/raritycore/item_data_matches/${safeFile(entry.itemKey)}.json`, {
      item_id: entry.registryId,
      conditions: [{ path, type: "equals", value }],
      rarity: entry.rarity!.value,
      priority: 1000,
      enabled: true,
      fuzzy_match: false,
      description: `UTD Asset Workbench: ${entry.variantDiscriminator}`
    }));
  }
  if (Object.keys(baseRarity).length) {
    files.push(jsonFile("integrations/raritycore/FinalRarityConfig/utd_asset_workbench.json", baseRarity));
  }
  if (enabled.some((entry) => entry.blockz)) {
    files.push(jsonFile("integrations/blockz/grid_items.utd-overrides.json", toBlockZGridDocument(project)));
  }
  for (const entry of enabled.filter((row) => row.tacz)) {
    const gun = entry.tacz!;
    const [namespace, dataPath] = splitResource(gun.sourceDataId);
    if (!namespace || !dataPath || !Object.keys(gun.sourceData).length) continue;
    const data = structuredClone(gun.sourceData);
    data.ammo_amount = gun.ammoAmount;
    data.rpm = gun.rpm;
    const bullet = ensureObject(data, "bullet");
    bullet.damage = gun.damage;
    bullet.speed = gun.bulletSpeed;
    bullet.gravity = gun.gravity;
    bullet.pierce = gun.pierce;
    const extra = ensureObject(bullet, "extra_damage");
    extra.armor_ignore = gun.armorIgnore;
    const reload = ensureObject(data, "reload");
    const feed = ensureObject(reload, "feed");
    feed.tactical = gun.reloadTacticalFeed;
    feed.empty = gun.reloadEmptyFeed;
    const cooldown = ensureObject(reload, "cooldown");
    cooldown.tactical = gun.reloadTacticalCooldown;
    cooldown.empty = gun.reloadEmptyCooldown;
    const inaccuracy = ensureObject(data, "inaccuracy");
    inaccuracy.stand = gun.inaccuracyStand;
    inaccuracy.move = gun.inaccuracyMove;
    inaccuracy.sneak = gun.inaccuracySneak;
    inaccuracy.lie = gun.inaccuracyLie;
    inaccuracy.aim = gun.inaccuracyAim;
    files.push(jsonFile(
      `integrations/tacz/utd_workbench_pack/data/${safePath(namespace)}/data/guns/${safeResourcePath(dataPath)}.json`,
      data
    ));
  }
  const foods = enabled.filter((entry) => entry.food).map((entry) => ({
    asset_key: entry.itemKey,
    registry_id: entry.registryId,
    variant_discriminator: entry.variantDiscriminator,
    food_id: entry.food!.foodId,
    nutrition: entry.food!.nutrition,
    saturation: entry.food!.saturation,
    thirst_delta: entry.food!.thirstDelta,
    water_delta: entry.food!.waterDelta,
    thirst_mode: entry.food!.thirstMode,
    effects: entry.food!.effects.map((effect) => ({
      id: effect.id,
      duration_ticks: effect.durationTicks,
      amplifier: effect.amplifier,
      chance: effect.chance
    }))
  }));
  if (foods.length) {
    files.push(jsonFile("integrations/firstpersonfoodeating/utd_food_overrides.json", {
      schema_version: "utd-food-property-overrides/v1",
      foods
    }));
  }
  return files.sort((left, right) => left.filename.localeCompare(right.filename, "en"));
}

function defaultTacz(gunId: string): TaczGunProperty {
  const [namespace = "tacz", path = ""] = gunId.split(":", 2);
  return {
    gunId,
    sourcePack: "",
    sourceNamespace: namespace,
    sourceDataId: `${namespace}:${path}_data`,
    sourceData: {},
    damage: 0,
    ammoAmount: 1,
    rpm: 600,
    reloadTacticalFeed: 1,
    reloadTacticalCooldown: 1,
    reloadEmptyFeed: 1,
    reloadEmptyCooldown: 1,
    inaccuracyStand: 0,
    inaccuracyMove: 0,
    inaccuracySneak: 0,
    inaccuracyLie: 0,
    inaccuracyAim: 0,
    armorIgnore: 0,
    pierce: 0,
    bulletSpeed: 0,
    gravity: 0
  };
}

function validateTacz(value: TaczGunProperty, add: (code: string, message: string) => void): void {
  if (!RESOURCE_ID.test(value.gunId)) add("item_property_tacz_id", "TaCZ gun id must use namespace:path.");
  if (!value.sourcePack || !RESOURCE_ID.test(value.sourceDataId) || !Object.keys(value.sourceData).length) {
    add("item_property_tacz_source", "TaCZ gun must be linked to a scanned source data file before promotion.");
  }
  if (!finiteIn(value.damage, 0, 100000)) add("item_property_tacz_damage", "TaCZ damage must be from 0 to 100000.");
  if (!integerIn(value.ammoAmount, 1, 10000)) add("item_property_tacz_ammo", "TaCZ magazine capacity must be an integer from 1 to 10000.");
  if (!integerIn(value.rpm, 1, 1200)) add("item_property_tacz_rpm", "TaCZ rpm must be an integer from 1 to 1200.");
  if (![value.reloadTacticalFeed, value.reloadTacticalCooldown, value.reloadEmptyFeed, value.reloadEmptyCooldown]
    .every((entry) => finiteIn(entry, 0, 120))) add("item_property_tacz_reload", "TaCZ reload values must be from 0 to 120 seconds.");
  if (![value.inaccuracyStand, value.inaccuracyMove, value.inaccuracySneak, value.inaccuracyLie, value.inaccuracyAim]
    .every((entry) => finiteIn(entry, 0, 180))) add("item_property_tacz_inaccuracy", "TaCZ inaccuracy values must be from 0 to 180.");
  if (!finiteIn(value.armorIgnore, 0, 1)) add("item_property_tacz_armor", "TaCZ armor ignore must be from 0 to 1.");
  if (!integerIn(value.pierce, 0, 100)) add("item_property_tacz_pierce", "TaCZ pierce must be an integer from 0 to 100.");
  if (!finiteIn(value.bulletSpeed, 0, 10000) || !finiteIn(value.gravity, 0, 100)) {
    add("item_property_tacz_ballistics", "TaCZ bullet speed/gravity is outside the supported range.");
  }
}

function validateEffect(effect: FoodEffectProperty, add: (code: string, message: string) => void): void {
  if (!RESOURCE_ID.test(effect.id)) add("item_property_food_effect_id", `Invalid food effect id: ${effect.id || "<blank>"}.`);
  if (!integerIn(effect.durationTicks, 0, 20_000_000) || !integerIn(effect.amplifier, 0, 255) || !finiteIn(effect.chance, 0, 1)) {
    add("item_property_food_effect_value", `Invalid food effect values for ${effect.id || "<blank>"}.`);
  }
}

function effectsFromSnbt(snbt: string): FoodEffectProperty[] {
  const body = /effects:\[(.*?)\](?:,|})/s.exec(snbt)?.[1] ?? "";
  return [...body.matchAll(/\{[^{}]*id:\"([^\"]+)\"[^{}]*duration_ticks:([-+]?\d+)[^{}]*amplifier:([-+]?\d+)[^{}]*chance:([-+]?\d+(?:\.\d+)?)(?:f|d)?[^{}]*}/g)]
    .map((match) => ({ id: match[1], durationTicks: Number(match[2]), amplifier: Number(match[3]), chance: Number(match[4]) }));
}

function numberFromSnbt(snbt: string, key: string, fallback: number, scope?: string): number {
  const source = scope ? new RegExp(`${scope}:\\{([^}]*)}`, "s").exec(snbt)?.[1] ?? "" : snbt;
  const match = new RegExp(`(?:^|[,\\{])${key}:([-+]?\\d+(?:\\.\\d+)?)(?:[bslfd])?(?:[,\\}])`, "i").exec(source);
  return match ? Number(match[1]) : fallback;
}

function stringFromSnbt(snbt: string, key: string): string {
  return new RegExp(`(?:^|[,\\{])${key}:\"([^\"]*)\"`).exec(snbt)?.[1] ?? "";
}

function discriminatorValue(value: string, key: string): string {
  return value.startsWith(`${key}=`) ? value.slice(key.length + 1) : "";
}

function positiveInteger(value: number, max: number): boolean {
  return integerIn(value, 1, max);
}

function integerIn(value: number, min: number, max: number): boolean {
  return Number.isInteger(value) && value >= min && value <= max;
}

function finiteIn(value: number, min: number, max: number): boolean {
  return Number.isFinite(value) && value >= min && value <= max;
}

function jsonFile(filename: string, value: JsonObject): ItemPropertyIntegrationFile {
  return { filename, content: stableStringify(value, 2) + "\n" };
}

function ensureObject(parent: JsonObject, key: string): JsonObject {
  const current = parent[key];
  if (current !== null && typeof current === "object" && !Array.isArray(current)) return current;
  const created: JsonObject = {};
  parent[key] = created;
  return created;
}

function splitDiscriminator(value: string): [string, string] {
  const separator = value.indexOf("=");
  return separator > 0 ? [value.slice(0, separator), value.slice(separator + 1)] : ["", ""];
}

/** Maps stable Workbench discriminators onto their actual custom-data location. */
export function customDataPath(key: string): string {
  return key === "food_id" ? "firstpersonfoodeating_profile.food_id" : key;
}

function splitResource(value: string): [string, string] {
  const separator = value.indexOf(":");
  return separator > 0 ? [value.slice(0, separator), value.slice(separator + 1)] : ["", ""];
}

function safeFile(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9_.-]+/g, "_") || "item";
}

function safePath(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9_.-]+/g, "_") || "utd";
}

function safeResourcePath(value: string): string {
  return value.split("/").map(safeFile).join("/");
}
