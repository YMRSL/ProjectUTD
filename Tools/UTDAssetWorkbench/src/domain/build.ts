import { buildFilteredGraph } from "./graph";
import { variantDiscriminator } from "./identity";
import { normalizeLootRegistry, normalizeRecipeSource, normalizeSnapshot } from "./normalize";
import { hasVariantIdentity, lootMatchesItem, refMatchesItem } from "./relations";
import type {
  CanonicalItem,
  CanonicalLootPolicy,
  CanonicalRecipe,
  CanonicalRef,
  ItemSource,
  JsonObject,
  SourceFingerprint,
  ValidationIssue,
  WorkbenchProject
} from "./schema";
import { WORKBENCH_SCHEMA } from "./schema";
import { UNCATEGORIZED_CATEGORY } from "./categories";
import { asJsonObject, displayNameFromId, fingerprint, namespaceOf } from "./stable";

export interface BuildWorkbenchInput {
  snapshot: unknown;
  recipeData: unknown;
  lootRegistry?: unknown;
  lootBalance?: unknown;
  projectId?: string;
  generatedAt?: string;
  source?: {
    snapshot?: SourceFingerprint;
    recipes?: SourceFingerprint;
    lootRegistry?: SourceFingerprint;
    lootBalance?: SourceFingerprint;
    blockTransforms?: SourceFingerprint;
    itemProperties?: SourceFingerprint;
    categories?: SourceFingerprint;
  };
}

export function buildWorkbenchProject(input: BuildWorkbenchInput): WorkbenchProject {
  const snapshot = normalizeSnapshot(input.snapshot);
  const recipeSource = normalizeRecipeSource(input.recipeData);
  const recipes = recipeSource.recipes;
  const normalizedLoot = input.lootRegistry === undefined ? [] : normalizeLootRegistry(input.lootRegistry);
  const lootPolicies = remapLogicalLootIdentities(normalizedLoot, recipes);
  const items = buildStatusCatalog(snapshot.items, recipes, lootPolicies);
  const issues: ValidationIssue[] = [];

  markStatuses(items, recipes, lootPolicies);
  addStructuralIssues(items, recipes, issues);
  let graph = buildFilteredGraph(items, recipes);
  for (const cycle of graph.cycles) {
    const message = `Managed recipe cycle: ${cycle.join(" -> ")}`;
    issues.push({ code: "recipe_cycle", severity: "error", message, entityType: "project", entityId: cycle.join("|") });
    for (const itemKey of cycle) {
      const item = items.find((candidate) => candidate.itemKey === itemKey);
      if (item && !item.issues.includes("recipe_cycle")) item.issues.push("recipe_cycle");
    }
  }
  for (const item of items) {
    item.sync = item.issues.includes("recipe_cycle") || item.issues.includes("duplicate_item_key")
      ? "error"
      : item.deployedHash
        ? item.catalogHash === item.deployedHash ? "synced" : "stale"
        : "local_only";
  }
  graph = buildFilteredGraph(items, recipes);
  // This is the bundle build time. Source timestamps stay under manifest.source
  // so an older recipe export cannot make a fresh release look older than it is.
  const generatedAt = input.generatedAt ?? new Date().toISOString();
  const projectId = input.projectId ?? snapshot.projectId ?? "utd-assets";
  const contentFingerprint = fingerprint({
    items: items.map((item) => ({
      itemKey: item.itemKey,
      registryId: item.registryId,
      variantDiscriminator: item.variantDiscriminator,
      humanSelected: item.humanSelected,
      managed: item.managed,
      recipeInputCount: item.recipeInputCount,
      recipeOutputCount: item.recipeOutputCount,
      lootEnabled: item.lootEnabled,
      lootLevel: item.lootLevel,
      catalogHash: item.catalogHash
    })),
    recipes: recipes.map((recipe) => ({ id: recipe.id, inputs: recipe.inputs, outputs: recipe.outputs, station: recipe.station, level: recipe.level })),
    lootPolicies: lootPolicies.map((policy) => ({ identityKey: policy.identityKey, registryId: policy.registryId, variantDiscriminator: policy.variantDiscriminator, enabled: policy.lootEnabled, level: policy.level, count: policy.count })),
    presentations: [],
    blockTransforms: [],
    itemProperties: []
  });
  return {
    schemaVersion: WORKBENCH_SCHEMA,
    manifest: {
      schemaVersion: WORKBENCH_SCHEMA,
      projectId,
      generatedAt,
      contentFingerprint,
      catalogHash: contentFingerprint,
      deployedHash: null,
      source: {
        snapshot: input.source?.snapshot ?? {},
        recipes: { generatedAt: recipeSource.generatedAt, ...input.source?.recipes },
        lootRegistry: input.source?.lootRegistry,
        lootBalance: input.source?.lootBalance,
        blockTransforms: input.source?.blockTransforms,
        itemProperties: input.source?.itemProperties,
        categories: input.source?.categories
      },
      counts: {
        managedItems: items.filter((item) => item.managed).length,
        dependencyItems: items.filter((item) => !item.managed).length,
        recipes: recipes.length,
        lootPolicies: lootPolicies.length,
        presentations: 0,
        blockTransforms: 0,
        itemProperties: 0,
        cycles: graph.cycles.length,
        issues: issues.length
      }
    },
    items,
    categories: [UNCATEGORIZED_CATEGORY],
    recipes,
    lootPolicies,
    presentations: [],
    blockTransforms: [],
    itemProperties: [],
    lootBalance: input.lootBalance === undefined ? null : asJsonObject(input.lootBalance),
    graph,
    issues,
    sourcePolicies: recipeSource.policies
  };
}

