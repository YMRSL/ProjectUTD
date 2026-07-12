import type {
  CanonicalItem,
  CanonicalLootPolicy,
  CanonicalRecipe,
  CanonicalRef,
  JsonObject,
  JsonValue,
  RecipeSourceKind,
  SourcePolicies
} from "./schema";
import { unwrapSnapshot } from "./parsers";
import { variantDiscriminator } from "./identity";
import {
  asJsonObject,
  displayNameFromId,
  fingerprint,
  isRecord,
  namespaceOf,
  numberOr,
  registryIdFromIdentity,
  stableStringify,
  stringArray,
  stringOr
} from "./stable";

export interface NormalizedRecipeSource {
  generatedAt?: string;
  recipes: CanonicalRecipe[];
  policies: SourcePolicies;
}

export function normalizeSnapshot(input: unknown): {
  items: CanonicalItem[];
  generatedAt?: string;
  projectId?: string;
} {
  const envelope = unwrapSnapshot(input);
  const items = envelope.items.map((entry, index) => normalizeSnapshotItem(entry, index));
  return { ...envelope, items };
}

function normalizeSnapshotItem(entry: unknown, index: number): CanonicalItem {
  if (!isRecord(entry)) throw new Error(`Snapshot item #${index + 1} is not an object.`);
  const record = isRecord(entry.asset) ? entry.asset : entry;
  const status = isRecord(entry.status) ? entry.status : {};
  const registryId = stringOr(record.registryId ?? record.registry_id ?? record.id).trim();
  if (!registryId || !registryId.includes(":")) {
    throw new Error(`Snapshot item #${index + 1} has invalid registry_id: ${registryId || "<blank>"}`);
  }
  const canonicalComponents = normalizeSnapshotComponents(record);
  const componentsCanonical = stringOr(record.componentsCanonical ?? record.components_canonical)
    || (isRecord(record.canonicalComponents ?? record.canonical_components ?? record.components)
      ? stableStringify(record.canonicalComponents ?? record.canonical_components ?? record.components)
      : "");
  const componentsSnbt = stringOr(record.componentsSnbt ?? record.components_snbt);
  const identityComponentsCanonical = stringOr(
    record.identityComponentsCanonical ?? record.identity_components_canonical
  );
  const canonicalVariant = ((record.canonicalVariant ?? record.canonical_variant ?? record.variant) ?? null) as JsonValue;
  const suppliedVariantKey = stringOr(record.variantKey ?? record.variant_key).trim();
  const suppliedItemKey = stringOr(record.assetKey ?? record.asset_key ?? record.itemKey ?? record.item_key ?? record.key).trim();
  const declaredIdentityKind = stringOr(record.identityKind ?? record.identity_kind);
  const discriminator = stringOr(record.variantDiscriminator ?? record.variant_discriminator)
    || variantDiscriminator([identityComponentsCanonical, canonicalVariant, canonicalComponents, suppliedVariantKey]);
  const declaredVariant = declaredIdentityKind
    ? !["item", "plain", "base"].includes(declaredIdentityKind.toLocaleLowerCase())
    : canonicalVariant !== null || isRecord(record.canonicalComponents ?? record.canonical_components ?? record.components);
  const hasStableVariant = Boolean(suppliedVariantKey || discriminator || declaredVariant);
  const variantKey = suppliedVariantKey || (hasStableVariant
    ? fingerprint({ discriminator, identityComponentsCanonical, canonicalVariant })
    : "");
  const itemKey = suppliedItemKey || (variantKey ? `${registryId}::${variantKey}` : registryId);
  const identityKind = declaredIdentityKind || (hasStableVariant ? "variant" : "item");
  const clientNameZhCn = stringOr(
    record.clientNameZhCn ?? record.client_name_zh_cn ?? record.nameZhCn ?? record.displayNameZhCn ?? record.displayName ?? record.name,
    displayNameFromId(registryId)
  );
  const translationKey = stringOr(record.translationKey ?? record.translation_key);
  const catalogHash = stringOr(record.catalogHash ?? record.catalog_hash ?? status.catalogHash ?? status.catalog_hash) || fingerprint({
    itemKey,
    registryId,
    identityKind,
    variantDiscriminator: discriminator,
    componentsCanonical,
    componentsSnbt,
    identityComponentsCanonical,
    variantKey: variantKey || null,
    canonicalVariant,
    canonicalComponents,
    clientNameZhCn,
    translationKey
  });
  const deployedHash = stringOr(record.deployedHash ?? record.deployed_hash ?? status.deployedHash ?? status.deployed_hash) || null;
  return {
    itemKey,
    registryId,
    identityKind,
    variantDiscriminator: discriminator,
    componentsCanonical,
    componentsSnbt,
    identityComponentsCanonical,
    variantKey: variantKey || null,
    canonicalVariant,
    canonicalComponents,
    clientNameZhCn,
    translationKey,
    namespace: namespaceOf(registryId),
    managed: true,
    humanSelected: true,
    ownership: "utd",
    source: "whitelist",
    recipeInput: false,
    recipeOutput: false,
    recipeInputCount: 0,
    recipeOutputCount: 0,
    lootEnabled: false,
    lootLevel: null,
    catalogHash,
    deployedHash,
    sync: deployedHash ? (deployedHash === catalogHash ? "synced" : "stale") : "local_only",
    issues: []
  };
}

