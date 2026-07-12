import { matchLootPolicies } from "./build";
import { validateBlockTransforms } from "./blockTransforms";
import { buildFilteredGraph } from "./graph";
import {
  constrainPresentationScope,
  defaultPresentation,
  derivePresentationKeys,
  isFpePresentationVariant,
  presentationForItem
} from "./presentation";
import { lootMatchesItem, refMatchesItem } from "./relations";
import type {
  BlockTransform,
  CanonicalItem,
  CanonicalLootPolicy,
  CanonicalRecipe,
  ItemPresentationOverride,
  ValidationIssue,
  WorkbenchProject
} from "./schema";
import { fingerprint } from "./stable";

type ItemPatch = Pick<CanonicalItem, "clientNameZhCn" | "translationKey">;
type RecipePatch = Pick<CanonicalRecipe, "station" | "stationKey" | "stationScope" | "form" | "level">;
type LootPatch = Pick<CanonicalLootPolicy, "lootEnabled" | "level" | "count" | "commonTags" | "commonBaseWeight" | "directedWeight" | "replacePriority">;
type PresentationPatch = Pick<ItemPresentationOverride, "enabled" | "nameZhCn" | "descriptionZhCn" | "applyScope">;
type BlockTransformPatch = Pick<BlockTransform,
  "enabled" | "priority" | "clickedBlock" | "targetState" | "resultBlock" | "resultState" | "copyProperties"
  | "inputSource" | "hand" | "requireSneaking" | "allowFakePlayer" | "consumeInput"
  | "creativeRequireInput" | "creativeConsume"
> & {
  catalystCount: number;
};

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

export function updateItemPresentation(
  project: WorkbenchProject,
  itemKey: string,
  patch: Partial<PresentationPatch>
): WorkbenchProject {
  const next = structuredClone(project);
  const item = next.items.find((candidate) => candidate.itemKey === itemKey);
  if (!item?.managed || item.ownership !== "utd") return project;
  const exactIdentity = next.presentations.find((entry) => entry.applyScope === "identity"
    && (entry.itemKey === itemKey
      || (entry.registryId === item.registryId
        && entry.variantDiscriminator === item.variantDiscriminator)));
  const registryPresentation = next.presentations.find((entry) => entry.applyScope === "registry"
    && entry.registryId === item.registryId);
  const current = presentationForItem(next, item);
  const requestedScope = patch.applyScope ?? current?.applyScope ?? defaultPresentation(item).applyScope;
  const applyScope = constrainPresentationScope(item, requestedScope);
  const switchingScope = current !== undefined && current.applyScope !== applyScope;

  let presentation = applyScope === "registry"
    ? registryPresentation ?? (switchingScope ? exactIdentity : undefined)
    : exactIdentity ?? (switchingScope ? registryPresentation : undefined);
  if (!presentation) {
    presentation = defaultPresentation(item);
    next.presentations.push(presentation);
  }

  // Changing the selector means moving this authored override, not leaving a
  // more-specific stale entry that would continue to win at runtime.
  if (switchingScope && current && current !== presentation) {
    next.presentations = next.presentations.filter((entry) => entry !== current);
  }
  Object.assign(presentation, patch);
  presentation.itemKey = item.itemKey;
  presentation.registryId = item.registryId;
  presentation.variantDiscriminator = item.variantDiscriminator;
  presentation.applyScope = applyScope;
  presentation.observedNameZhCn ||= item.clientNameZhCn;
  presentation.baseCatalogHash ||= item.catalogHash;
  presentation.updatedAt = new Date().toISOString();
  const keys = derivePresentationKeys(item, presentation.applyScope);
  presentation.nameKey = keys.nameKey;
  presentation.descriptionKey = keys.descriptionKey;

  // Keep one row per semantic runtime target. This also cleans up registry
  // duplicates from older in-memory projects before they can be exported.
  next.presentations = next.presentations.filter((entry) => entry === presentation
    || (applyScope === "registry"
      ? entry.applyScope !== "registry" || entry.registryId !== item.registryId
      : entry.applyScope !== "identity"
        || entry.registryId !== item.registryId
        || entry.variantDiscriminator !== item.variantDiscriminator));
  if (isFpePresentationVariant(item)) {
    next.presentations = next.presentations.filter((entry) => entry === presentation
      || entry.applyScope !== "registry"
      || entry.registryId !== item.registryId);
  }
  const touched = new Set(next.items
    .filter((candidate) => presentation!.applyScope === "registry"
      ? candidate.registryId === item.registryId && candidate.managed
      : candidate.itemKey === itemKey)
    .map((candidate) => candidate.itemKey));
  for (const candidate of next.items) if (touched.has(candidate.itemKey)) candidate.sync = "pending";
  return refreshProject(next, touched);
}

