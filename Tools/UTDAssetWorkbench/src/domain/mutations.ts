import { matchLootPolicies } from "./build";
import { buildFilteredGraph } from "./graph";
import { lootMatchesItem, refMatchesItem } from "./relations";
import type { CanonicalItem, CanonicalLootPolicy, CanonicalRecipe, ValidationIssue, WorkbenchProject } from "./schema";
import { fingerprint } from "./stable";

type ItemPatch = Pick<CanonicalItem, "clientNameZhCn" | "translationKey">;
type RecipePatch = Pick<CanonicalRecipe, "station" | "stationKey" | "stationScope" | "form" | "level">;
type LootPatch = Pick<CanonicalLootPolicy, "lootEnabled" | "level" | "count" | "commonTags" | "commonBaseWeight" | "directedWeight" | "replacePriority">;

export function updateManagedItem(project: WorkbenchProject, itemKey: string, patch: Partial<ItemPatch>): WorkbenchProject {
  const next = structuredClone(project);
  const item = next.items.find((candidate) => candidate.itemKey === itemKey);
  if (!item?.managed) return project;
  Object.assign(item, patch);
  item.sync = "pending";
  return refreshProject(next, new Set([itemKey]));
}

export function updateManagedRecipe(project: WorkbenchProject, recipeId: string, patch: Partial<RecipePatch>): WorkbenchProject {
  const next = structuredClone(project);
  const recipe = next.recipes.find((candidate) => candidate.id === recipeId);
  if (!recipe?.editable) return project;
  Object.assign(recipe, patch);
  const touched = new Set(
    recipe.outputs
      .filter((output) => output.refKind === "item")
      .flatMap((output) => next.items.filter((item) => item.managed && refMatchesItem(output, item)).map((item) => item.itemKey))
  );
  for (const item of next.items) if (touched.has(item.itemKey)) item.sync = "pending";
  return refreshProject(next, touched);
}

export function updateManagedLoot(project: WorkbenchProject, identityKey: string, patch: Partial<LootPatch>): WorkbenchProject {
  const next = structuredClone(project);
  const policy = next.lootPolicies.find((candidate) => candidate.identityKey === identityKey);
  if (!policy) return project;
  const ownedItems = next.items.filter((item) => item.managed && lootMatchesItem(policy, item));
  if (!ownedItems.length) return project;
  Object.assign(policy, patch);
  const touched = new Set(ownedItems.map((item) => item.itemKey));
  for (const item of ownedItems) item.sync = "pending";
  return refreshProject(next, touched);
}

function refreshProject(project: WorkbenchProject, touched: Set<string>): WorkbenchProject {
  const issues: ValidationIssue[] = project.issues.filter((issue) => issue.entityType === "recipe");
  for (const item of project.items) {
    item.recipeInputCount = project.recipes.filter((recipe) => recipe.inputs.some((ref) => refMatchesItem(ref, item))).length;
    item.recipeOutputCount = project.recipes.filter((recipe) => recipe.outputs.some((ref) => refMatchesItem(ref, item))).length;
    item.recipeInput = item.recipeInputCount > 0;
    item.recipeOutput = item.recipeOutputCount > 0;
    const loot = matchLootPolicies(item, project.lootPolicies.filter((policy) => policy.registryId === item.registryId));
    item.lootEnabled = loot.some((policy) => policy.lootEnabled);
    const levels = loot.filter((policy) => policy.lootEnabled).map((policy) => policy.level);
    item.lootLevel = levels.length ? Math.max(...levels) : null;
    item.issues = item.issues.filter((code) => !["translation_key_missing", "managed_item_orphan", "recipe_cycle"].includes(code));
    if (item.humanSelected && !item.translationKey) addItemIssue(item, issues, "translation_key_missing", "Human-selected item has no translation_key.");
    if (item.humanSelected && !item.recipeOutput && !item.lootEnabled) addItemIssue(item, issues, "managed_item_orphan", "Human-selected item has neither a recipe output nor enabled Loot.");
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
      recipeInputCount: item.recipeInputCount,
      recipeOutputCount: item.recipeOutputCount,
      loot: loot.map((policy) => [policy.identityKey, policy.lootEnabled, policy.level, policy.count, policy.commonTags])
    });
    if (touched.has(item.itemKey)) item.sync = "pending";
    else if (item.deployedHash) item.sync = item.catalogHash === item.deployedHash ? "synced" : "stale";
    else item.sync = "local_only";
  }
  project.graph = buildFilteredGraph(project.items, project.recipes);
  for (const cycle of project.graph.cycles) {
    issues.push({ code: "recipe_cycle", severity: "error", message: `Managed recipe cycle: ${cycle.join(" -> ")}`, entityType: "project", entityId: cycle.join("|") });
    for (const itemKey of cycle) {
      const item = project.items.find((candidate) => candidate.itemKey === itemKey);
      if (item) {
        item.issues.push("recipe_cycle");
        item.sync = "error";
      }
    }
  }
  project.issues = issues;
  project.manifest.contentFingerprint = fingerprint({ items: project.items, recipes: project.recipes, loot: project.lootPolicies });
  project.manifest.catalogHash = project.manifest.contentFingerprint;
  project.manifest.counts = {
    managedItems: project.items.filter((item) => item.managed).length,
    dependencyItems: project.items.filter((item) => !item.managed).length,
    recipes: project.recipes.length,
    lootPolicies: project.lootPolicies.length,
    cycles: project.graph.cycles.length,
    issues: project.issues.length
  };
  return project;
}

function addItemIssue(item: CanonicalItem, issues: ValidationIssue[], code: string, message: string): void {
  item.issues.push(code);
  issues.push({ code, severity: "warning", message, entityType: "item", entityId: item.itemKey });
}