function remapLogicalLootIdentities(
  policies: CanonicalLootPolicy[],
  recipes: CanonicalRecipe[]
): CanonicalLootPolicy[] {
  const recipeIdentityMap = new Map<string, CanonicalRef>();
  for (const recipe of recipes) {
    // A logical TaCZ/FPE identity may only appear as a recipe input (for
    // example a dismantling recipe). Inputs are just as authoritative as
    // outputs for resolving the shared carrier item + stable discriminator.
    for (const ref of [...recipe.inputs, ...recipe.outputs]) {
      if (ref.refKind !== "item") continue;
      if (ref.identityKey) recipeIdentityMap.set(ref.identityKey, ref);
      if (ref.variantDiscriminator?.startsWith("GunId=")) {
        recipeIdentityMap.set(ref.variantDiscriminator.slice("GunId=".length), ref);
      }
    }
  }
  return policies.map((policy) => {
    const mapped = recipeIdentityMap.get(policy.identityKey);
    if (!mapped) return policy;
    return {
      ...policy,
      registryId: mapped.ref,
      variantDiscriminator: mapped.variantDiscriminator ?? policy.variantDiscriminator
    };
  });
}

/** Full status catalog: whitelist identities + every recipe item ref + every Loot identity. */
function buildStatusCatalog(
  whitelist: CanonicalItem[],
  recipes: CanonicalRecipe[],
  lootPolicies: CanonicalLootPolicy[]
): CanonicalItem[] {
  const items = [...whitelist];
  for (const recipe of recipes) {
    for (const ref of recipe.inputs.filter((candidate) => candidate.refKind === "item")) {
      ensureRefItem(items, ref, "recipe_dependency");
    }
    for (const ref of recipe.outputs.filter((candidate) => candidate.refKind === "item")) {
      ensureRefItem(items, ref, "recipe_output");
    }
  }
  for (const policy of lootPolicies) ensureLootItem(items, policy);
  return items;
}