function normalizeSnapshotComponents(record: Record<string, unknown>): JsonObject {
  const direct = record.canonicalComponents ?? record.canonical_components ?? record.components;
  if (isRecord(direct)) return direct as JsonObject;
  const canonical = stringOr(record.componentsCanonical ?? record.components_canonical)
    || (typeof direct === "string" ? direct : "");
  const snbt = stringOr(record.componentsSnbt ?? record.components_snbt);
  if (!canonical && !snbt) return {};
  return {
    encoding: "utd-nbt-canonical-v1",
    canonical,
    snbt
  };
}

export function normalizeRecipeSource(input: unknown): NormalizedRecipeSource {
  if (!isRecord(input)) throw new Error("Recipe data must be an object.");
  const recipes: CanonicalRecipe[] = [];
  for (const sourceKind of ["shaped", "shapeless", "custom"] as const) {
    const rows = Array.isArray(input[sourceKind]) ? input[sourceKind] : [];
    rows.forEach((entry) => recipes.push(normalizeRecipe(entry, sourceKind)));
  }
  return {
    generatedAt: stringOr(input.generatedAt ?? input.generated_at) || undefined,
    recipes,
    policies: {
      removeTypes: stringArray(input.removeTypes),
      removeRecipeIds: stringArray(input.removeRecipeIds),
      removeOutputs: stringArray(input.removeOutputs)
    }
  };
}

function normalizeRecipe(entry: unknown, sourceKind: RecipeSourceKind): CanonicalRecipe {
  if (!isRecord(entry)) throw new Error(`Recipe entry in ${sourceKind} is not an object.`);
  const id = stringOr(entry.id);
  if (!id) throw new Error(`Recipe entry in ${sourceKind} has no id.`);
  const utd = isRecord(entry.utd) ? entry.utd : {};
  const rawJson = isRecord(entry.json) ? entry.json : {};
  const ownership = id.startsWith("kubejs:utd_") || Object.keys(utd).length > 0 ? "utd" : "external";
  let inputs: CanonicalRef[] = [];
  let outputs: CanonicalRef[] = [];
  let pattern: string[] | undefined;
  let symbolMap: Record<string, CanonicalRef> | undefined;
  let recipeType: string;

  if (sourceKind === "shaped") {
    recipeType = "minecraft:crafting_shaped";
    pattern = stringArray(entry.pattern);
    symbolMap = {};
    const key = isRecord(entry.key) ? entry.key : {};
    for (const [symbol, rawRef] of Object.entries(key)) {
      const normalized = normalizeRef(rawRef, 1);
      if (!normalized) continue;
      symbolMap[symbol] = normalized;
      const count = pattern.reduce((sum, row) => sum + [...row].filter((cell) => cell === symbol).length, 0);
      inputs.push({ ...normalized, count: Math.max(1, count) * normalized.count });
    }
    outputs = normalizeRefList(entry.output);
  } else if (sourceKind === "shapeless") {
    recipeType = "minecraft:crafting_shapeless";
    inputs = normalizeRefList(entry.ingredients);
    outputs = normalizeRefList(entry.output);
  } else {
    recipeType = stringOr(rawJson.type, "custom:unknown");
    inputs = extractCustomInputs(rawJson);
    outputs = extractCustomOutputs(rawJson);
    if (!outputs.length) outputs = stringArray(utd.outputKeys).map((ref) => ({
      refKind: "item" as const,
      ref: registryIdFromIdentity(ref),
      count: numberOr(utd.count, 1)
    }));
    const declaredOutput = stringOr(utd.output);
    if (!outputs.length && declaredOutput) {
      outputs = [{ refKind: "item", ref: registryIdFromIdentity(declaredOutput), count: numberOr(utd.count, 1) }];
    }
  }
  outputs = enrichRecipeOutputs(outputs, utd, rawJson);
  inputs = aggregateRefs(inputs);
  outputs = aggregateRefs(outputs);
  const issues: string[] = [];
  if (!inputs.length) issues.push("recipe_has_no_inputs");
  if (!outputs.length) issues.push("recipe_has_no_outputs");
  return {
    id,
    sourceKind,
    recipeType,
    ownership,
    editable: ownership === "utd",
    station: stringOr(utd.station, sourceKind === "custom" ? recipeType : "工作台"),
    stationKey: stringOr(utd.stationKey, sourceKind === "custom" ? "custom" : "crafting"),
    stationScope: stringOr(utd.stationScope, sourceKind === "custom" ? "custom" : "crafting"),
    form: stringOr(utd.form, sourceKind),
    sheet: stringOr(utd.sheet, "external"),
    sourceRow: typeof utd.row === "number" ? utd.row : null,
    level: typeof utd.level === "number" ? utd.level : null,
    outputName: stringOr(utd.outputName),
    inputs,
    outputs,
    pattern,
    symbolMap,
    raw: entry as JsonObject,
    issues
  };
}