export function addBlockTransform(project: WorkbenchProject, catalystItemKey: string): WorkbenchProject {
  const next = structuredClone(project);
  const item = next.items.find((candidate) => candidate.itemKey === catalystItemKey);
  if (!item?.managed || item.ownership !== "utd") return project;
  const prefix = `utd:block_transform/${safePath(item.registryId)}`;
  let suffix = 1;
  let id = `${prefix}_${suffix}`;
  while (next.blockTransforms.some((entry) => entry.id === id)) id = `${prefix}_${++suffix}`;
  next.blockTransforms.push({
    id,
    enabled: false,
    priority: 0,
    clickedBlock: "",
    targetState: {},
    resultBlock: "",
    resultState: {},
    copyProperties: [],
    catalyst: {
      refKind: "item",
      ref: item.registryId,
      identityKey: item.variantDiscriminator ? item.itemKey : undefined,
      variantDiscriminator: item.variantDiscriminator || undefined,
      count: 1
    },
    // Stable TaCZ/FPE discriminators deliberately ignore mutable runtime
    // components such as ammo count, fire mode, durability, or Sona rot time.
    // Generic component identities have no safer discriminator and retain an
    // exact component match.
    catalystComponentsSnbt: item.variantDiscriminator
      ? "{}"
      : ["item", "plain", "base"].includes(item.identityKind.toLocaleLowerCase())
        ? "{}"
        : item.componentsSnbt || "{}",
    inputSource: "clicked_hand",
    hand: "main",
    requireSneaking: false,
    allowFakePlayer: false,
    consumeInput: true,
    cancelInteraction: true,
    blockEntityPolicy: "reject",
    creativeRequireInput: true,
    creativeConsume: false
  });
  item.sync = "pending";
  return refreshProject(next, new Set([item.itemKey]));
}

export function updateBlockTransform(
  project: WorkbenchProject,
  id: string,
  patch: Partial<BlockTransformPatch>
): WorkbenchProject {
  const next = structuredClone(project);
  const rule = next.blockTransforms.find((entry) => entry.id === id);
  if (!rule) return project;
  const { catalystCount, ...direct } = patch;
  Object.assign(rule, direct);
  if (direct.inputSource === "inventory") rule.requireSneaking = true;
  if (catalystCount !== undefined) rule.catalyst.count = Math.max(1, catalystCount);
  const touched = new Set(next.items
    .filter((item) => item.managed && refMatchesItem(rule.catalyst, item))
    .map((item) => item.itemKey));
  for (const item of next.items) if (touched.has(item.itemKey)) item.sync = "pending";
  return refreshProject(next, touched);
}

export function removeBlockTransform(project: WorkbenchProject, id: string): WorkbenchProject {
  const next = structuredClone(project);
  const rule = next.blockTransforms.find((entry) => entry.id === id);
  if (!rule) return project;
  next.blockTransforms = next.blockTransforms.filter((entry) => entry.id !== id);
  const touched = new Set(next.items
    .filter((item) => item.managed && refMatchesItem(rule.catalyst, item))
    .map((item) => item.itemKey));
  for (const item of next.items) if (touched.has(item.itemKey)) item.sync = "pending";
  return refreshProject(next, touched);
}

export function refreshProject(project: WorkbenchProject, touched: Set<string>): WorkbenchProject {
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
      loot: loot.map((policy) => [policy.identityKey, policy.lootEnabled, policy.level, policy.count, policy.commonTags]),
      presentations: project.presentations.filter((entry) => entry.itemKey === item.itemKey || (entry.applyScope === "registry" && entry.registryId === item.registryId)),
      blockTransforms: project.blockTransforms.filter((entry) => refMatchesItem(entry.catalyst, item))
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
  issues.push(...validateBlockTransforms(project.blockTransforms));
  project.issues = issues;
  project.manifest.contentFingerprint = fingerprint({
    items: project.items,
    recipes: project.recipes,
    loot: project.lootPolicies,
    presentations: project.presentations,
    blockTransforms: project.blockTransforms
  });
  project.manifest.catalogHash = project.manifest.contentFingerprint;
  project.manifest.counts = {
    managedItems: project.items.filter((item) => item.managed).length,
    dependencyItems: project.items.filter((item) => !item.managed).length,
    recipes: project.recipes.length,
    lootPolicies: project.lootPolicies.length,
    presentations: project.presentations.length,
    blockTransforms: project.blockTransforms.length,
    cycles: project.graph.cycles.length,
    issues: project.issues.length
  };
  return project;
}

function safePath(value: string): string {
  return value.toLocaleLowerCase().replace(/[^a-z0-9_.-]+/g, "_").replace(/^_+|_+$/g, "") || "rule";
}

function addItemIssue(item: CanonicalItem, issues: ValidationIssue[], code: string, message: string): void {
  item.issues.push(code);
  issues.push({ code, severity: "warning", message, entityType: "item", entityId: item.itemKey });
}