function ensureRefItem(items: CanonicalItem[], ref: CanonicalRef, source: ItemSource): void {
  if (items.some((item) => refMatchesItem(ref, item))) return;
  if (!ref.variantDiscriminator) {
    const capturedPlain = items.filter((item) =>
      item.registryId === ref.ref
      && !item.variantDiscriminator
      && ["plain", "base"].includes(item.identityKind.toLocaleLowerCase())
    );
    if (capturedPlain.length === 1) return;
  }
  const discriminator = ref.variantDiscriminator ?? "";
  const itemKey = discriminator ? `recipe:${ref.ref}::${discriminator}` : ref.ref;
  if (items.some((item) => item.itemKey === itemKey)) return;
  const item = catalogItem(itemKey, ref.ref, source);
  if (discriminator) {
    item.identityKind = "recipe_variant";
    item.variantKey = fingerprint(discriminator);
    item.variantDiscriminator = discriminator;
    item.identityComponentsCanonical = discriminator;
    item.canonicalVariant = { recipe_identity: ref.identityKey ?? discriminator };
    const legacyNbt = typeof ref.components?.legacy_nbt === "string" ? ref.components.legacy_nbt : "";
    if (legacyNbt) {
      item.componentsCanonical = ref.identityKey ?? discriminator;
      item.componentsSnbt = legacyNbt;
      item.canonicalComponents = { legacy_nbt: legacyNbt };
    }
  }
  items.push(item);
}

function ensureLootItem(items: CanonicalItem[], policy: CanonicalLootPolicy): void {
  const existing = items.find((item) => lootMatchesItem(policy, item));
  if (existing) {
    // Recipe discovery often creates the logical FPE variant first. Enrich that
    // same row with the deployable legacy SNBT instead of discarding Loot data.
    if (policy.legacyNbt) {
      const preferLootEncoding = existing.source !== "whitelist";
      if (preferLootEncoding || !existing.componentsCanonical) existing.componentsCanonical = policy.identityKey;
      if (preferLootEncoding || !existing.componentsSnbt) existing.componentsSnbt = policy.legacyNbt;
      if (preferLootEncoding || !Object.keys(existing.canonicalComponents).length) {
        existing.canonicalComponents = {
          ...existing.canonicalComponents,
          legacy_nbt: policy.legacyNbt
        };
      }
    }
    return;
  }
  const discriminator = policy.variantDiscriminator;
  const itemKey = discriminator ? `loot:${policy.registryId}::${discriminator}` : policy.identityKey;
  if (items.some((item) => item.itemKey === itemKey)) return;
  const item = catalogItem(itemKey, policy.registryId, "loot_registry");
  if (discriminator) {
    item.identityKind = "loot_variant";
    item.variantKey = fingerprint(discriminator);
    item.variantDiscriminator = discriminator;
    item.componentsCanonical = policy.identityKey;
    item.componentsSnbt = policy.legacyNbt ?? "";
    item.identityComponentsCanonical = discriminator;
    item.canonicalVariant = { loot_identity: policy.identityKey };
  }
  items.push(item);
}

function catalogItem(itemKey: string, registryId: string, source: ItemSource): CanonicalItem {
  return {
    itemKey,
    registryId,
    identityKind: "item",
    variantDiscriminator: "",
    componentsCanonical: "",
    componentsSnbt: "",
    identityComponentsCanonical: "",
    variantKey: null,
    canonicalVariant: null,
    canonicalComponents: {},
    clientNameZhCn: displayNameFromId(registryId),
    translationKey: "",
    iconDataUrl: "",
    categoryKey: UNCATEGORIZED_CATEGORY.key,
    categoryLabelZhCn: UNCATEGORIZED_CATEGORY.labelZhCn,
    categoryLevel: null,
    namespace: namespaceOf(registryId),
    managed: false,
    humanSelected: false,
    ownership: "external",
    source,
    recipeInput: false,
    recipeOutput: false,
    recipeInputCount: 0,
    recipeOutputCount: 0,
    lootEnabled: false,
    lootLevel: null,
    catalogHash: fingerprint({ itemKey, registryId, source }),
    deployedHash: null,
    sync: "local_only",
    issues: []
  };
}