function extractCustomInputs(json: Record<string, unknown>): CanonicalRef[] {
  const refs: CanonicalRef[] = [];
  if (Array.isArray(json.ingredients)) refs.push(...normalizeRefList(json.ingredients));
  if (json.input !== undefined) refs.push(...normalizeRefList(json.input));
  if (Array.isArray(json.materials)) {
    for (const material of json.materials) {
      if (!isRecord(material)) continue;
      const count = numberOr(material.count, 1);
      const normalized = normalizeRef(material.item ?? material.ingredient ?? material, count);
      if (normalized) refs.push(normalized);
    }
  }
  return refs;
}

function extractCustomOutputs(json: Record<string, unknown>): CanonicalRef[] {
  const refs: CanonicalRef[] = [];
  if (json.results !== undefined) refs.push(...normalizeRefList(json.results));
  if (json.result !== undefined) refs.push(...normalizeRefList(json.result));
  return refs;
}

function normalizeRefList(value: unknown): CanonicalRef[] {
  const entries = Array.isArray(value) ? value : value === undefined ? [] : [value];
  return entries.flatMap((entry) => {
    const normalized = normalizeRef(entry, 1);
    return normalized ? [normalized] : [];
  });
}

function normalizeRef(value: unknown, inheritedCount: number): CanonicalRef | null {
  if (typeof value === "string") {
    const isTag = value.startsWith("#");
    return isTag
      ? { refKind: "tag", ref: value.slice(1), count: inheritedCount }
      : itemRef(value, inheritedCount);
  }
  if (!isRecord(value)) return null;
  const count = numberOr(value.count, inheritedCount);
  const chance = typeof value.chance === "number" ? value.chance : undefined;
  const consume = typeof value.consume === "boolean" ? value.consume : undefined;
  const legacyNbt = typeof value.nbt === "string"
    ? value.nbt
    : isRecord(value.nbt)
      ? stableStringify(value.nbt)
      : "";
  const components = isRecord(value.components) || legacyNbt
    ? {
        ...(isRecord(value.components) ? value.components as JsonObject : {}),
        ...(legacyNbt ? { legacy_nbt: legacyNbt } : {})
      }
    : undefined;
  const withMeta = (ref: CanonicalRef): CanonicalRef => ({ ...ref, count, chance, consume, components });
  if (typeof value.item === "string") return withMeta(withLegacyNbt(itemRef(value.item, count), legacyNbt));
  if (typeof value.tag === "string") return withMeta({ refKind: "tag", ref: value.tag.replace(/^#/, ""), count });
  if (typeof value.fluid === "string") return withMeta({ refKind: "fluid", ref: value.fluid, count });
  for (const key of ["item", "ingredient", "stack", "value"] as const) {
    if (isRecord(value[key])) {
      const nested = normalizeRef(value[key], count);
      if (nested) return {
        ...withLegacyNbt(nested, legacyNbt),
        chance,
        consume,
        components: components ?? nested.components
      };
    }
  }
  for (const key of ["id", "gun", "ammo"] as const) {
    if (typeof value[key] === "string" && value[key].includes(":")) {
      return withMeta(itemRef(value[key], count));
    }
  }
  return null;
}

function withLegacyNbt(ref: CanonicalRef, legacyNbt: string): CanonicalRef {
  if (!legacyNbt || ref.refKind !== "item") return ref;
  const discriminator = variantDiscriminator(legacyNbt);
  if (!discriminator) return ref;
  return {
    ...ref,
    identityKey: `${ref.ref}${legacyNbt}`,
    variantDiscriminator: discriminator
  };
}

function aggregateRefs(refs: CanonicalRef[]): CanonicalRef[] {
  const aggregated = new Map<string, CanonicalRef>();
  for (const ref of refs) {
    const key = `${ref.refKind}:${ref.ref}:${ref.identityKey ?? ""}:${ref.variantDiscriminator ?? ""}:${stableStringify(ref.components ?? {})}:${ref.chance ?? ""}:${ref.consume ?? ""}`;
    const existing = aggregated.get(key);
    if (existing) existing.count += ref.count;
    else aggregated.set(key, { ...ref });
  }
  return [...aggregated.values()];
}

export function normalizeLootRegistry(input: unknown): CanonicalLootPolicy[] {
  const rows = Array.isArray(input)
    ? input
    : isRecord(input) && Array.isArray(input.items)
      ? input.items
      : [];
  return rows.flatMap((entry) => {
    if (!isRecord(entry)) return [];
    const identityKey = stringOr(entry.id);
    const registryId = stringOr(entry.lootItemId) || registryIdFromIdentity(identityKey);
    if (!registryId) return [];
    return [{
      identityKey,
      registryId,
      variantDiscriminator: variantDiscriminator([identityKey, stringOr(entry.lootNbt)]),
      lootEnabled: entry.lootEnabled !== false,
      level: numberOr(entry.level, 0),
      count: numberOr(entry.count, 1),
      commonTags: stringArray(entry.commonTags),
      commonBaseWeight: numberOr(entry.commonBaseWeight, 0),
      allowedCommonTemplates: stringArray(entry.allowedCommonTemplates),
      directedTemplates: stringArray(entry.directedTemplates),
      directedWeight: numberOr(entry.directedWeight, 0),
      replacePriority: numberOr(entry.replacePriority, 0),
      legacyNbt: stringOr(entry.lootNbt) || undefined,
      raw: entry as JsonObject
    }];
  });
}

function itemRef(identity: string, count: number): CanonicalRef {
  const discriminator = variantDiscriminator(identity);
  return {
    refKind: "item",
    ref: registryIdFromIdentity(identity),
    identityKey: identity !== registryIdFromIdentity(identity) ? identity : undefined,
    variantDiscriminator: discriminator || undefined,
    count
  };
}

function enrichRecipeOutputs(
  outputs: CanonicalRef[],
  utd: Record<string, unknown>,
  rawJson: Record<string, unknown>
): CanonicalRef[] {
  const result = isRecord(rawJson.result) ? rawJson.result : null;
  const taczType = result ? stringOr(result.type) : "";
  const taczId = result ? stringOr(result.id) : "";
  const carriers: Record<string, { registryId: string; discriminatorKey: string }> = {
    gun: { registryId: "tacz:modern_kinetic_gun", discriminatorKey: "GunId" },
    ammo: { registryId: "tacz:ammo", discriminatorKey: "AmmoId" },
    attachment: { registryId: "tacz:attachment", discriminatorKey: "AttachmentId" }
  };
  const carrier = carriers[taczType];
  if (carrier && taczId) {
    const count = numberOr(result?.count, outputs[0]?.count ?? numberOr(utd.count, 1));
    return [{
      refKind: "item",
      ref: carrier.registryId,
      identityKey: taczId,
      variantDiscriminator: `${carrier.discriminatorKey}=${taczId.toLocaleLowerCase()}`,
      count
    }];
  }

  const declared = stringArray(utd.outputKeys);
  return outputs.map((output) => {
    if (output.refKind !== "item" || output.variantDiscriminator) return output;
    const identity = declared.find((key) =>
      registryIdFromIdentity(key) === output.ref && variantDiscriminator(key)
    );
    if (!identity) return output;
    return {
      ...output,
      identityKey: identity,
      variantDiscriminator: variantDiscriminator(identity)
    };
  });
}