function markStatuses(items: CanonicalItem[], recipes: CanonicalRecipe[], lootPolicies: CanonicalLootPolicy[]): void {
  for (const item of items) {
    const inputRecipes = recipes.filter((recipe) => recipe.inputs.some((ref) => refMatchesItem(ref, item)));
    const outputRecipes = recipes.filter((recipe) => recipe.outputs.some((ref) => refMatchesItem(ref, item)));
    const managedOutputRecipes = outputRecipes.filter((recipe) => recipe.ownership === "utd");
    const policies = matchLootPolicies(item, lootPolicies);
    item.recipeInputCount = inputRecipes.length;
    item.recipeOutputCount = outputRecipes.length;
    item.recipeInput = item.recipeInputCount > 0;
    item.recipeOutput = item.recipeOutputCount > 0;
    if (item.humanSelected && hasVariantIdentity(item)) {
      const sameRegistryPolicies = lootPolicies.filter((policy) => policy.registryId === item.registryId);
      if (sameRegistryPolicies.length > 0 && policies.length === 0) item.issues.push("loot_variant_unmatched");
    }
    item.lootEnabled = policies.some((policy) => policy.lootEnabled);
    const levels = policies.filter((policy) => policy.lootEnabled).map((policy) => policy.level);
    item.lootLevel = levels.length ? Math.max(...levels) : null;
    item.managed = item.humanSelected || managedOutputRecipes.length > 0 || policies.length > 0;
    item.ownership = item.managed ? "utd" : "external";
    if (!item.humanSelected) {
      if (policies.length) item.source = "loot_registry";
      else if (item.recipeOutput) item.source = "recipe_output";
      else item.source = "recipe_dependency";
    }
    item.catalogHash = fingerprint({
      identity: [
        item.itemKey,
        item.registryId,
        item.variantKey,
        item.variantDiscriminator,
        item.identityComponentsCanonical || item.canonicalVariant,
        item.clientNameZhCn,
        item.translationKey
      ],
      managed: item.managed,
      humanSelected: item.humanSelected,
      recipeInputIds: inputRecipes.map((recipe) => recipe.id),
      recipeOutputIds: outputRecipes.map((recipe) => recipe.id),
      loot: policies.map((policy) => [policy.identityKey, policy.lootEnabled, policy.level, policy.count, policy.commonTags])
    });
  }
}

function addStructuralIssues(items: CanonicalItem[], recipes: CanonicalRecipe[], issues: ValidationIssue[]): void {
  const itemCounts = countBy(items.map((item) => item.itemKey));
  for (const item of items.filter((candidate) => candidate.humanSelected)) {
    if ((itemCounts.get(item.itemKey) ?? 0) > 1) pushItemIssue(item, issues, "duplicate_item_key", "error", "Whitelist contains a duplicate item_key.");
    if (!item.translationKey && !item.clientNameZhCn) {
      pushItemIssue(item, issues, "translation_key_missing", "warning", "Human-selected item has neither a translation_key nor a captured display name.");
    }
    if (!item.recipeOutput && !item.lootEnabled) pushItemIssue(item, issues, "managed_item_orphan", "warning", "Human-selected item has neither a matching recipe output nor enabled Loot.");
    if (item.issues.includes("loot_variant_unmatched")) {
      issues.push({
        code: "loot_variant_unmatched",
        severity: "warning",
        message: "Variant item has Loot rows for the same registry id, but no matching discriminator.",
        entityType: "item",
        entityId: item.itemKey
      });
    }
  }
  const recipeCounts = countBy(recipes.map((recipe) => recipe.id));
  for (const recipe of recipes) {
    if ((recipeCounts.get(recipe.id) ?? 0) > 1) {
      recipe.issues.push("duplicate_recipe_id");
      issues.push({ code: "duplicate_recipe_id", severity: "error", message: "Recipe catalog contains a duplicate id.", entityType: "recipe", entityId: recipe.id });
    }
    for (const code of recipe.issues) {
      issues.push({ code, severity: "warning", message: code.replaceAll("_", " "), entityType: "recipe", entityId: recipe.id });
    }
  }
}

function pushItemIssue(
  item: CanonicalItem,
  issues: ValidationIssue[],
  code: string,
  severity: ValidationIssue["severity"],
  message: string
): void {
  item.issues.push(code);
  issues.push({ code, severity, message, entityType: "item", entityId: item.itemKey });
}

function countBy(values: string[]): Map<string, number> {
  const result = new Map<string, number>();
  for (const value of values) result.set(value, (result.get(value) ?? 0) + 1);
  return result;
}

export function matchLootPolicies(item: CanonicalItem, policies: CanonicalLootPolicy[]): CanonicalLootPolicy[] {
  return policies.filter((policy) => lootMatchesItem(policy, item));
}
